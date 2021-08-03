/*
 * Bluetooth Low Energy (LE) Manager
 *
 * Handles all Bluetooth GATT connection tasks.
 *
 * Contains all specific CTEK Battery Sense communication parts.
 */

package com.ctek.sba.bluetooth;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.ctek.sba.device.DeviceMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import greendao.Device;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeManager implements CTEKBLEManagerInterface {

  private static final String TAG = BluetoothLeManager.class.getSimpleName();

  private final Context context;
  private static BluetoothLeManager self;

  private BluetoothManager mBluetoothManager;
  private BluetoothAdapter mBluetoothAdapter;
  private Device mDevice;
  private BluetoothGatt mBluetoothGatt;
  private BluetoothGattService baseService;
  private BluetoothGattService baseServiceV2;
  private BluetoothGattService infoService;
  private Handler handler;
  private BluetoothAdapter.LeScanCallback leScanCallback;

  //Added
  public static final String DEVICE_CONNECTED = "com.ctek.sba.DEVICE_CONNECTED";

  //Added
  private ArrayList<String> arrDeviceMac = new ArrayList<>();
  AlertDialog.Builder alertBuilder;

  // Update device related variables
  private final List<Double> voltagesGlobal = new ArrayList<>();
  private final List<Integer> temperatGlobal = new ArrayList<>();

  //Added store devices
  private final ArrayList<BluetoothDevice> availDevices = new ArrayList<>();

  // These are read from the device
  // Defaults are valid but over written on successfulk read
  private long uptime = 0;
  private int interval = 5;
  private int historyCursor = 0;

  // Updates have been don up to this position/time
  private int currentCursor = -1;
  private long timeOf_currentCursor = -1;

  private int expectedRecords = 0;

  private boolean bGapInData;

  private ScheduledExecutorService bondingThreadPoolExecutor;

  private Task task;
  private ScanCallback mScanCallback;

  public enum Task {
    UPDATING_DEVICE,
    READING_SERIAL_NUMBER,
    PAIRING_DEVICE
  }

  // EA 14-Oct-2016. Special real-time mode.
  private long lastRealLiveVoltageCall = 0;
  private long lastRealLiveTemperaCall = 0;

  private String macSelectedInDetails2;

  public void startLiveMode(String mac) {
    macSelectedInDetails2 = mac;
    if (mac == null) {
      lastRealLiveVoltageCall = 0;
      lastRealLiveTemperaCall = 0;
    }
  }

  protected boolean isInLiveMode() {
    Log.i("BLE","In Live mode");
    return macSelectedInDetails2 != null;
  }

  public static synchronized BluetoothLeManager getInstance(Context context) {
    if (self == null) {
      self = new BluetoothLeManager(context.getApplicationContext());
    }
    return self;
  }

  private BluetoothLeManager(Context context) {
    this.context = context.getApplicationContext();
    this.handler = new Handler(context.getMainLooper());
    setupCallback();
  }

  private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      Log.d(TAG, String.format("onConnectionStateChange: %s, OldStatus:%s", newState, status));
      if (status == BluetoothGatt.GATT_SUCCESS) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
          // OK, we are connected
          Log.i(TAG, "2: Connected to GATT server.");
          // Attempt to discover services after successful connection.
          Log.i(TAG, "3: Discover services.");
          // TODO: Need to check sleep is required or not
          try {
            Thread.sleep(800);
          } catch (Exception e) {
            Log.i(TAG, "Error thread");
          }
          try {
            handler.post(new Runnable() {
              @Override
              public void run() {
                if (mBluetoothGatt != null) {
                  mBluetoothGatt.discoverServices();
                }
              }
            });
          } catch (Exception e) {
           // handler.removeCallbacks(gattServiceTimeout);
            Log.i(TAG,"Failed: discoverServices()");
          }
          List<BluetoothDevice> gattDevices = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
          Log.i(TAG, "Devices:" + gattDevices);
          if (gattDevices.isEmpty()) {
            // mBluetoothGatt.disconnect();

            // TODO: Need to check sleep is required or not
            try {
              Thread.sleep(600);
            } catch (Exception e) {
              Log.i(TAG, "Error thread");
            }
            //Not helping
            DeviceManagerHiQ.getInstance().updateAllDevices("noDevice", "");
          }

        }
        else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
          //Added
          mBluetoothGatt.disconnect();
          mBluetoothGatt.close();

          String devieId = gatt.getDevice().getAddress();
          Log.i(TAG, "Disconnected from GATT server. DeviceAddress: " + devieId);

          broadcastUpdateWithDeviceId(CTEK.ACTION_SERVICE_STOPPED, devieId);
          //broadcastUpdate(CTEK.ACTION_SERVICE_STOPPED);
        }
      }
      else {
        Log.i(TAG, "Error when connecting to GATT server.");
        //Added
        if (mBluetoothGatt != null) {
          mBluetoothGatt.disconnect();
          mBluetoothGatt.close();
        }
        broadcastUpdate(CTEK.ACTION_SERVICE_STOPPED);
      }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      // Assume no services found
      baseService = null;
      infoService = null;

      if (status == BluetoothGatt.GATT_SUCCESS) {
        // Get our base service
        baseService = mBluetoothGatt.getService(SBADevice.Service.BASE_SERVICE_UUID);
        infoService = mBluetoothGatt.getService(SBADevice.Service.DEVICE_INFORMATION_SERVICE_UUID);
        baseServiceV2 = mBluetoothGatt.getService(SBADevice.Service.BASE_SERVICE_UUID_V2);


        if (baseServiceV2 != null && infoService != null) {
          Log.i(TAG, "4: Services discovered.");
          triggerBonding();

//          if (task == Task.READING_SERIAL_NUMBER) {
//            readSerialNumber();
//          } else {
//            Log.i(TAG, "5: Write keyhole.");
//            writeToKeyHole();
//          }
        }
      }
      if (baseServiceV2 == null || infoService == null) {
        Log.d(TAG, "onServicesDiscovered failed.");
        //Added
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        //
        broadcastUpdate(CTEK.ACTION_SERVICE_STOPPED);
      }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
      Log.i(TAG, String.format("onCharacteristicRead: %s, status: %d, value: %s ",
          characteristic.getUuid(),
          status,
          Arrays.toString(characteristic.getValue())));

      // Handle uptime first to manage paring.
      if (SBADevice.Characteristic.UPTIME_UUID_V2.equals(characteristic.getUuid())) {
        // First time there should a specific status to indicate successful pairing.
        if (SBADevice.Status.CORRECT_KEY == status) {
          // Now we need to read uptime again to be sure it worked.
          Log.i(TAG, "10: Pairing confirmed (not completed).");
          readUptime();
          return;
        }
        // Handle pairing success or failure
        if (task == Task.PAIRING_DEVICE) {
          if (BluetoothGatt.GATT_SUCCESS == status) {
            Log.i(TAG, "11: Pairing confirmed and completed.");
            broadcastUpdate(CTEK.ACTION_KEY_UNLOCKED, characteristic);
          } else {
            Log.w(TAG, "Device key wrong. Device still locked and not paired");
            broadcastUpdate(CTEK.ACTION_KEY_UNLOCK_FAIL);
          }
          return;
        }
      }
      // Handle serial number
      if (SBADevice.Characteristic.SERIAL_NUMBER_UUID.equals(characteristic.getUuid())) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
          String serialNumber = characteristic.getStringValue(0);
          Log.d(TAG, String.format("serial number: %s", serialNumber));
          onSerialNumberReceived(serialNumber);
        } else {
          onReadSerialFail();
        }
      }
      // Proceed with normal battery data update
      if (status == BluetoothGatt.GATT_SUCCESS) {
        // Update is started by reading uptime after keyhole write and bonding
        if (SBADevice.Characteristic.UPTIME_UUID_V2.equals(characteristic.getUuid())) {
          uptime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
          // old readInterval();
          if (isInLiveMode()) {
            readLiveVoltage();
            handler.postDelayed(new Runnable() {
              public void run() {
                readLiveTemperature();
              }
            }, 500);
          } else {
            readInterval();
          }
        }
        // Next: get the measurement interval
        else if (SBADevice.Characteristic.MEASUREMENT_INTERVAL_UUID.equals(characteristic.getUuid())) {
          interval = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
          readHistoryCursor();
        }
        // Next: get the writepos
        else if (SBADevice.Characteristic.VOLTAGE_HISTORY_CURSOR_UUID.equals(characteristic.getUuid())) {
          resetServiceTimeout();
          historyCursor = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
          long numberOfRecords;
          long timeOf_historyCursor;
          int oldestCursor;
          long timeOf_oldestCursor;
          // Establish number of records and cursor to oldest and newest
          if (uptime >= interval * 60 * 30000) {
            // Device storage wrapped, setup to read until end first.
            numberOfRecords = 30000;
            oldestCursor = historyCursor + 1;
            if (oldestCursor >= 30000) {
              oldestCursor = 0;
            }
          }
          else {
            // Not, wrapped. Read from start.
            numberOfRecords = historyCursor;
            oldestCursor = 0;
          }
          // Calculate time of device oldest and newest data
          if (interval == 0) {
            timeOf_historyCursor = -1;
            timeOf_oldestCursor = -1;
          }
          else {
            timeOf_historyCursor = System.currentTimeMillis() - (uptime % (interval * 60)) * 1000;
            timeOf_oldestCursor = timeOf_historyCursor - (numberOfRecords * interval * 60) * 1000;
          }
          if (timeOf_currentCursor > timeOf_historyCursor) {
            //readLiveTemperature();
            // We are already up to date. Signal to UI that we did not get any new data.
            onDeviceDataReady();
          }
          else if (numberOfRecords == 0) {
            // There is no data on the device, read out a live voltage value
            if (uptime > 6) { //Original 60
              readLiveVoltage();
              //Added crashing
              // Toast.makeText(context, "No data found in CTEK device, refresh again after 5 - 10 mins", Toast.LENGTH_SHORT).show();
              // readLiveTemperature();
              onSoCNotReady();
            } else {
              testonDeviceUpdateFail("NoHistory");
            }
          }
          else {
            if (timeOf_currentCursor < timeOf_oldestCursor) {
              // There is a gap in data, indicate gap to get timestamps correctly set
              bGapInData = true;
              currentCursor = oldestCursor;
            }
            // Prepare reading history data using notifications
            enableHistoryNotifications();
          }
        }
        // Live voltage value was requested
        else if (SBADevice.Characteristic.VOLTAGE_CURRENT_UUID.equals(characteristic.getUuid())) {
          byte[] data = characteristic.getValue();

          //Added if  --->
          // if ((data != null) && (data.length > 0)) {
          CTEKData ctek = CTEK.parseVoltagesArray(CTEK.MODE_GET_BATTERY_LEVEL, data);
          Double[] voltages = ctek.getVoltages();
          Log.d(TAG + " " + 349, "Voltages array: " + Arrays.toString(voltages));

          if (isInLiveMode()) {
            if (lastRealLiveVoltageCall != 0) {
              long now_msecs = System.currentTimeMillis();
              long passed = now_msecs - lastRealLiveVoltageCall;
              long delta = 1000 - passed;
              if (delta < 0) delta = 0;
              handler.postDelayed(new Runnable() {
                public void run() {
                  readLiveVoltage();
                }
              }, delta);

              if (voltages != null && (voltages.length > 0)) {
                CTEK.broadcastLiveVoltage(context, mDevice.getAddress(), voltages[0]);
              }
            } else {
              readLiveVoltage();
            }
          }
          else { //Orginally present but empty

            gatt.getDevice().connectGatt(context,true,mGattCallback);
            handler.postDelayed(new Runnable() {
              public void run() {
                readLiveVoltage();
              }
            }, 1000); //commented orginally
          }
        }
        else if (SBADevice.Characteristic.TEMPERATURE_CURRENT_UUID.equals(characteristic.getUuid())) {
          byte[] data = characteristic.getValue();
          if ((data != null) && (data.length > 0)) {
            int iLiveTemp = (int) data[0];
            double dLiveTemperature = Math.round(((double) iLiveTemp) / 2.f);
            Log.e(TAG, "CURRENT TEMPERATURE = " + dLiveTemperature);
            DeviceMap.getInstance().setDeviceCurrTemp(mDevice.getId(), dLiveTemperature);

            if (isInLiveMode()) {
              CTEK.broadcastLiveTemperature(context, mDevice.getAddress(), dLiveTemperature);

              if (lastRealLiveTemperaCall != 0) {
                long now_msecs = System.currentTimeMillis();
                long passed = now_msecs - lastRealLiveTemperaCall;
                long delta = 1000 - passed;
                if (delta < 0) delta = 0;
                handler.postDelayed(new Runnable() {
                  public void run() {
                    readLiveTemperature();
                  }
                }, delta);
              } else {
                readLiveTemperature();
              }
            } else {
              // onDeviceDataReady();
              // Usual mode does't call 'get current temperature'. Check!!!
              handler.postDelayed(new Runnable() {
                public void run() {
                  readLiveTemperature();
                }
              }, 1000);
            }
          }
        }
      } else {
        // GATT read failed
        onDeviceUpdateFail();
      }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
      Log.d(TAG, String.format("onCharacteristicChanged, uuid: %s", characteristic.getUuid().toString()));
      if (characteristic.getUuid().equals(SBADevice.Characteristic.VOLTAGE_HISTORY_UUID)) {

        byte[] data = characteristic.getValue();
        CTEKData ctek = CTEK.parseVoltagesArray(CTEK.MODE_GET_BATTERY_AND_TEMP, data);
        Double[] voltages = ctek.getVoltages();
        Log.d(TAG + " " + 420, "Voltages array: " + Arrays.toString(voltages));
        Integer[] temp_ = ctek.getTemperatures();
        Log.d(TAG, "Temperatures array: " + Arrays.toString(temp_));

        voltagesGlobal.addAll(Arrays.asList(voltages));
        temperatGlobal.addAll(Arrays.asList(temp_));

        int totalLengthOfDataRead = voltagesGlobal.size();

        if (currentCursor > historyCursor) {
          // We are reading to end of buffer, check against max
          if (totalLengthOfDataRead == expectedRecords) {
            // Ask for the rest
            currentCursor = 0;
            writeVoltageCursor();
          }
        } else {
          if (totalLengthOfDataRead == expectedRecords) {
            onDeviceDataReady();
            Log.d(TAG, "Total data length: " + totalLengthOfDataRead);
          }
        }
      }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
      Log.d(TAG, String.format("onDescriptorRead, uuid: %s", descriptor.getUuid().toString()));
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
      Log.d(TAG, String.format("onDescriptorWrite, uuid: %s", descriptor.getUuid().toString()));
      if (BluetoothGatt.GATT_SUCCESS == status) {
        handler.postDelayed(new Runnable() {
          public void run() {
            subscribeToVoltageNotifications();
          }
        }, 0);
        handler.postDelayed(new Runnable() {
          public void run() {
            subscribeToTemperatureNotifications();
          }
        }, 500);
      }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
      Log.d(TAG, String.format("onCharacteristicWrite: %s, status: %d, value: %s ",
          characteristic.getUuid(),
          status,
          Arrays.toString(characteristic.getValue())));
      if (status == BluetoothGatt.GATT_SUCCESS) {
        if (SBADevice.Characteristic.KEY_HOLE_UUID.equals(characteristic.getUuid())) {
          Log.i(TAG, "6: Keyhole written.");

          triggerBonding();
        }
      } else {
        Log.w(TAG, "Characteristic write failed");
        //Added
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        //
      }
    }
  };

  //Added
  private void onSoCNotReady() {
    task = null;
    Intent i_ = new Intent(DeviceManagerHiQ.START_LIVE_MODE);
    context.sendBroadcast(i_);
  }

  private void onDeviceDataReady() {
    task = null;
    List<Double> packed = CTEK.packData(voltagesGlobal, temperatGlobal);
    CTEK.sendBCOnDeviceDataReady(context, mDevice.getAddress(), packed, historyCursor, bGapInData);
  }

  private void onSerialNumberReceived(String serial) {
    task = null;
    CTEK.sendBCOnSerialNumberReceived(context, mDevice.getAddress(), serial);
  }

  private void broadcastUpdate(final String action) {
    CTEK.broadcastUpdate(context, mDevice.getAddress(), action);
  }

  private void broadcastUpdateWithDeviceId(final String action, String deviceAddress) {
    CTEK.broadcastUpdate(context, deviceAddress, action);
  }

  private void testbroadcastUpdate(final String action, String mseg) {
    CTEK.testbroadcastUpdate(context, mDevice.getAddress(), action, mseg);
  }

  private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
    CTEK.broadcastUpdate(context, TAG, action, mDevice, characteristic);
  }

  /**
   * Initializes a reference to the local Bluetooth adapter.
   *
   * @return Return true if the initialization is successful.
   */
  public boolean initialize() {
    // For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
    if (mBluetoothManager == null) {
      mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
      if (mBluetoothManager == null) {
        Log.e(TAG, "Unable to initialize BluetoothManager.");
        return false;
      }
    }
    mBluetoothAdapter = mBluetoothManager.getAdapter();

    if (mBluetoothAdapter == null) {
      Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
      return false;
    }
    return true;
  }

  /**
   * Connects to the GATT server hosted on the Bluetooth LE device.
   * <p>
   * // @param device The destination device.
   *
   * @return Return true if the connection is initiated successfully. The connection result
   * is reported asynchronously through the
   * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
   * callback.BluetoothGattService service = mBluetoothGatt.getService(SBADevice.Service.BASE_SERVICE_UUID);
   */

  private void setupCallback() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {

      mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
          super.onScanResult(callbackType, result);
          BluetoothDevice bluetoothDevice = result.getDevice();
//            int rssi = result.getRssi();
//            String name = bluetoothDevice.getName();
            if (arrDeviceMac.contains(bluetoothDevice.getAddress())) {
              //Log.d("BLETest", "Device already in communication");
            }
            else {
              Log.d("Found", "onScanResult device: "+ bluetoothDevice.getAddress());
              arrDeviceMac.add(bluetoothDevice.getAddress());

              //Added intent
              Intent ble_i_ = new Intent(BluetoothLeManager.DEVICE_CONNECTED);
              ble_i_.putExtra("available_devices",arrDeviceMac);
              context.sendBroadcast(ble_i_);
              ///

              // openConnection(bluetoothDevice);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
          Log.d("BLETest", "onBatchScanResults");
          super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
          Log.d("BLETest", "onScanFailed "+errorCode);
          super.onScanFailed(errorCode);
        }
      };
    }
    else {
      leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        }
      };
    }
  }
  int startBTScanCounter =0;
  public void startBTSearch() {
    arrDeviceMac.clear();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {

      Log.i("Started Scan", "Scanning");
      BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
      scanner.flushPendingScanResults(mScanCallback);
      if (scanner != null) {
        ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        ScanSettings settings = scanSettingsBuilder.build();
        ScanFilter uuidFilter = new ScanFilter.Builder().build();
        ArrayList<ScanFilter> filters = new ArrayList<>();
        filters.add(uuidFilter);
        scanner.startScan(filters, settings, mScanCallback);
        Log.i(TAG,"Scan counter "+startBTScanCounter++);
      }
    }
  }

  public void closeAndDisconnectGattServer () {
    if (mBluetoothGatt!=null) {
      mBluetoothGatt.disconnect();
      mBluetoothGatt.close();
      mBluetoothGatt = null;
    }
  }


  public void stopBTSearch() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
      BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
      if (scanner != null) {
        scanner.flushPendingScanResults(mScanCallback);
        scanner.stopScan(mScanCallback);
        Log.i(TAG,"Scan counter "+startBTScanCounter--);
      }
    } else {
      mBluetoothAdapter.stopLeScan(leScanCallback);
    }
  }

  //Added
  public void  disconnectFromGatt () {
   // handler.removeCallbacks(gattServiceTimeout);
    mBluetoothGatt.disconnect();
    mBluetoothGatt.close();
   // mBluetoothGatt = null;
  }

  public boolean connect(Device device, String caller) {
    mDevice = device;
    // Toast.makeText(context, "caller: " + caller, Toast.LENGTH_SHORT).show();
    if (mBluetoothAdapter == null || device.getAddress() == null) {
      Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
      return false;
    }
    // Previously connected device. Should not happen - forget.
    try {
      if (mBluetoothGatt != null) {
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        Log.i(TAG, "Gatt closed in connect.");
      }
    } catch (Exception ignored) {
      mBluetoothGatt = null;
      Log.e(TAG, "Closing issue handled in connect.");
    }

    final BluetoothDevice bleDevice = mBluetoothAdapter.getRemoteDevice(device.getAddress());

    alertBuilder = new AlertDialog.Builder(context);
    Log.i(TAG, "1: Connect to: " + bleDevice.getAddress());
    handler.post(new Runnable() {
      public void run() {
        //This will be executed on main thread using Looper.
//        mBluetoothGatt = bleDevice.connectGatt(context, false, mGattCallback);  Original
        //Added
        bleDevice.createBond();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
          mBluetoothGatt = bleDevice.connectGatt(context, true, mGattCallback, BluetoothDevice.TRANSPORT_LE); //Original autoConnect: False , BluetoothDevice.TRANSPORT_LE-> Worked Fine but works only on BLE , TRANSPORT_AUTO-> Not able to pair device
        } else {
          //Original without if-else
          mBluetoothGatt = bleDevice.connectGatt(context, true, mGattCallback);//Original autoConnect: False
        }
        if (mBluetoothGatt == null) {
          Log.w(TAG, "Failed, creating mBluetoothGatt");
        } else {
          // Set an overall gatt service timeout in case we don't get connected
          // or don't get services or data call-backs.
          handler.removeCallbacks(gattServiceTimeout);
          handler.postDelayed(gattServiceTimeout, CTEK.getGattServiceTimeout(isInLiveMode()));
        }
      }
    });
    return true;
  }

  private boolean writeToKeyHole() {
    if (baseService == null) {
      Log.e(TAG, "Our base service was not found. Not the correct device.");
    }
    else {
      BluetoothGattCharacteristic characteristic = baseService.getCharacteristic(SBADevice.Characteristic.KEY_HOLE_UUID);
      String serial = mDevice.getSerialnumber();
      if (serial != null) {
        Log.d(TAG, String.format("Unlocking with serial %s", serial));
        characteristic.setValue(BondingHelper.MD5EncryptPasscode(serial));
        boolean success = mBluetoothGatt.writeCharacteristic(characteristic);
        Log.d(TAG, String.format("Write to key hole result: %s", success));
        return success;
      }
    }
    return false;
  }

  private void triggerBonding() {
    if (bondingThreadPoolExecutor == null) {
      bondingThreadPoolExecutor = Executors.newScheduledThreadPool(0);
    }
    Log.i(TAG, "7: Trigger pairing, delay 1 second.");
    bondingThreadPoolExecutor.schedule(
        new Runnable() {
          @Override
          public void run() {
            if (baseServiceV2 != null) {
              BluetoothGattCharacteristic uptimeCharacteristic = baseServiceV2.getCharacteristic(SBADevice.Characteristic.UPTIME_UUID_V2);
              mBluetoothGatt.readCharacteristic(uptimeCharacteristic);
              Log.i(TAG, "8: Do/check pairing, read uptime.");
            }
          }
        }, 1400, TimeUnit.MILLISECONDS);  //2 Sec delay
  }

  private void readUptime() {
    if (mBluetoothGatt != null && baseService != null) {
      boolean success = mBluetoothGatt.readCharacteristic(baseService.getCharacteristic(SBADevice.Characteristic.UPTIME_UUID_V2));
      Log.d(TAG, String.format("Uptime read: %s", success));
    }
  }

  private void readHistoryCursor() {
    if (mBluetoothGatt != null && baseService != null) {
      boolean success = mBluetoothGatt.readCharacteristic(baseService.getCharacteristic(SBADevice.Characteristic.VOLTAGE_HISTORY_CURSOR_UUID));
      Log.d(TAG, String.format("Cursor position read: %s", success));
    }
  }

  private void readNumberOfRecords() {
    if (mBluetoothGatt != null && baseService != null) {
      boolean success = mBluetoothGatt.readCharacteristic(baseService.getCharacteristic(SBADevice.Characteristic.NUMBER_OF_RECORDS_UUID));
      Log.d(TAG, String.format("Number of records read: %s", success));
    }
  }

  private void readLiveVoltage() {
    if (mBluetoothGatt != null && baseService != null) {
      boolean success = mBluetoothGatt.readCharacteristic(baseService.getCharacteristic(SBADevice.Characteristic.VOLTAGE_CURRENT_UUID));
      Log.d(TAG, String.format("Live voltage read: %s", success));
      if(success) {
        lastRealLiveVoltageCall = System.currentTimeMillis();
      }
    }
  }

  private void readLiveTemperature() {
    if (mBluetoothGatt != null && baseService != null) {
      boolean success = mBluetoothGatt.readCharacteristic(baseService.getCharacteristic(SBADevice.Characteristic.TEMPERATURE_CURRENT_UUID));
      Log.d(TAG, String.format("Live temperature read: %s", success));
      if(success) {
        lastRealLiveTemperaCall = System.currentTimeMillis();
      }
    }
  }

  private void readInterval() {
    if (mBluetoothGatt != null && baseService != null) {
      boolean success = mBluetoothGatt.readCharacteristic(baseService.getCharacteristic(SBADevice.Characteristic.MEASUREMENT_INTERVAL_UUID));
      Log.d(TAG, String.format("Measurement interval read: %s", success));
    }
  }

  private void readSerialNumber() {
    if (mBluetoothGatt != null && infoService != null) {
      BluetoothGattCharacteristic serialNoCharacteristic = infoService.getCharacteristic(SBADevice.Characteristic.SERIAL_NUMBER_UUID);
      boolean success = mBluetoothGatt.readCharacteristic(serialNoCharacteristic);
      Log.d(TAG, String.format("Serial read: %s", success));
    }
  }

  private void subscribeToTemperatureNotifications() {
    if (mBluetoothGatt != null && baseService != null) {
      BluetoothGattCharacteristic gattChar = baseService.getCharacteristic(SBADevice.Characteristic.TEMPERATURE_CURRENT_UUID);
      boolean success = mBluetoothGatt.setCharacteristicNotification(gattChar, true);
      Log.d(TAG, String.format("Subscribe to temperature service: %s", success));
    }
  }

  private void enableHistoryNotifications() {
    if (mBluetoothGatt != null && baseService != null) {
      BluetoothGattCharacteristic voltageCharacteristic = baseService.getCharacteristic(SBADevice.Characteristic.VOLTAGE_HISTORY_UUID);
      List<BluetoothGattDescriptor> descriptors = voltageCharacteristic.getDescriptors();
      BluetoothGattDescriptor descriptor = descriptors.get(0);
      descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

      // on the onDescriptorWrite() above we call subscribeToVoltageNotifications()
      boolean writeDescriptorSuccess = mBluetoothGatt.writeDescriptor(descriptor);
      Log.d(TAG, String.format("Write to notification descriptor: %s", writeDescriptorSuccess));
    }
  }

  private void subscribeToVoltageNotifications() {
    if (mBluetoothGatt != null && baseService != null) {
      BluetoothGattCharacteristic voltageCharacteristic = baseService.getCharacteristic(SBADevice.Characteristic.VOLTAGE_HISTORY_UUID);
      boolean successVoltage = mBluetoothGatt.setCharacteristicNotification(voltageCharacteristic, true);
      Log.d(TAG, String.format("Subscribe to voltage service: %s", successVoltage));
      writeVoltageCursor();
    }
  }

  private void writeVoltageCursor() {

    if (currentCursor == -1 || historyCursor == -1 ) {
      onDeviceUpdateFail();
      return;
    }
    // Prepare history read command
    byte[] history;
    if (currentCursor > historyCursor) {
      // We need to read until end first
      expectedRecords = 30000 - currentCursor;
      history = CTEK.getHistoryCommand(currentCursor, 30000);
    }
    else {
      expectedRecords += historyCursor - currentCursor;
      history = CTEK.getHistoryCommand(currentCursor, historyCursor);
    }
    if (mBluetoothGatt != null && baseService != null) {
      BluetoothGattCharacteristic voltageWriteCharacteristic = baseService.getCharacteristic(SBADevice.Characteristic.VOLTAGE_HISTORY_WRITE_UUID);
      voltageWriteCharacteristic.setValue(history);
      boolean successWrite = mBluetoothGatt.writeCharacteristic(voltageWriteCharacteristic);
      Log.d(TAG, String.format("Write to voltage write: %s", successWrite));
    }
  }

  /**
   * After using a given BLE device, the app must call this method to ensure resources are
   * released properly.
   */
  public synchronized void close() {
    Log.d(TAG, "close");
    if (bondingThreadPoolExecutor != null) {
      List<Runnable> scheduledBondingTasks = bondingThreadPoolExecutor.shutdownNow();
      if (scheduledBondingTasks.size() > 0) {
        Log.w(TAG, "scheduled tasks canceled count: " + scheduledBondingTasks.size());
      }
      bondingThreadPoolExecutor = null;
    }
    handler.removeCallbacks(gattServiceTimeout);
    try {
      if (mBluetoothGatt != null) {
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        mBluetoothGatt = null;
      }
    } catch (Exception ignored) {
      mBluetoothGatt = null;
    }
  }

  private void onDeviceUpdateFail() {
    task = null;
    Log.d(TAG, "onDeviceUpdateFail");
    //Added
    mBluetoothGatt.disconnect();
    broadcastUpdate(CTEK.ACTION_DEVICE_UPDATE_FAIL);
  }

  private void testonDeviceUpdateFail(String mesg) {
    task = null;
    Log.d(TAG, "onDeviceUpdateFail");
    //Added
    mBluetoothGatt.disconnect();
    testbroadcastUpdate(CTEK.ACTION_DEVICE_UPDATE_FAIL, mesg);
  }

  private void onReadSerialFail() {
    task = null;
    Log.d(TAG, "onReadSerialFail");
    //Added
    mBluetoothGatt.disconnect();
    broadcastUpdate(CTEK.ACTION_GET_SERIAL_FAIL);
  }

  public void getDeviceSerialNumber(Device device) {
    task = Task.READING_SERIAL_NUMBER;
    connect(device,"getDeviceSerialNumber");
  }

  public void pairDevice(Device device) {
    task = Task.PAIRING_DEVICE;
    connect(device,"pairDevice");
  }

  public void updateBatteryData(Device device) {
    /*
     * - Read cursor position from VOLTAGE HISTORY CURSOR
     * - Subscribe to notifications from VOLTAGE HISTORY
     * - Success! subscribed
     * - Write byte array to VOLTAGE HISTORY WRITE
     * - Get notifications from VOLTAGE HISTORY
     * - Feed back data to device database
     * - Send broadcast that "device" is updated
     */
    // Init some variables catching the battery data
    historyCursor = -1;
    voltagesGlobal.clear();
    temperatGlobal.clear();
    currentCursor = device.getReadCursor() == null ? 0 : device.getReadCursor();
    timeOf_currentCursor = device.getUpdated() == null ? -1 : device.getUpdated();
    // Set the expected number in writeVoltageCursor()
    expectedRecords = 0;
    bGapInData = false;
    task = Task.UPDATING_DEVICE;
    connect(device, "updateBatteryData");
  }

  private Runnable gattServiceTimeout = new Runnable() {
    public void run() {
      boolean bDisconnect = true;
      if(isInLiveMode()) {
        long gattServiceTimeoutLIVE  = CTEK.getGattServiceTimeoutLIVE();
        if(lastRealLiveVoltageCall!=0) {
          long now_msecs = System.currentTimeMillis();
          long passed = now_msecs - lastRealLiveVoltageCall;
          bDisconnect = passed > gattServiceTimeoutLIVE;
        }
        if(bDisconnect) {
          Log.d(TAG, "gattServiceTimeout. Live mode. No data during " + (gattServiceTimeoutLIVE / 1000) + " s. Disconnect.");
          Toast.makeText(context, "No data in last" + (gattServiceTimeoutLIVE/1000) + " sec from "+mDevice.getName()+", disconnecting", Toast.LENGTH_SHORT).show();
        }
        else {
          Log.d(TAG, "gattServiceTimeout ignored. Live mode.");
        }
      }

      if(bDisconnect) {
        Log.d(TAG, "BluetoothLeManager timed out - force disconnect.");
        // Close GATT here in case the broadcast receiver is no longer there.
        try {
          if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            Log.i(TAG, "Gatt closed in timeout.");
            //Added
            //Toast.makeText(context,  mDevice.getName()+" device disconnected closing connection", Toast.LENGTH_LONG).show();
            //
          }
        } catch (Exception ignored) {
          mBluetoothGatt = null;
          Log.e(TAG, "Closing issue handled in timeout.");
        }
        broadcastUpdate(CTEK.ACTION_SERVICE_STOPPED);
      }
      else {
        // repost self
        resetServiceTimeout();
      }
      return;
    }
  };

  private void resetServiceTimeout () {
    handler.removeCallbacks(gattServiceTimeout);
    handler.postDelayed(gattServiceTimeout, CTEK.getGattServiceTimeout(isInLiveMode()));
    return;
  }

} // EOClass BluetoothLeManager
