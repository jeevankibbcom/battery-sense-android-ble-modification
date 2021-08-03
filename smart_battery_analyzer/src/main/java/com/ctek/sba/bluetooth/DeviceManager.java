package com.ctek.sba.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import com.ctek.sba.io.dao.DeviceRepository;
import com.ctek.sba.ui.LostSenderController;

import java.util.List;

import de.greenrobot.event.EventBus;
import greendao.Device;

/*
  Device manager
  The device manager handle three different tasks: find serial number, pair with device and update battery data.

  Device Manager uses BlueToothLeManager for BLE-communicaitons, DeviceList for storing BLE search results and
  for list of senders that need to be updated (based on update interval). UpDateManager is used for storing new
  voltage data to the database.
 */

public class DeviceManager {

  private static final String TAG = DeviceManager.class.getName();

  private static DeviceManager self;

  private final Context mContext;

  public static final String ACTION_DEVICE_UPDATED = "com.ctek.sba.ACTION_DEVICE_UPDATED";
  public static final String ACTION_DEVICE_FOUND = "com.ctek.sba.ACTION_DEVICE_FOUND";
  public static final String ACTION_DEVICE_DISCONNECTED = "com.ctek.sba.ACTION_DEVICE_DISCONNECTED";

  public static final String EXTRA_DEVICE_ID = "com.ctek.sba.DEVICE_ID";

  // The tasks defined for the Device Manager
  public enum Task {
    PAIR_WITH_DEVICE,
    FIND_SERIAL_NUMBER,
    UPDATE_BATTERY_DATA,  // Not used here.
  }
  // When task is null the Device Manager could be doing background wait.
  private Task task;
  public Task  getTaskRunning () { return task; }

  // Scanner used when finding devices
  private DeviceScanner mScanner;
  // List of devices found when scanning
  private DeviceList mDeviceList = new DeviceList();
  // Period to scan before giving up with timeout.
  private static final long SCAN_PERIOD = 30000;
  // Handler for timeout
  private Handler handler = new Handler();

  // Serial number of device when we are trying to find
  // or null when we look for a disclosed serial number.
  private String mSerial;
  // The device we are currently connecting to.
  private Device mDevice;

  // List of devices that need an update and counter to know when done.
  private DeviceList mDevicesUpdateList = new DeviceList();
  private int mDevicesToUpdate = 0;

  // Timing of background updates used for test
//  protected static final long SCAN_INTERVAL = 30*1000;      // 30 seconds
//  protected static final long UPDATE_INTERVAL = 5*60*1000;   // 5 minutes

  // Timing of background updates
  protected static final long SCAN_INTERVAL = 3*60*1000L;     //  3 minutes
  protected static final long UPDATE_INTERVAL = 60*60*1000L;  // 60 minutes

  // Bluetooth manager for connecting to devices
  private CTEKBLEManagerInterface bleManager; // Bluetooth manager for connecting to devices

  // EA 14-Oct-2016. Special real-time mode.
  private String macSelectedInDetails;

  private LostSenderController.FoundDevicesListener mFoundDevicesListener;


  // Device Manager constructor
  private DeviceManager(Context context) {
    this.mContext = context.getApplicationContext();
  }

  // Mechanism for having a single instance of the Device Manager.
  public static synchronized DeviceManager init(Context context) {
    if (self == null) {
      self = new DeviceManager(context);
    }
    return self;
  }

  // Provide the Device Manager instance
  public static DeviceManager getInstance() {
    return self;
  }

  // Start the task of pairing with a device
  // with serial number given by the user or as result of previous find serial task.
  public void bondDevice(String serial) {
    // Cancel any previous task.
    stop();

    // Start the pairing task.
    task = Task.PAIR_WITH_DEVICE;

    if (mDevice != null) {
      // Pull serial from the device if we got one
      mSerial = mDevice.getSerialnumber();
    }
    // Check if the device has the serial number we are looking for.
    if (mDevice != null && mSerial != null && mSerial.equals(serial)) {
      // No need to search. Use mDevice.
      startTask();
    }
    else {
      // Forget any previous device and start search for this serial number.
      mSerial = serial;
      mDevice = null;
      startSearch(DeviceScanner.ScanMode.MEDIUM);  //Original FAST
    }
  }

