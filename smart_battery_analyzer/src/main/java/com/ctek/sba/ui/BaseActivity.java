package com.ctek.sba.ui;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.ctek.sba.application.CtekApplication;
import com.ctek.sba.bluetooth.BluetoothLeManager;
import com.ctek.sba.rest.SrvPostSocs;

/**
 * Created by evgeny.akhundzhanov on 28.08.2017.
 */

public class BaseActivity extends AppCompatActivity {

  private final static String TAG = "BaseActivity";


  @Override
  protected void onStart() {
    super.onStart();

    //Added
   // BluetoothLeManager.getInstance(this).startBTSearch();
    ///
    CtekApplication.incActivitiesStarted();
    if (CtekApplication.getActivitiesStarted() > 1) {
      // previous activity has not been stopped yet
      if (CtekApplication.isInBackgroundState()) {
        // reset background state to FALSE, it's a collision here
        CtekApplication.setBackgroundState(false);
      }

      boolean fromBG = CtekApplication.isInBackgroundState();
      if(fromBG) {
        CtekApplication.setBackgroundState(false);
        CtekApplication.incAwakedCounter();
        Log.d(TAG, getClass().getSimpleName() + " onStart: " +
            "Awaked counter: " + CtekApplication.getAwakedCounter() +
            "Activities counter: " + CtekApplication.getActivitiesStarted()
        );

        onComeFromBackground();
      }
      else {
        Log.d(TAG, getClass().getSimpleName() + " onStart. In foreground.");
      }
    }

    return;
  }

  @Override
  protected void onResume() {
    super.onResume();
    boolean fromBG = CtekApplication.isInBackgroundState();
    if(fromBG) {
      CtekApplication.setBackgroundState(false);
      CtekApplication.incAwakedCounter();
      Log.d(TAG, getClass().getSimpleName() + " onResume: " +
          "Awaked counter: " + CtekApplication.getAwakedCounter() +
          "Activities counter: " + CtekApplication.getActivitiesStarted()
      );

      onComeFromBackground();
    }
    else {
      Log.d(TAG, getClass().getSimpleName() + " onResume: In foreground.");
    }
    return;
  }

  @Override
  protected void onUserLeaveHint () {
    Log.d(TAG, getClass().getSimpleName() + " onUserLeaveHint");
		 // User leaves the activity; most commonly app is sent to background then
		 // CAUTION: despite what the documentation says, it is also called
		 // for an activity A when it starts another activity B.
    CtekApplication.setBackgroundState(true);
    super.onUserLeaveHint();
    return;
  }

  @Override
  protected void onStop() {
    CtekApplication.decActivitiesStarted();
    super.onStop();
    return;
  }

  private void onComeFromBackground () {
    // String reason = getClass().getSimpleName() + " comes from background.";
    // SrvPostSocs.start(getApplicationContext(), reason, SrvPostSocs.MODE_SEND_LAST_SOC);
    //
    // EA 03-Apr-2018.
    // 1. MODE_SEND_LAST_SOC is obsolete and is not used now.
    // 2. MODE_HISTORY_DATA mode is activated from DeviceRepository.insertOrUpdateWithVoltages().
    // That's all with CBS-172 Android - Push battery data to back-end.
    return;
  }

} // EOClass BaseActivity
