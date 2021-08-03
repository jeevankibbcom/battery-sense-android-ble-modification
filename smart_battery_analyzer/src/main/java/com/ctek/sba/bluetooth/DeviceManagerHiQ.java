package com.ctek.sba.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;

import com.ctek.sba.io.dao.DeviceRepository;
import com.ctek.sba.util.SettingsHelper;

import java.util.List;

import greendao.Device;

/**
 * Created by evgeny.akhundzhanov on 18.10.2016.
 *
 * BLE Search is not required for UPDATE_BATTERY_DATA task.
 * Update code was transferred from DeviceManager and remastered.
 * Live view mode added.
 */
public class DeviceManagerHiQ {

  private final String TAG = this.getClass().getName();

  public static final String ACTION_UPDATE_COMPLETE  = "com.ctek.sba.ACTION_UPDATE_COMPLETE";
  public static final String EXTRA_UPDATE_RESULT     = "com.ctek.sba.EXTRA_UPDATE_RESULT";
  public static final String EXTRA_UPDATE_SUCCESS_COUNT   = "com.ctek.sba.EXTRA_UPDATE_SUCCESS_COUNT";

  public static final String ACTION_UPDATE_DEVICES_STATE  = "com.ctek.sba.ACTION_UPDATE_DEVICES_STATE";
  public static final String EXTRA_UPDATE_DEVICES_COUNT   = "com.ctek.sba.EXTRA_UPDATE_DEVICES_COUNT";
  public static final String EXTRA_UPDATE_DEVICE_STATE    = "com.ctek.sba.EXTRA_UPDATE_DEVICE_STATE";

  //Added
  public static final String START_LIVE_MODE = "com.ctek.sba.START_LIVE_MODE";
  public static final String LIVE_MODE_ENDED = "com.ctek.sba.LIVE_MODE_ENDED";
  ///

  public static final long INTERVAL_1_MINUTE_MSECS  = 60*1000L;
  public static final long INTERVAL_5_MINUTES_MSECS =  5 * INTERVAL_1_MINUTE_MSECS;
  public static final long INTERVAL_60_MINUTES_MSECS= 60 * INTERVAL_1_MINUTE_MSECS;

  private enum EForceUpdate {
    FORCE_UPDATE_NOT,
    FORCE_UPDATE_YES,
  };

  Handler handler = new Handler();

  // these 2 are used for dummy devices
  public static final String DUMMY_DEVICE_MAC_1 = "01:01:01:01";
  public static final String DUMMY_DEVICE_MAC_2 = "01:01:01:02";

  private boolean isValidBluetoothAddress (String mac) {
    return !mac.equals(DUMMY_DEVICE_MAC_1) && !mac.equals(DUMMY_DEVICE_MAC_2);
  }


  private static DeviceManagerHiQ self;

  // Device Manager constructor
  private DeviceManagerHiQ (Context context) {
    this.mContext = context.getApplicationContext();
    // bleManager = CTEK.bUseBluetoothLeManager ? BluetoothLeManager.getInstance(mContext) : CTEKBLEManager.getInstance(mContext);
    bleManager = BluetoothLeManager.getInstance(mContext);
    if (!bleManager.initialize()) {
      Log.e(TAG, "Unable to initialize Bluetooth");
    }
  }

  public static synchronized DeviceManagerHiQ init(Context context) {
    if (self == null) {
      self = new DeviceManagerHiQ(context);
    }
    return self;
  }

  public static DeviceManagerHiQ getInstance() {
    return self;
  }

  private final Context mContext;
  private DeviceManager.Task task;
  private DeviceUpdateList mDevicesUpdateList = new DeviceUpdateList(); // buildDeviceUpdateList() creates this.

  private CTEKBLEManagerInterface bleManager; // Bluetooth manager for connecting to devices

  private Updater updater = new Updater();
  public void setScheduleUpdates (boolean bScheduleUpdates) {
    updater.setScheduleUpdates(bScheduleUpdates);
  }