  public void bondDeviceWithMac(Device device) {
    // Cancel any previous task.
    stop();

    // Start the pairing task.
    task = Task.PAIR_WITH_DEVICE;
    mDevice = device;

    if (mDevice != null) {
      // Pull serial from the device if we got one
      mSerial = mDevice.getSerialnumber();
    }
    // Check if the device has the serial number we are looking for.
    if (mDevice != null) {
      // No need to search. Use mDevice.
      startTask();
    }
    else {
      // Forget any previous device and start search for this serial number.
//      mSerial = serial;
      mDevice = null;
      startSearch(DeviceScanner.ScanMode.MEDIUM);  //Original FAST
    }
  }

  // Start the task of finding a disclosed serial number
  // which is possible during the first five minutes after power on (of sender)
  public void getSerialNumber() {
    // Cancel any previous task.
    stop();

    // Start the find serial task.
    task = Task.FIND_SERIAL_NUMBER;
    // Forget any previous serial number and/or device and start search.
    mSerial = null;
    mDevice = null;
    startSearch(DeviceScanner.ScanMode.MEDIUM); //Original FAST
  }



  // Abort any ongoing task
  public void stop() {
    stopTask();
    stopSearch();
    closeConnection();

    DeviceManagerHiQ.getInstance().stop();
  }

  private void buildDeviceUpdateList(Long interval, boolean bForceUpdate) {
    // Get current time
    Long now = System.currentTimeMillis();
    // Clear list to start fresh
    mDevicesUpdateList.clear();

    // Get the devices handled by this phone
    List<Device> myDevices = DeviceRepository.getAllDevices(mContext);
    // Find devices that have not been updated since the given interval
    for (Device device : myDevices) {
      if(macSelectedInDetails!=null) {
        if(device.getAddress().equals(macSelectedInDetails)) {
          mDevicesUpdateList.add(device); // only one survives
          break;
        }
      }
      else {
        // If this device was never updated, try now
        if (device.getUpdated() == null) {
          mDevicesUpdateList.add(device);
        }
        else {
          if(bForceUpdate) {
            mDevicesUpdateList.add(device);
          }
          else {
            // If not updated for given time, try now
            Long elapsed = now - device.getUpdated();
            if ((elapsed > interval) || (device.getAddress().equals(macSelectedInDetails))) {
              mDevicesUpdateList.add(device);
            }
          }
        }
      }
    }
    // Set counter to be able to stop searching when done.
    mDevicesToUpdate = mDevicesUpdateList.size();
    Log.d(TAG, "buildDeviceUpdateList mDevicesToUpdate = " + mDevicesToUpdate);
    for(int kk=0;kk<mDevicesToUpdate;++kk) {
      Device deviceKK = mDevicesUpdateList.getDevice(kk);
      Log.d(TAG, "Device " + (kk+1) + "/. serial = " + deviceKK.getSerialnumber() + " name = " + deviceKK.getName() + " " + deviceKK.getAddress());
    }
  }

  // ********************************************************************************************
  // Section for finding devices nearby.

  private void startSearch(DeviceScanner.ScanMode scanMode) {
    // Remove any old devices in the list
    mDeviceList.clear();
    try {
      // Create a scanner.
      mScanner = new DeviceScanner(mContext, new ProgressListener(), scanMode);
      // Start scanning
      mScanner.StartDeviceScan();
      // Set a timeout for the scan.
      handler.postDelayed(onSearchTimeout, SCAN_PERIOD);
    }
    catch (Exception ignored) {}
  }

  private void pauseSearch() {
    if (mScanner != null) {
      mScanner.StopDeviceScan();
      Log.d(TAG, "pauseSearch");
    }
    // Remove the timeout
    handler.removeCallbacks(onSearchTimeout);
  }

