package com.ctek.sba.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.ctek.sba.R;
import com.ctek.sba.services.CTEKUpdateServiceHost;

import java.util.Locale;

/**
 * Created by elab on 18/02/15.
 */
public class SettingsHelper {

  private static final String CTEK_PREFS = "ctek_prefs";

  public static final int MODE_PAGER = 1;
  public static final int MODE_LIST = 2;

  /*
  public final static String                                    PACKAGE = "com.ctek.sba.util";
  public final static String ACTION_SETTING_BG_UPDATE_CHANGED = PACKAGE + ".SETTING_BG_UPDATE_CHANGED";
  public static final String EXTRA_SETTING_BG_UPDATE          = PACKAGE + ".SETTING_BG_UPDATE_VALUE";

  static void broadcastSettingChangedBGUpdate (Context ctx, boolean value) {
    final Intent intent = new Intent(ACTION_SETTING_BG_UPDATE_CHANGED);
    intent.putExtra(EXTRA_SETTING_BG_UPDATE, value);
    ctx.sendBroadcast(intent);
  }
  */

  public static boolean getNotificationData(Context context) {
    return getPrefs(context).getBoolean(context.getString(R.string.pref_key_notif_data), true);
  }

  public static boolean getNotificationStatus(Context context) {
    return getPrefs(context).getBoolean(context.getString(R.string.pref_key_notif_status), true);
  }

  public static void setNotificationData(Context context, boolean b) {
    getPrefs(context).edit().putBoolean(context.getString(R.string.pref_key_notif_data), b).commit();
  }

  public static boolean getBackgroundUpdate(Context context) {
    return getPrefs(context).getBoolean(context.getString(R.string.pref_key_background_update), true);
  }

  public static void setBackgroundUpdate(Context ctxApp, boolean value) { // We need Application context here!
    getPrefs(ctxApp).edit().putBoolean(ctxApp.getString(R.string.pref_key_background_update), value).commit();
    if(value) {
      CTEKUpdateServiceHost.initInstance(ctxApp);
    }
    else {
      CTEKUpdateServiceHost.getInstance().unbindServiceAndSetInstance2Null();
    }
    // broadcastSettingChangedBGUpdate(ctxApp, value);
    return;
  }

  public static void setNotificationStatus(Context context, boolean b) {
    getPrefs(context).edit().putBoolean(context.getString(R.string.pref_key_notif_status), b).commit();
  }

  public static SharedPreferences getPrefs(Context context) {
    return context.getSharedPreferences(CTEK_PREFS, Context.MODE_PRIVATE);
  }

  public static int getDeviceListMode(Context context) {
    return getPrefs(context).getInt(context.getString(R.string.pref_key_device_list_mode), MODE_PAGER);
  }

  public static void setDeviceListModeList(Context context) {
    getPrefs(context).edit().putInt(context.getString(R.string.pref_key_device_list_mode), MODE_LIST).commit();
  }

  public static void setDeviceListModePager(Context context) {
    getPrefs(context).edit().putInt(context.getString(R.string.pref_key_device_list_mode), MODE_PAGER).commit();
  }

  public static String getDeviceListModeKey(Context context) {
    return context.getString(R.string.pref_key_device_list_mode);
  }


  public static final boolean DFLT_SHOW_VOLTAGE = false;

  public static boolean getShowVoltage(Context context) {
    return getPrefs(context).getBoolean(context.getString(R.string.pref_key_show_voltage_mode), DFLT_SHOW_VOLTAGE);
  }

  public static void setShowVoltage(Context context, boolean value) {
    getPrefs(context).edit().putBoolean(context.getString(R.string.pref_key_show_voltage_mode), value).commit();
  }

  public static final boolean DFLT_T_IN_CELSIUS = true;

  public static boolean getCelsius(Context context) {
    return getPrefs(context).getBoolean(context.getString(R.string.pref_key_celsius_degrees), DFLT_T_IN_CELSIUS);
  }

  public static void setCelsius(Context context, boolean value) {
    getPrefs(context).edit().putBoolean(context.getString(R.string.pref_key_celsius_degrees), value).commit();
  }

  // [°F] = [°C] × 9⁄5 + 32
  public static float convertCelcius2Fahrenheit (float tC) {
    final float tZeroF = 32.f;
    if(tC==Float.NaN) tC = tZeroF;
    float tF = (tC)*9.f/5.f + tZeroF;
    return tF;
  }

  /*
  public static final boolean DFLT_NEW_CHART = false;

  public static boolean getNewChart(Context context) {
    return getPrefs(context).getBoolean(context.getString(R.string.pref_key_new_chart_mode), DFLT_NEW_CHART);
  }

  public static void setNewChart(Context context, boolean value) {
    getPrefs(context).edit().putBoolean(context.getString(R.string.pref_key_new_chart_mode), value).commit();
  }
  */

  // CBS-174 Android - Add opt-in screen for data collection.
  public static final int DATA_COLLECTION_NOT_SET = -1;  // not set initially, requires user Yes or No answer on first start.

  public static void setDataCollection(Context context, boolean enabled) {
    getPrefs(context).edit().putInt(context.getString(R.string.pref_key_data_collection), enabled ? 0 : 0).commit();
  }

  private static int getDataCollection(Context context) {
    return getPrefs(context).getInt(context.getString(R.string.pref_key_data_collection), DATA_COLLECTION_NOT_SET);
  }

  public static boolean isDataCollectionSet (Context context) {
    return getDataCollection(context) != DATA_COLLECTION_NOT_SET;
  }

  // This is for test only.
  public static void setDataCollectionNotSet(Context context) {
    getPrefs(context).edit().putInt(context.getString(R.string.pref_key_data_collection), DATA_COLLECTION_NOT_SET).commit();
  }

  public static boolean isDataCollectionEnabled (Context context) {
    return getDataCollection(context) == 1;
  }


  public static final long DFLT_TEMPERATURE_STARTED_MSECS = 0;

  public static long getTemperatureStartedTimestamp (Context context) {
    long msecs = getPrefs(context).getLong(context.getString(R.string.pref_key_temperature_started), DFLT_TEMPERATURE_STARTED_MSECS);
    if(msecs==0) {
      msecs = System.currentTimeMillis();
      setTemperatureStartedTimestamp(context, msecs);
    }
    return msecs;
  }

  private static void setTemperatureStartedTimestamp(Context context, long msecs) {
    getPrefs(context).edit().putLong(context.getString(R.string.pref_key_temperature_started), msecs).commit();
  }


  public  static final String CS_HTTP = "http://";
  public  static final String CS_HTTPS= "https://";

  public static void openExternalURL (Context ctx, String url) {
    openExternalLink(ctx, url, "text/html");
  }

  public static void openExternalPDF (Context ctx, String url) {
    openExternalLink(ctx, url, "application/pdf");
  }

  public static void openExternalLink (Context ctx, String url, String mimeType) {
    try {
      if(!url.startsWith(CS_HTTP) && !url.startsWith(CS_HTTPS)) {
        url = CS_HTTP + url;
      }

      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
      // intent.setType(mimeType);  // EA 17-Apr-2018. Failed to open with this flag.
      ctx.startActivity(intent);
      /*
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
      intent.setType(mimeType);
      Intent chooser = Intent.createChooser(intent, "Select application to open PDF");
      chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // optional
      ctx.startActivity(chooser);
      */
    }
    catch(ActivityNotFoundException ignore) {}
    return;
  }

} // EOClass SettingsHelper
