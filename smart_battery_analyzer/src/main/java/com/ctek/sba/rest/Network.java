package com.ctek.sba.rest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by evgeny.akhundzhanov on 28.08.2017.
 */

public class Network {

  private Network() {}

  public static boolean isOnline (final Context ctx) {
    ConnectivityManager mgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo info = mgr.getActiveNetworkInfo();
    return info!=null && info.isAvailable() && info.isConnected();
  }

} // EOClass Network