  // Update schedule functions. Obsolete.
  // Now CTEKUpdateService do the job.
  private class Updater {

    private Handler handler;
    private boolean bScheduleUpdates;

    private Runnable startNewUpdate = new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "startNewUpdate (background).");
        updateDevices("AUTO", INTERVAL_60_MINUTES_MSECS, DeviceScanner.ScanMode.MEDIUM, EForceUpdate.FORCE_UPDATE_NOT);
      }
    };

    public Updater () {
      handler = new Handler();
      bScheduleUpdates = false;
    }
    void setScheduleUpdates (boolean value) {
      bScheduleUpdates = value;
    }
    void scheduleNewUpdate(){
      if(bScheduleUpdates) {
        boolean bSettingUpdateInBG = SettingsHelper.getBackgroundUpdate(mContext);
        if(bSettingUpdateInBG) {
          cancelBackgroundUpdate(); // remove previously posted updates
          startBackgroundUpdate();  // create new
        }
        else {
          Log.d(TAG, "scheduleNewUpdate IGNORED as setting is not set. ");
        }
      }
      return;
    }

    void startBackgroundUpdate() {
      if(bScheduleUpdates) {
        Log.d(TAG, "scheduleNewUpdate in " + (DeviceManager.SCAN_INTERVAL / 1000) + " s.");
        handler.postDelayed(startNewUpdate, DeviceManager.SCAN_INTERVAL);
      }
    }
    void cancelBackgroundUpdate() {
      handler.removeCallbacks(startNewUpdate);
    }
  };

  // Live view mode. ForceUpdate is not required for this mode.
  private String macSelectedInDetails;            // Special real-time mode. Selected device only.
  public boolean isInLiveMode () {
    if (macSelectedInDetails!=null)
      return true;
    //return macSelectedInDetails!=null;
    else
      return false;
  }
  public void stopLiveMode () {
    startLiveMode(null);
    //Added check
//    BluetoothLeManager.getInstance(mContext).disconnectFromGatt();
    ///
  }

  public void liveModeUpdateBattery (String mac) {
    String taskName = (task != null) ? task.name() : null;
    if (taskName != null) {
      Log.d(TAG, "task " + taskName + " stopped.");
      stop();
    }
    else {
      updater.cancelBackgroundUpdate();
    }
    updateAllDevices("LIVE MODE", mac);
  }

  public void startLiveMode(String mac) {
    String taskName = (task != null) ? task.name() : null;
    if (taskName != null) {
      Log.d(TAG, "task " + taskName + " stopped.");
      stop();
    }
    else {
      updater.cancelBackgroundUpdate();
    }

    Log.d(TAG, "updateSelectedDevice mac = " + mac);
    String oldSelectedMac = macSelectedInDetails;
    Log.i(TAG,"oldSelectedDeviceMac = "+oldSelectedMac);
    macSelectedInDetails = mac;

//    bleManager.startLiveMode(mac);
    if(mac!=null) {
      Log.d(TAG, "LIVE MODE started.");
      bleManager.startLiveMode(mac);
      updateAllDevices("LIVE MODE", mac);   //Originally Present
    }
    else {
      if(oldSelectedMac!=null) {
        // stop live mode
        registerResultAndContinueUpdate(oldSelectedMac, true);
        Log.d(TAG, "LIVE MODE ended.");
        //Added
        bleManager.startLiveMode(null);
        Intent i_ = new Intent(LIVE_MODE_ENDED);
        mContext.sendBroadcast(i_);

        ///
      }
    }
    return;
  }

  public void stopBLEConnection() {
    clearTask();
    stopListeningBLEManagerDisconnectAndCloseGatt();
    updater.cancelBackgroundUpdate();

  }

  // Called when app is started or main view is resumed.
  // Only update those who have not been updated for 5 minutes.
  public void updateAllDevices(String caller, String mac) {

    if (mac.length() > 0) {
      macSelectedInDetails = mac;
    }

    if (caller == "LIVE MODE") {
      updateDevices(caller, INTERVAL_5_MINUTES_MSECS, DeviceScanner.ScanMode.MEDIUM, EForceUpdate.FORCE_UPDATE_YES);
    } else if (caller == "UPDATE DEVICE") {
      updateDevices(caller, INTERVAL_5_MINUTES_MSECS, DeviceScanner.ScanMode.MEDIUM, EForceUpdate.FORCE_UPDATE_YES);
    }
//  else if (caller == "noDevice") {
//      updateDevices(caller, INTERVAL_5_MINUTES_MSECS, DeviceScanner.ScanMode.MEDIUM, EForceUpdate.FORCE_UPDATE_YES);
//    } else if (caller == "Startup"){
//      //updateDevices(caller, INTERVAL_5_MINUTES_MSECS, DeviceScanner.ScanMode.MEDIUM, EForceUpdate.FORCE_UPDATE_NOT);  //Original
//    } else {
//
//    }
  }

  // --> Invalid. Called when app is started or main view is resumed.
  // Called when user does PullToRefresh.
  // Only update those who have not been updated for 1 minute.
  public void refreshAllDevices(String caller) {
    updateDevices(caller, INTERVAL_1_MINUTE_MSECS, DeviceScanner.ScanMode.MEDIUM, EForceUpdate.FORCE_UPDATE_YES);
  }
  ///

  // Start the task of updating battery data.
  // Only try to update those that has not been updated since the given interval
  // bForceUpdate = true --> forces update all. Any current task is killed.
  private void updateDevices (String caller, Long updateInterval, DeviceScanner.ScanMode scanMode, EForceUpdate eForceUpdate) {
    // Only start update if no task is running.
    String taskName = (task != null) ? task.name() : null;
    if (taskName != null) {
      Log.d(TAG, "updateDevices IGNORED as task " + taskName + " is running.");
      if(eForceUpdate==EForceUpdate.FORCE_UPDATE_YES) {
        stop();
      }
      else {
        return;
      }
    }

    Log.d(TAG, "updateDevices (" + caller + ") interval =  " + (updateInterval / 1000) + " s. ForceUpdate = " + (eForceUpdate==EForceUpdate.FORCE_UPDATE_YES));
    buildDeviceUpdateList(updateInterval, eForceUpdate);

    if (mDevicesUpdateList.size() > 0) {
      task = DeviceManager.Task.UPDATE_BATTERY_DATA;

      continueUpdate(); // Start the update task. //Original
    }
    else {
      updater.scheduleNewUpdate();
    continueUpdate(); // This will do the job. //Original
    }
    return;
  }

  // Abort any ongoing task
  public void stop() {
    clearTask();
    stopListeningBLEManagerDisconnectAndCloseGatt();
    updater.cancelBackgroundUpdate();
  }

  private void clearTask() {
    task = null;
  }

  public void cleanTaskIfAny () {
    stop();
    mDevicesUpdateList.clear();
    resendUpdateStates();
    return;
  }


  private void buildDeviceUpdateList(Long interval, EForceUpdate eForceUpdate) {
    Long now = System.currentTimeMillis();  // Get current time
    mDevicesUpdateList.clear();             // Clear list to start fresh

    // Get the devices handled by this phone.
    List<Device> myDevices = DeviceRepository.getAllDevices(mContext);
    // Find devices that have not been updated since the given interval
    for (Device device : myDevices) {
      //Original----------
      if(macSelectedInDetails!=null) {
        if(device.getAddress().equals(macSelectedInDetails)) {
          if(isValidBluetoothAddress(macSelectedInDetails)) {
            mDevicesUpdateList.add(device); // only one survives
            //Log
            Log.i(TAG,"Device to update: " + mDevicesUpdateList.toString());
          }
          else {
            // ignore live mode for dummy devices
          }
          break;
        }
      }
      else {
        // If this device was never updated, try now
        if (device.getUpdated() == null) {
          mDevicesUpdateList.add(device);
        }
        else {
          if(eForceUpdate==EForceUpdate.FORCE_UPDATE_YES) {
            mDevicesUpdateList.add(device);
          }
          else {
            // If not updated for given time, try now.
            Long elapsed = now - device.getUpdated();
            if (elapsed > interval) {
              mDevicesUpdateList.add(device);
            }
          }
        }
      }
      //-----------------
    }

    mDevicesUpdateList.sortByUpdateTimeDESC();
    mDevicesUpdateList.reportList(TAG);
    return;
  }

  private void startListeningBLEManager() {
    /*
    // Get the Bluetooth handler, initialize and connect
    bleManager = CTEKBLEManager.getInstance(mContext);
    if (!bleManager.initialize()) {
      Log.e(TAG, "Unable to initialize Bluetooth");
      return;
    }
    */
    mContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    Log.d(TAG, "GATT receiver registered.");
    return;
  }

  private void stopListeningBLEManagerDisconnectAndCloseGatt() {
    try {
      //Added
     // BluetoothLeManager.getInstance(mContext).disconnectFromGatt();
      ///
      mContext.unregisterReceiver(mGattUpdateReceiver);
      Log.d(TAG, "GATT receiver un-registered.");
    }
    catch (Exception ignored) {}

    if(bleManager!=null) {
      bleManager.close();
    }
    return;
  }

  private IntentFilter makeLiveModeStartIntentFilter() {
    IntentFilter filter = new IntentFilter();
    filter.addAction(DeviceManagerHiQ.START_LIVE_MODE);
    return filter;
  }

  private IntentFilter makeGattUpdateIntentFilter() {
    IntentFilter intentFilter = new IntentFilter();
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
      final String mac = intent.getStringExtra(CTEK.EXTRA_BLE_DEVICE_ADDRESS);
      boolean bUpdateFailed = false;
      if (CTEK.ACTION_SERIAL_RECEIVED.equals(action)) {
        String serial = intent.getStringExtra(CTEK.EXTRA_SERIAL_NUMBER);
        Log.d(TAG, "ACTION_SERIAL_RECEIVED mac = " + mac + " serial = " + serial);
      }
      else if (CTEK.ACTION_GET_SERIAL_FAIL.equals(action)) {
        Log.d(TAG, "ACTION_GET_SERIAL_FAIL mac = " + mac);
        bUpdateFailed = true;
      }
      else if (CTEK.ACTION_KEY_UNLOCKED.equals(action)) {
        Log.d(TAG, "ACTION_KEY_UNLOCKED");
      }
      else if (CTEK.ACTION_KEY_UNLOCK_FAIL.equals(action)) {
        Log.d(TAG, "ACTION_KEY_UNLOCK_FAIL mac = " + mac);
        bUpdateFailed = true;
      }
      else if (CTEK.ACTION_DEVICE_UPDATE_FAIL.equals(action)) {
        Log.d(TAG, "ACTION_DEVICE_UPDATE_FAIL mac = " + mac);
        bUpdateFailed = true;
      }
      else if (CTEK.ACTION_SERVICE_STOPPED.equals(action)) {
        // disconnected, timeout or error
        String reason = intent.getStringExtra(CTEK.EXTRA_BLE_REASON);
        Log.d(TAG, "ACTION_SERVICE_STOPPED mac = " + mac + " Reason: " + reason);
        bUpdateFailed = true;
      }
      else if (CTEK.ACTION_DEVICE_UPDATED.equals(action)) {
        Log.d(TAG, "ACTION_DEVICE_UPDATED mac = " + mac);
        if(!isInLiveMode()) {
          // ### stopListeningBLEManagerDisconnectAndCloseGatt();
        }
        registerSuccessAndContinueUpdate(intent);
      }
      else {
        Log.e(TAG, "ACTION " + action + " is not handled.");
      }

      if(bUpdateFailed) {

        if (CTEK.ACTION_DEVICE_UPDATE_FAIL.equals(action)) {

          if (intent.hasExtra("DATA")) {
            String data = intent.getStringExtra("DATA");
            testonUpdateDeviceFailed(action, mac, data);
          }
          else {
            onUpdateDeviceFailed(action, mac);
          }
        }
        else {
          onUpdateDeviceFailed(action, mac);

        }
      }
      return;
    }
  };

  public void resendUpdateStates () {
    Intent broadcast = new Intent(DeviceManagerHiQ.ACTION_UPDATE_DEVICES_STATE);
    int count = (task!=null) ? mDevicesUpdateList.size() : 0;
    broadcast.putExtra(DeviceManagerHiQ.EXTRA_UPDATE_DEVICES_COUNT, count);
    Log.d(TAG, "resendUpdateStates: count = " + count);
    for(int kk=0;kk<count;++kk) {
      DeviceUpdateInfo infoKK = mDevicesUpdateList.getDeviceUpdateInfo(kk);
      Long deviceId = infoKK.device.getId();
      boolean isUpdatedNow;
      if(isInLiveMode()) {
        isUpdatedNow = false;
      }
      else {
        isUpdatedNow = infoKK.isUpdatedNow();
      }
      Log.d(TAG, "resendUpdateStates: " + deviceId + " isUpdatedNow = " + isUpdatedNow);
      broadcast.putExtra(DeviceManager.EXTRA_DEVICE_ID + kk, deviceId);
      broadcast.putExtra(DeviceManagerHiQ.EXTRA_UPDATE_DEVICE_STATE + kk, isUpdatedNow);
    }
    mContext.sendBroadcast(broadcast);
    Log.d(TAG, "resendUpdateStates =======");
    return;
  }

  private void sendBC (String action, Long deviceId) {
    if(deviceId!=null) {
      Intent broadcast = new Intent(action);
      broadcast.putExtra(DeviceManager.EXTRA_DEVICE_ID, deviceId);
      mContext.sendBroadcast(broadcast);
    }
    return;
  }

  private void testsendBC (String action, Long deviceId, String mseg) {
    if(deviceId!=null) {
      Intent broadcast = new Intent(action);
      broadcast.putExtra(DeviceManager.EXTRA_DEVICE_ID, deviceId);
      broadcast.putExtra("DATA", mseg);
      mContext.sendBroadcast(broadcast);
    }
    return;
  }

  private void onUpdateDeviceFailed (String action, String mac) {
    Long deviceId = DeviceRepository.getDeviceIdForAddress(mContext, mac);
    Log.d(TAG, action + ". device = " + mac + " id = " + deviceId);
    sendBC(DeviceManager.ACTION_DEVICE_DISCONNECTED, deviceId); // ACTION_DEVICE_DISCONNECTED stops spinner in UI
    registerResultAndContinueUpdate(mac, false);
    return;
  }

  private void testonUpdateDeviceFailed (String action, String mac, String mseg) {
    Long deviceId = DeviceRepository.getDeviceIdForAddress(mContext, mac);
    Log.d(TAG, action + ". device = " + mac + " id = " + deviceId);
//    sendBC(DeviceManager.ACTION_DEVICE_DISCONNECTED, deviceId); // ACTION_DEVICE_DISCONNECTED stops spinner in UI
    testsendBC(DeviceManager.ACTION_DEVICE_DISCONNECTED, deviceId, mseg);
    registerResultAndContinueUpdate(mac, false);
    return;
  }

  private void registerSuccessAndContinueUpdate (Intent intent) {
    String action = intent.getAction();
    String mac = intent.getStringExtra(CTEK.EXTRA_BLE_DEVICE_ADDRESS);
    Device device = DeviceRepository.getDeviceForAddress(mContext, mac);
    Long deviceId = (device!=null) ? device.getId() : null;
    Log.d(TAG, action + ". device = " + mac + " id = " + deviceId);

    if(device!=null) {
      double[] newVoltages = intent.getDoubleArrayExtra(CTEK.EXTRA_VOLTAGE_ARRAY);
      int cursor = intent.getIntExtra(CTEK.EXTRA_CURSOR, -1);
      boolean gapDetected = intent.getBooleanExtra(CTEK.EXTRA_GAP_IN_DATA_DETECTED, false);
      CTEK.storeData(mContext, TAG, device, newVoltages, cursor, gapDetected);
      sendBC(DeviceManager.ACTION_DEVICE_UPDATED, deviceId);  // ACTION_DEVICE_UPDATED stops spinner in UI
      Log.d(TAG, "Update completed MAC = " + mac);
    }
    registerResultAndContinueUpdate(mac, true);
    return;
  }

  private void registerResultAndContinueUpdate (String mac, boolean bResult) {
    mDevicesUpdateList.registerResult(TAG, mac, bResult);
    stopListeningBLEManagerDisconnectAndCloseGatt();
    if(isInLiveMode()) {
      stopLiveMode();
    }
    continueUpdate();  //Check Originally present
  }

  private void continueUpdate () {
    DeviceUpdateInfo next = mDevicesUpdateList.getNextDeviceUpdateInfo();
    if(next!=null) {
      next.registerStart();
      String mac = next.device.getAddress();
      Long deviceId = next.device.getId(); // DeviceRepository.getDeviceIdForAddress(mContext, mac);
      Log.d(TAG, "Started update of battery data for device " + mac + " id = " + deviceId);
      sendBC(DeviceManager.ACTION_DEVICE_FOUND, deviceId);
      if(isValidBluetoothAddress(mac)) {
        //Original
        startListeningBLEManager();
        bleManager.updateBatteryData(next.device);
      }
      else {
        // IllegalArgumentException: 01:01:01:01 is not a valid Bluetooth address
        sendBC(DeviceManager.ACTION_DEVICE_UPDATED, deviceId);
        registerResultAndContinueUpdate(mac, true);
      }
    }
    else {
      Log.d(TAG, "Update completed. No more devices in the list.");
      mDevicesUpdateList.reportResults(TAG);
      stop();
      updater.scheduleNewUpdate();
      // inform update service (and everybody who is interested)
      Pair<Integer, Integer> summary = mDevicesUpdateList.getUpdateSummary();
      mDevicesUpdateList.clear();
      boolean isEntireUpdateSuccessfull = (summary.first == summary.second);
      Log.d(TAG, "Update result is " + isEntireUpdateSuccessfull);
      Intent broadcast = new Intent(ACTION_UPDATE_COMPLETE);
      broadcast.putExtra(EXTRA_UPDATE_RESULT, isEntireUpdateSuccessfull);
      broadcast.putExtra(EXTRA_UPDATE_DEVICES_COUNT, summary.second);
      broadcast.putExtra(EXTRA_UPDATE_SUCCESS_COUNT, summary.first);
      Log.i(TAG,"Battery update complete");
      broadcast.putExtra(DeviceManagerHiQ.START_LIVE_MODE,"LIVE MODE");
      mContext.sendBroadcast(broadcast);
      //Added
      //Log.i("Update complete","Battery update complete");
//      Intent i1_= new Intent(DeviceManagerHiQ.START_LIVE_MODE);
//      mContext.sendBroadcast(i1_);
      ///
      boolean isEntireUpdateFailed = (summary.first == 0);
      if(isEntireUpdateFailed) {
        Notifications.setNewNotifications(mContext);
      }
    }
    return;
  }

} // EOClass DeviceManagerHiQ