  private void resumeSearch() {
    if (mScanner != null) {
      mScanner.StartDeviceScan();
      Log.d(TAG, "resumeSearch");
    }
    // Set a timeout for the scan.
    handler.postDelayed(onSearchTimeout, SCAN_PERIOD);
  }

  private void stopSearch() {
    // Stop scanning
    if (mScanner != null) {
      mScanner.StopDeviceScan();
      mScanner = null;
      Log.d(TAG, "stopSearch");
    }
    // Remove the timeout
    handler.removeCallbacks(onSearchTimeout);
  }

  private Runnable onSearchTimeout = new Runnable() {
    public void run() {
      stopSearch();
      Log.w(TAG, "Timeout. Search done. No more sender found.");

      // Clear any partial results
      mDevice = null;
      mSerial = null;

      if (task == Task.FIND_SERIAL_NUMBER) {
        reportSerialFailed();
      }
      else if (task == Task.PAIR_WITH_DEVICE) {
        reportPairingFailed();
      }
      else if (task == Task.UPDATE_BATTERY_DATA) {
        // ### This task is not used here. // DeviceManagerHiQ.getInstance().scheduleNewUpdate(); // UPDATE_BATTERY_DATA - should not be the case
      }

      stopTask();
    }
  };

  private class ProgressListener implements DeviceScanner.DeviceScanResult {

    @Override
    public void processResult(BluetoothDevice bluetoothDevice, int rssi) {
      // Get the address of the found sender
      String address = bluetoothDevice.getAddress();
      String name = bluetoothDevice.getName();
      // Check if the device is already added to the search list
      // Only deal with it once
      Device device = mDeviceList.getDevice(address);
      if (device == null) {
        // Create a new device and add it to the list
        device = new Device();
        device.setSerialnumber(mSerial); // Set same to all for later pairing attempts
        device.setAddress(bluetoothDevice.getAddress());
        if (name != null) device.setName(name);
        else device.setName("");
        device.setRssi(rssi);

        // Put all devices in the list
        mDeviceList.add(device);
        Log.i(TAG, "Sender added to list. " + address + " name = " + name);
        if (mDeviceList.size() > 0) {
          EventBus.getDefault().post(new GotDevicesScanList(mDeviceList));
        }

        if (task == Task.UPDATE_BATTERY_DATA) {
          // Check if this sender is scheduled for update.
          // Here we get the correct serial number.
          mDevice = mDevicesUpdateList.getDevice(address);
          if (mDevice != null) {
            pauseSearch();

            Intent broadcast = new Intent(ACTION_DEVICE_FOUND);
            broadcast.putExtra(EXTRA_DEVICE_ID, mDevice.getId());
            mContext.sendBroadcast(broadcast);

            startTask();
          }
        }
        else { // FIND or PAIR
          mDevice = device;
//          pauseSearch();
//          startTask();
        }

      }
      else {
        // Log.d(TAG, "onScanResult: Sender already listed. " + address + " rssi = " + rssi + " name = " + name);
      }
    }
  }

  // ********************************************************************************************
  // Initialize/Close connection to the bluetooth manager.

  // Open connection before connecting to the first in the list.
  private void openConnection() {
    // Get the Bluetooth handler, initialize and connect
    // bleManager = CTEK.bUseBluetoothLeManager ? BluetoothLeManager.getInstance(mContext) : CTEKBLEManager.getInstance(mContext);
    bleManager = BluetoothLeManager.getInstance(mContext);
    if (!bleManager.initialize()) {
      Log.d(TAG, "Unable to initialize Bluetooth");
    }
    // Setup callback for Bluetooth actions
    mContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    Log.d(TAG, "GATT receiver registered");
    return;
  }

