package com.ctek.sba.services;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by evgeny.akhundzhanov on 27.10.2016.
 *
 * The "singleton" that holds the connection to CTEKUpdateService.
 * Should be initialized on start/login and "closed" on finish/logout.
 *
 * initInstance() starts and binds the connection to CTEKUpdateService.
 * unbindServiceAndSetInstance2Null() unbinds the connection and set instance to null.
 *
 * 1. start
 * CTEKUpdateServiceHost.initInstance(ctx);
 *
 * 2. finish
 * CTEKUpdateServiceHost.getInstance().unbindServiceAndSetInstance2Null();
 *
 */
public class CTEKUpdateServiceHost {


  public final String TAG	= getClass().getSimpleName();

  private static CTEKUpdateServiceHost instance = null;

  public static CTEKUpdateServiceHost initInstance (Context ctx) {
    if(instance==null) {
      instance = new CTEKUpdateServiceHost(ctx);
    }
    return instance;
  }

  public static CTEKUpdateServiceHost getInstance () {
    return instance;
  }



  private Context				    ctx;
  private CTEKUpdateService mBoundService;

  private CTEKUpdateServiceHost (Context ctx) {
    this.ctx = ctx;
    mBoundService = null;
    bindService(true);
  }

  private ServiceConnection mConnection = new ServiceConnection() {

    public void onServiceConnected(ComponentName className, IBinder binder) {
      Log.d(TAG, "onServiceConnected");
      mBoundService = ((CTEKUpdateService.UpdateServiceBinder)binder).getService();
    }

    public void onServiceDisconnected(ComponentName className) {
      Log.d(TAG, "onServiceDisconnected");
      mBoundService = null;
    }
  };

  public boolean isConnected () { return mBoundService!=null; }

  public void unbindServiceAndSetInstance2Null () {
    bindService(false);
    instance = null; // force create service and bind on next call to initInstance().
  }

  private void bindService (boolean bStart) {
    if(bStart) {
      if(!isConnected()) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter!=null) {
          boolean bond = ctx.bindService(new Intent(this.ctx, CTEKUpdateService.class), mConnection, Context.BIND_AUTO_CREATE | Context.BIND_NOT_FOREGROUND);
          Log.d(TAG, "bindService returns = " + bond);
        }
        else {
          Log.e(TAG, "This device does not support Bluetooth.");
        }
      }
    }
    else {
      if(isConnected()) {
        try {
          ctx.unbindService(mConnection);
        }
        catch(Exception xpt) {}
        Log.d(TAG, "unbindService");
      }
      mBoundService = null;
      // instance = null; // force create service and bind on next call to initInstance().
    }
    return;
  }

  /*
  public void doConnect (boolean bConnect) {
    if(mBoundService!=null) {
      if(bConnect) {
        mBoundService.doConnect();
      }
      else {
        mBoundService.doDisconnect();
      }
    }
    return;
  }
  */

} // EOClass CTEKUpdateServiceHost
