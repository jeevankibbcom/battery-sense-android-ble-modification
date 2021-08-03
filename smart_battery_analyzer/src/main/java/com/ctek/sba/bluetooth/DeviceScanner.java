package com.ctek.sba.bluetooth;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.ctek.sba.bluetooth.SBADevice.Service.BASE_SERVICE_UUID;

/**
 * Created by mfahlen on 2015-10-24.
 *
 * This class scans for Battery Sense senders (devices)
 */

public class DeviceScanner {

    private static final String TAG = DeviceScanner.class.getName();

    // Callback for API level 21 and above (LOLLIPOP)
    private ScanCallback mScanCallback;

    // Callback for API levels 19-20
    private BluetoothAdapter.LeScanCallback mLeScanCallback;

    // Bluetooth adapter to use for scanning
    private BluetoothAdapter mBleAdapter;

    DeviceScanResult mListener;

    public enum ScanMode {
      FAST,
      MEDIUM,
      SLOW
    }

    ScanMode mScanMode;

    public interface DeviceScanResult {
        void processResult(BluetoothDevice bluetoothDevice, int rssi);
    }

    public DeviceScanner(Context context, DeviceScanResult listener, ScanMode scanMode) {
      Log.d(TAG, "Constructor scanMode = " + scanMode.name());

        // Setup the callers listener and scan mode
        mListener = listener;
        mScanMode = scanMode;

        //Added Variables
      final int[] noDeviceMatch = {0};

        // Get the Bluetooth adapter
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBleAdapter = bluetoothManager.getAdapter();

        // Create the appropriate callbacks depending on API level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            mScanCallback = new ScanCallback() {

                @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    // Log.d(TAG, "onScanResult callbackType = " + callbackType);

                    BluetoothDevice bluetoothDevice = result.getDevice();

                    //Added
                //  Log.i("AvailableDevice(OnScan)",bluetoothDevice.getName());
                    ///

//                    if (bluetoothDevice.getAddress().startsWith("2C:FC:E4")) {
                        int rssi = result.getRssi();
                        mListener.processResult(bluetoothDevice, rssi);

                        //Added
//                    }
//                    else {
//                      noDeviceMatch[0]++;
//                      if (noDeviceMatch[0]>50) {
//                      //  Toast.makeText(context, "No CTEK device found nearby"+noDeviceMatch[0], Toast.LENGTH_SHORT).show();
//                        mScanCallback.onScanFailed(12);
//                      }
//                    }
                    ///
                }

                @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    Log.d(TAG, "onBatchScanResults: " + results.size() + " results");
                    for (ScanResult result : results) {
                        BluetoothDevice bluetoothDevice = result.getDevice();

                      //Added
                     // Log.i("AvailableDevice(OnBatch",bluetoothDevice.getAddress());
                        int rssi = result.getRssi();

                        mListener.processResult(bluetoothDevice, rssi);
                    }
                }

                @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
                @Override
                public void onScanFailed(int errorCode) {
//                  //Added working ask
//                  if (errorCode == 12) {
//                      Toast.makeText(context, "No nearby CTEK device found", Toast.LENGTH_LONG).show();
//                    ///
//                  }
//                  else {
                    Log.d(TAG, "LE Scan Failed: " + errorCode);
                  }
             //   }
            };
        }
        else {
            mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {

                    mListener.processResult(bluetoothDevice, rssi);
                }
            };
        }

    }

    public void StartDeviceScan() {
      Log.d(TAG, "StartDeviceScan");
      // Make sure we're running API level 21 or higher to use new scanner API

      //Added to scan near by devices
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {

        // Get scanner to use
        BluetoothLeScanner scanner = mBleAdapter.getBluetoothLeScanner();

        // We will not get a scanner if bluetooth is turned off. User should have been notified.
        if (scanner != null) {
          // Set type of scan
          ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();

          // Choose scan duty cycle, low power, balanced or low latency.
          if (mScanMode == ScanMode.FAST) {
            scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
          }
          else if (mScanMode == ScanMode.MEDIUM) {
            scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
          }
          else {
            scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
          }

          //            scanSettingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES); //
          //            scanSettingsBuilder.setScanResultType(ScanSettings.SCAN_RESULT_TYPE_FULL); //

          // EA 30-Nov-2016. This do not help.
          /*
          if (Build.VERSION.SDK_INT >= 23) {
            scanSettingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
            scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
          }
          */

          ScanSettings settings = scanSettingsBuilder.build();

          // UUID to scan for
          //              ParcelUuid uuid = new ParcelUuid(SBADevice.Service.ADVERTISEMENT_SERVICE_UUID);

          // Create uuid filter
          ScanFilter uuidFilter = new ScanFilter.Builder()
              //                .setServiceUuid(uuid)
              .build();

          // Create list of filters and add our uuid filter
          ArrayList<ScanFilter> filters = new ArrayList<>();
          filters.add(uuidFilter);

          /*
          ParcelUuid uuid_1 = new ParcelUuid(SBADevice.Service.BASE_SERVICE_UUID);
          filters.add(new ScanFilter.Builder().setServiceUuid(uuid_1).build());

          ParcelUuid uuid_2 = new ParcelUuid(SBADevice.Service.DEVICE_INFORMATION_SERVICE_UUID);
          filters.add(new ScanFilter.Builder().setServiceUuid(uuid_2).build());
          */

          // Do the scan
          scanner.startScan(filters, settings, mScanCallback);
        }
      }
      else {
        // UUID to scan for
        // UUID[] services = {SBADevice.Service.ADVERTISEMENT_SERVICE_UUID};

        // Do the scan
        //mBleAdapter.startLeScan(services, mLeScanCallback);

        mBleAdapter.startLeScan(null, mLeScanCallback);
      }
    }

    public void StopDeviceScan() {
      Log.d(TAG, "StopDeviceScan");
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        BluetoothLeScanner scanner = mBleAdapter.getBluetoothLeScanner();
        if (scanner != null) {
          scanner.stopScan(mScanCallback);
        }
      } else {
        mBleAdapter.stopLeScan(mLeScanCallback);
      }
    }

} // EOClass DeviceScanner
