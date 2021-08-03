package com.ctek.sba.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ctek.sba.util.SettingsHelper;

/**
 * Created by evgeny.akhundzhanov on 27.10.2016.
 *
 * Starts CTEKUpdateService.
 */
public class RcvDeviceBoot extends BroadcastReceiver {

  public final String TAG	= getClass().getSimpleName();

  @Override
  public void onReceive(Context ctx, Intent i_) {
    String action = i_.getAction();
    if(Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
      Log.d(TAG, "Action " + action + " received.");
      // start update service
      try {
        Context ctxApp = ctx.getApplicationContext();
        if(SettingsHelper.getBackgroundUpdate(ctxApp)) {
          CTEKUpdateServiceHost.initInstance(ctxApp); // Seems like this works. But not emmediately after reboot. Wait 30 seconds.
        }
        else {
          Log.d(TAG, "Update Service not started, as 'Background updates' setting is switched OFF.");
        }
      }
      catch(Exception xpt) {
        Log.e(TAG, "Start update service failed - " + xpt.getMessage());
      }
    }
    return;
  }

} // EOClass RcvDeviceBoot
