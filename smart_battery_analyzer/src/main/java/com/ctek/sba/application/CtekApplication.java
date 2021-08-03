package com.ctek.sba.application;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;

import com.ctek.sba.bluetooth.DeviceManager;
import com.ctek.sba.bluetooth.DeviceManagerHiQ;
import com.ctek.sba.device.DeviceMap;
import com.ctek.sba.device.DeviceMapIntervals;
import com.ctek.sba.device.DeviceSoCMap;
import com.ctek.sba.rest.SrvPostCapacity;
import com.ctek.sba.services.CTEKUpdateServiceHost;
import com.ctek.sba.ui.support.Ui;
import com.ctek.sba.util.SettingsHelper;

import de.greenrobot.event.EventBus;
import greendao.DaoMaster;
import greendao.DaoSession;


public class CtekApplication extends Application {

  private DaoSession daoSession;

  // application background flag
  private static boolean isInBackground = true;

  public static void setBackgroundState(boolean bkg) {
    isInBackground = bkg;
  }

  public static boolean isInBackgroundState() {
    return isInBackground;
  }


  private static boolean isReadyForInteraction = true; // screen is unlocked

  public static boolean isAvailableForInteraction() {
    return isReadyForInteraction;
  }

  // current number of activities in 'started' state
  private static int mActivityCounter = 0;

  public static void incActivitiesStarted() {
    ++mActivityCounter;
  }

  public static void decActivitiesStarted() {
    --mActivityCounter;
  }

  public static int getActivitiesStarted() {
    return mActivityCounter;
  }

  // number of times app was awaked from background
  private static long mBkgAwakedCounter = 0;

  public static void incAwakedCounter() {
    ++mBkgAwakedCounter;
  }

  public static long getAwakedCounter() {
    return mBkgAwakedCounter;
  }


  private BroadcastReceiver mLockRcv = new BroadcastReceiver() {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();

      if (Intent.ACTION_SCREEN_OFF.equals(action)) {
        // treat as going to background
        CtekApplication.setBackgroundState(true);
        isReadyForInteraction = false;
      } else if (Intent.ACTION_SCREEN_ON.equals(action)) {

      } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
        // handle unlock event here if needed
        isReadyForInteraction = true;
      }
    }
  };


  @Override
  public void onCreate() {
    super.onCreate();

    setupDatabase();

    Context ctxApp = getApplicationContext();
    DeviceMap.initInstance(ctxApp);
    DeviceMapIntervals.initInstance(ctxApp);
    DeviceSoCMap.initInstance(ctxApp);
    Ui.init(ctxApp);
    DeviceManagerHiQ.init(ctxApp);
    DeviceManager.init(ctxApp);
    configureEventBus();


    if (SettingsHelper.getBackgroundUpdate(ctxApp)) {
      CTEKUpdateServiceHost.initInstance(ctxApp); // start CTEK update service
    }

    if (!isInBackgroundState()) {

      IntentFilter filter = new IntentFilter();
      filter.addAction(Intent.ACTION_SCREEN_OFF);
      filter.addAction(Intent.ACTION_SCREEN_ON);
      filter.addAction(Intent.ACTION_USER_PRESENT);
      registerReceiver(mLockRcv, filter);

      SrvPostCapacity.postAllCapacities(ctxApp);
    }
    return;
  }

  @Override
  public void onTerminate() {
    if (mLockRcv != null) {
      unregisterReceiver(mLockRcv);
      mLockRcv = null;
    }
    super.onTerminate();
  }


  private void configureEventBus() {
    EventBus.builder()
        .throwSubscriberException(true)
        .logNoSubscriberMessages(false)
        .sendNoSubscriberEvent(false)
        .installDefaultEventBus();
  }

  private void setupDatabase() {
    recreateDaoSession();
  }

  public DaoSession getDaoSession() {
    return daoSession;
  }

  public void recreateDaoSession() {
    DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "ctek-db", null);
    SQLiteDatabase db = helper.getWritableDatabase();
    DaoMaster daoMaster = new DaoMaster(db);
    daoSession = daoMaster.newSession();
  }

} // EOClass CtekApplication
