package com.ctek.sba.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Config;
import android.util.Log;

import com.ctek.sba.R;
import com.ctek.sba.bluetooth.DeviceManager;
import com.ctek.sba.bluetooth.DeviceManagerHiQ;
import com.ctek.sba.util.SettingsHelper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import static android.view.View.Z;

/**
 * Created by evgeny.akhundzhanov on 27.10.2016.
 * CBS-32 Android - device update should be an Android service.
 */
public class CTEKUpdateService extends Service {

  public final String TAG	= getClass().getSimpleName();

  private     PowerManager.WakeLock	wakeLock;
  private     WifiManager.WifiLock  wifilock;
  private     UpdateServiceBinder   serviceBinder;
  private     RcvServiceEvents      rcvServiceEvents;

  private final static long RECONNECT_INTERVAL_05_MIN_MSECS =  5*60*1000; // for test
  private final static long RECONNECT_INTERVAL_60_MIN_MSECS = 60*60*1000; // after successfull update
  private final static long RECONNECT_INTERVAL_03_MIN_MSECS =  3*60*1000; // after failed      update
  private final static long RECONNECT_INTERVAL_05_SEC_MSECS =     5*1000; // after service started AND after Bluetooth is switched ON
  private final  static long RECONNECT_INTERVAL_02_MIN_MSECS = 2*60*1000; //Test


  // CTEK wonders if we can do every 3 minutes with the exception of 60 minutes after successful connection?
  private final static long getReconnectIntervalMsecs (boolean lastUpdateResult) {
    return lastUpdateResult ? RECONNECT_INTERVAL_60_MIN_MSECS : RECONNECT_INTERVAL_03_MIN_MSECS;  //Changed true update interval to 5 Min
  }

