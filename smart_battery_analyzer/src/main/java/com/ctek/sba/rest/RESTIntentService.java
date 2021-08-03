package com.ctek.sba.rest;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;

/**
 * Created by evgeny.akhundzhanov on 28.08.2017.
 */

public abstract class RESTIntentService extends IntentService {

  protected Context ctx;
  protected	String		token;
  protected	RESTResult	theResult;

  protected	TTimer		timer;

  protected Handler mHandler = new Handler();


  public RESTIntentService (String name) {
    super(name);
  }

  @Override
  protected void onHandleIntent(Intent i_) {
    ctx = getApplicationContext();
    token = "";
    theResult = new RESTResult();
    timer = new TTimer();
  }

  protected void postDelayed (Runnable R_, long mSecs) {
    if(mHandler!=null) {
      mHandler.removeCallbacks(R_);
      mHandler.postDelayed(R_, mSecs);
    }
  }

} // EOClass RESTIntentService