  // Close connection after last device is handled.
  private void closeConnection() {
    try {
      mContext.unregisterReceiver(mGattUpdateReceiver);
      Log.d(TAG, "GATT receiver un-registered");

    }
    catch (Exception ignored) {}

    if (bleManager != null) {
      bleManager.close();
      bleManager = null;
    }
  }

  // Start/continue task using mDevice
  private void startTask() {
    if (mDevice != null) {
      openConnection();
      if (bleManager != null) {

        if (task == Task.FIND_SERIAL_NUMBER) {
          // Find serial
          bleManager.getDeviceSerialNumber(mDevice);
          Log.d(TAG, "Started new serial no search!");
        }

        else if (task == Task.PAIR_WITH_DEVICE) {
          // Pair with device
          bleManager.pairDevice(mDevice);
          Log.d(TAG, "Started attempt to pair!");
        }

        else if ( task == Task.UPDATE_BATTERY_DATA) {
          if(macSelectedInDetails!=null) {
            // Toast.makeText(mContext, R.string.enter_live_mode, Toast.LENGTH_SHORT).show();
            // Using spinner instead.
          }
          bleManager.updateBatteryData(mDevice);
          Log.d(TAG, "Started update of battery data!");

          if(macSelectedInDetails==null) {
            // One down, may be some more to go.
            mDevicesToUpdate = mDevicesToUpdate - 1;
          }
        }
        else {
          // Unknown task. Close connection.
          closeConnection();
        }

      }
    }
    else {
      Log.w(TAG, "startTask with device=null. IGNORED.");
    }
  }

  // Start task depending on if serial number is known or not
  private void stopTask() {
    task = null;
  }

    // Class used to signal pairing result to NewDeviceActivity
  public class PairingComplete {
    // null means pairing failed
    public final Device device;
    public PairingComplete(Device device) {
      this.device = device;
    }
  }

  private void reportDevicePaired(Device device) {
    EventBus.getDefault().post(new PairingComplete(device));
  }

  private void reportPairingFailed() {
    EventBus.getDefault().post(new PairingComplete(null));
  }

  // Class used to signal serial search result to LostSenderController
  public class SearchComplete {
    // null means search failed
    public final String serial;
    public SearchComplete(String serial) {
      this.serial = serial;
    }
  }

  public class GotDevicesScanList {
    // null means search failed
    public final DeviceList mDeviceList;
    public GotDevicesScanList(DeviceList mDeviceList) {
      this.mDeviceList = mDeviceList;
    }
  }

  private void reportSerialFound(String serial) {
    EventBus.getDefault().post(new SearchComplete(serial));
  }

  public void reportConnectTo(String serial) {
    EventBus.getDefault().post(new SearchComplete(serial));
  }

  private void reportSerialFailed() {
    EventBus.getDefault().post(new SearchComplete(null));
  }

  // These are the Bluetooth manager actions we shall listen to.
  private static IntentFilter makeGattUpdateIntentFilter() {
    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(CTEK.ACTION_SERIAL_RECEIVED);
    intentFilter.addAction(CTEK.ACTION_GET_SERIAL_FAIL);

    intentFilter.addAction(CTEK.ACTION_KEY_UNLOCKED);
    intentFilter.addAction(CTEK.ACTION_KEY_UNLOCK_FAIL);

    intentFilter.addAction(CTEK.ACTION_DEVICE_UPDATED);
    intentFilter.addAction(CTEK.ACTION_DEVICE_UPDATE_FAIL);

    intentFilter.addAction(CTEK.ACTION_SERVICE_STOPPED);
    return intentFilter;
  }

  private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();