  private Handler   mUpdateHandler = new Handler();
  private Runnable  runCheckUpdates = new Runnable() { public void run() { checkUpdates(); } };

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "on create RECONNECT_INTERVALs (T/F [sec]) = " + getReconnectIntervalMsecs(true) / 1000 + " / " + getReconnectIntervalMsecs(false) / 1000);

    serviceBinder = new UpdateServiceBinder(this);

    PowerManager pm = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    wakeLock.acquire();

    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    wifilock = wifiManager.createWifiLock(TAG + ":lockWiFi");
    if(wifilock!=null) {
      wifilock.acquire();
    }

    IntentFilter if_ = new IntentFilter();
    if_.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    if_.addAction(DeviceManagerHiQ.ACTION_UPDATE_COMPLETE);
    // if_.addAction(SettingsHelper.ACTION_SETTING_BG_UPDATE_CHANGED);
    rcvServiceEvents = new RcvServiceEvents();
    registerReceiver(rcvServiceEvents, if_);

    String mess = "Service created.";
    Log.d(TAG, mess);
    postDelayedCheckUpdates(RECONNECT_INTERVAL_05_SEC_MSECS, mess);
    return;
  }

  @Override
  public void onDestroy() {
    String mess = "Service destroyed.";
    Log.d(TAG, mess);
    if(rcvServiceEvents!=null) { unregisterReceiver(rcvServiceEvents); rcvServiceEvents = null; }
    Notifier.cancelNotification(this);

    cleanupCheckUpdatesTasks(mess);
    stopForeground(true);

    if(wifilock!=null && wifilock.isHeld()) {
      wifilock.release();
      wifilock = null;
    }

    if(wakeLock!=null && wakeLock.isHeld()) {
      wakeLock.release();
      wakeLock = null;
    }

    super.onDestroy();
    return;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return Service.START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return serviceBinder;
  }

  class UpdateServiceBinder extends Binder {

    private final CTEKUpdateService service;

    UpdateServiceBinder(CTEKUpdateService service) {
      this.service = service;
    }
    public CTEKUpdateService getService() {
      return service;
    }

  } // EOClass UpdateServiceBinder

  class RcvServiceEvents extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent i_) {
      String action = i_.getAction();
      if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
        int state = i_.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
        if(state==BluetoothAdapter.STATE_ON) {
          String mess = "Bluetooth ON.";
          Log.d(TAG, mess);
          postDelayedCheckUpdates(RECONNECT_INTERVAL_05_SEC_MSECS, mess);
        }
        else if(state==BluetoothAdapter.STATE_OFF) {
          String mess = "Bluetooth OFF.";
          Log.d(TAG, mess);
          postDelayedCheckUpdates(RECONNECT_INTERVAL_60_MIN_MSECS, mess); // Clear updates.
          // cleanupCheckUpdatesTasks(mess); // invalid message here - wait for strings
        }
      }
      else if(DeviceManagerHiQ.ACTION_UPDATE_COMPLETE.equals(action)) {
//        //Added
//        Log.i("Update complete","Battery update complete");
//        Intent i1_= new Intent(DeviceManagerHiQ.START_LIVE_MODE);
//        ctx.sendBroadcast(i1_);
        ///
        boolean updateResult = i_.getExtras().getBoolean(DeviceManagerHiQ.EXTRA_UPDATE_RESULT);
        int numberOfDevices = i_.getExtras().getInt(DeviceManagerHiQ.EXTRA_UPDATE_DEVICES_COUNT);
        int numberOfSuccess = i_.getExtras().getInt(DeviceManagerHiQ.EXTRA_UPDATE_SUCCESS_COUNT);
        String mess = "Update result is " + updateResult;
        if(numberOfDevices!=0) {
          mess += ". " + numberOfSuccess + " of " + numberOfDevices + " were updated.";
        }
        Log.d(TAG, mess);
        postDelayedCheckUpdates(getReconnectIntervalMsecs(updateResult), mess);
        Log.d(TAG,"Start live mode check");

        //Added
        if (!checkNetworkConnectivity()) {
          Intent iLive_ = new Intent(DeviceManagerHiQ.START_LIVE_MODE);
          ctx.sendBroadcast(iLive_);
        }
      }

      ///
      /*
      else if(SettingsHelper.ACTION_SETTING_BG_UPDATE_CHANGED.equals(action)) {
        // not used // boolean isEnabled = i_.getExtras().getBoolean(SettingsHelper.EXTRA_SETTING_BG_UPDATE);
        postDelayedCheckUpdates(0);
      }
      */
      return;
    }


    private boolean checkNetworkConnectivity(){
      boolean networkCheck = false;
      ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
      if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
          connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
        //we are connected to a network
        networkCheck = true;
        if (networkCheck) {
          try {
            Process p1 = java.lang.Runtime.getRuntime().exec("ping -c 1 www.google.com");
            int returnVal = p1.waitFor();
             networkCheck = (returnVal==0);
            return networkCheck;
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      else
        networkCheck = false;
      return networkCheck;
    }

  }

  ; // EOClass RcvServiceEvents

 // private void updateServiceNotification (long msecsNextUpdate) {
//    String titl = getString(R.string.app_name);
//    String text = getString(R.string.update_service_is_running);
//    // CBS-69 Android: translations for background service
//    // Only one string needed in notification area. String is: "Update service is running".
//    String subt = ""; // getString(R.string.update_service_next_update) + ": " + getMillisFormattedAsHHMMSS(msecsNextUpdate);
//    Notifier.notifyServiceRunning(this, titl, text, subt);
//    return;
//  }

  private void postDelayedCheckUpdates (long mSecs, String reason) {
    mUpdateHandler.removeCallbacks(runCheckUpdates);
    long msecsNextUpdate = System.currentTimeMillis() + mSecs;
    mUpdateHandler.postDelayed(runCheckUpdates, mSecs);
  //  updateServiceNotification(msecsNextUpdate);
    String nextUpdate = getString(R.string.update_service_next_update_is_scheduled_to) + ": " + getMillisFormattedAsHHMMSS(msecsNextUpdate);
    Log.d(TAG, nextUpdate + " - " + reason);

    return;
  }

  public static String getMillisFormattedAsHHMMSS (long msecs) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(msecs);
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
    return sdf.format(cal.getTime());
  }

  private void cleanupCheckUpdatesTasks (String reason) {
    if(mUpdateHandler!=null) {
      mUpdateHandler.removeCallbacksAndMessages(null);
      Log.d(TAG, "Pending update tasks removed: " + reason);
    }
  }

  private void checkDeviceManagers () {
    if(DeviceManager.getInstance()==null) {
      DeviceManager.init(getApplicationContext());
    }

    if(DeviceManagerHiQ.getInstance()==null) {
      DeviceManagerHiQ.init(getApplicationContext());
    }
    return;
  }

  private void doCheckUpdates () {
    checkDeviceManagers();

    if(DeviceManagerHiQ.getInstance().isInLiveMode()) {
      Log.d(TAG, "Auto update skipped. LIVE MODE is ON.");
    }
    else
      if(DeviceManager.getInstance().getTaskRunning()!=null) {
      Log.d(TAG, "Auto update skipped. User task (pair or search) is running " + DeviceManager.getInstance().getTaskRunning().name());
    }
    else {
      Log.d(TAG, "Auto update started.");
  //    DeviceManagerHiQ.getInstance().updateAllDevices("LIVE MODE");  //Originally present
    }
    return;
  }

  private void checkUpdates () {
    String mess;
    // check whether 'Background updates' setting is enabled.
    boolean bgUpdateIsEnabled = SettingsHelper.getBackgroundUpdate(getApplicationContext());
    if(bgUpdateIsEnabled) {
      BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
      // check whether Bluetooth is switched ON.
      if(adapter.isEnabled()) {
        mess = getString(R.string.update_service_starts);
        doCheckUpdates();
      }
      else {
        mess = getString(R.string.update_service_bluetooth_off);
        DeviceManagerHiQ.getInstance().cleanTaskIfAny();
      }
    }
    else {
      // This should not be the case as we stop service when setting is off.
      mess = getString(R.string.update_service_setting_off);
      DeviceManagerHiQ.getInstance().cleanTaskIfAny();
    }
    Log.d(TAG, mess);
    postDelayedCheckUpdates(RECONNECT_INTERVAL_03_MIN_MSECS, "checkUpdates. This could be changed based on update result.");
    //Log.i("Update result","Battery update process finished");
    return;
  }

} // EOClass CTEKUpdateService