      if (CTEK.ACTION_SERIAL_RECEIVED.equals(action)) {
        closeConnection();
        if (mDevice != null) {
          // We managed to read the serial number, empty or with data
          String serial = intent.getStringExtra(CTEK.EXTRA_SERIAL_NUMBER);

          if (serial == null || serial.length() == 0) {
            // Not a recently connected device, try next
            Log.w(TAG, "No serial no. Try next.");
            resumeSearch();
          } else {
            // Serial found. Close with serial number. Store serial and device.
            mSerial = serial;
            // Set the serial number on the current device
            mDevice.setSerialnumber(serial);
            reportSerialFound(serial);
            stopTask();
          }
        }
      }
      else if (CTEK.ACTION_GET_SERIAL_FAIL.equals(action)) {
        closeConnection();
        if (mDevice != null) {
          // We got an error code when reading serial number. Try next.
          Log.w(TAG, "Serial number read fail.");
          resumeSearch();
        }
      }

      else if (CTEK.ACTION_KEY_UNLOCKED.equals(action)) {
        closeConnection();
        if (mDevice != null) {
          // Pairing confirmed and completed.
          reportDevicePaired(mDevice);
          stopTask();
        }
      }
      else if (CTEK.ACTION_KEY_UNLOCK_FAIL.equals(action)) {
        closeConnection();
        if (mDevice != null) {
          // We got an error code when pairing. Try next.
          Log.w(TAG, "Pairing failed.");
          resumeSearch();
        }
      }

      else if (CTEK.ACTION_DEVICE_UPDATED.equals(action)) {
        if(macSelectedInDetails==null) {
          closeConnection();
        }
        if (mDevice != null) {
          String deviceAddress = intent.getStringExtra(CTEK.EXTRA_BLE_DEVICE_ADDRESS);
          // Update of battery data completed.
          if (mDevice.getAddress().equals(deviceAddress)) {
            double[] newVoltages = intent.getDoubleArrayExtra(CTEK.EXTRA_VOLTAGE_ARRAY);
            int cursor = intent.getIntExtra(CTEK.EXTRA_CURSOR, -1);
            boolean gapDetected = intent.getBooleanExtra(CTEK.EXTRA_GAP_IN_DATA_DETECTED, false);

            CTEK.storeData(mContext, TAG, mDevice, newVoltages, cursor, gapDetected);

            // Let someone know this happened
            Intent broadcast = new Intent(ACTION_DEVICE_UPDATED);
            broadcast.putExtra(EXTRA_DEVICE_ID, mDevice.getId());
            mContext.sendBroadcast(broadcast);

            Log.w(TAG, "Update completed MAC = " + deviceAddress);

            if (mDevicesToUpdate > 0) {
              if(macSelectedInDetails!=null) {
                // startTask();
                bleManager.updateBatteryData(mDevice);
                Log.d(TAG, "Started update of battery data!");
              }
              else {
                resumeSearch();
              }
            }
            else {
              stopTask();
              // ### scheduleNewScan();
            }
          }
        }
      }
      else if (CTEK.ACTION_DEVICE_UPDATE_FAIL.equals(action)) {
        closeConnection();
        if (mDevice != null) {
          Log.w(TAG, "Update failed.");

          // Update of battery data failed. Let caller know and resume to find more devices.
          // Should maybe decrease the count of devices to update here.
          Intent broadcast = new Intent(ACTION_DEVICE_DISCONNECTED);
          broadcast.putExtra(EXTRA_DEVICE_ID, mDevice.getId());
          mContext.sendBroadcast(broadcast);

          resumeSearch();
        }
      }

      else if (CTEK.ACTION_SERVICE_STOPPED.equals(action)) {
        closeConnection();
        if (mDevice != null) {
          // We got disconnected or service discovery took too long. Try next.
          String reason = intent.getStringExtra(CTEK.EXTRA_BLE_REASON);
          // Log.w(TAG, "Got disconnected or service discovery timed out.");
          Log.d(TAG, "ACTION_SERVICE_STOPPED mac = " + mDevice.getAddress() + " Reason: " + reason);

          // We got disconnected. Let caller know and resume to find more devices.
          Intent broadcast = new Intent(ACTION_DEVICE_DISCONNECTED);
          broadcast.putExtra(EXTRA_DEVICE_ID, mDevice.getId());
          mContext.sendBroadcast(broadcast);

          resumeSearch();
        }
      }
    }
  };
}
