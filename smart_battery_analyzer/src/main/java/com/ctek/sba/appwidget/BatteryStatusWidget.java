package com.ctek.sba.appwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import com.ctek.sba.R;
import com.ctek.sba.device.DeviceMap;
import com.ctek.sba.device.DeviceSoC;
import com.ctek.sba.device.DeviceSoCMap;
import com.ctek.sba.io.dao.DeviceRepository;
import com.ctek.sba.soc.SoCData;
import com.ctek.sba.soc.SoCUtils;
import com.ctek.sba.util.SettingsHelper;

import java.text.DecimalFormat;
import java.util.List;

import greendao.Device;
import greendao.Voltage;

import static java.lang.Math.round;

/**
 * Implementation of App Widget functionality.
 */
public class BatteryStatusWidget extends AppWidgetProvider {

  private final static String TAG = "BatteryStatusWidget";

  public static int getNumberOfWidgets(final Context context) {
    ComponentName componentName = new ComponentName(context, BatteryStatusWidget.class);
    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    int[] activeWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
    if (activeWidgetIds != null) {
      return activeWidgetIds.length;
    } else {
      return 0;
    }
  }

  static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                              int appWidgetId) {

    // Construct the RemoteViews object
   // RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.battery_status_widget);
    RemoteViews views = BatteryStatusWidget.createFirstTimeRemoteView(context);

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views);
  }

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    super.onUpdate(context, appWidgetManager, appWidgetIds);

    // There may be multiple widgets active, so update all of them
    for (int appWidgetId : appWidgetIds) {
      updateAppWidget(context, appWidgetManager, appWidgetId);
    }
  }

  @Override
  public void onEnabled(Context context) {
    super.onEnabled(context);
    // Enter relevant functionality for when the first widget is created

    //#1
    //CHECK THE BLUETOOTH
    //IF NOT CONNECTED THEN SHOW BLUETOOTH OFF MESSAGE

    //ELSE
    //CHECK ANY PREVIOUS IN DB
    //IF NO DEVICE THEN SHOW NO DEVICE CONNECTED

    //ELSE
    //FETCH THE DEVICE DETAIL FROM DB AND SHOW THE DETAILS

    //ELSE SHOW NOT ABLE TO FETCH THE DETAILS
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//      context.startForegroundService(new Intent(context, MonitorService.class));
//    } else {
//      context.startService(new Intent(context, MonitorService.class));
//    }
    context.startService(new Intent(context, MonitorService.class));

    RemoteViews remoteViews = BatteryStatusWidget.createFirstTimeRemoteView(context);
    ComponentName componentName = new ComponentName(context, BatteryStatusWidget.class);
    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    appWidgetManager.updateAppWidget(componentName, remoteViews);
  }

  @Override
  public void onDisabled(Context context) {
    // Enter relevant functionality for when the last widget is disabled
  }

  @Override
  public void onDeleted(Context context, int[] widgetIds) {
    super.onDeleted(context, widgetIds);
    if (getNumberOfWidgets(context) == 0) {
      //stop monitoring if there are no more widgets on screen
      context.stopService(new Intent(context, MonitorService.class));

    }
  }


  public static RemoteViews createFirstTimeRemoteView(Context context) {

    BluetoothAdapter bluetoothadapter = BluetoothAdapter.getDefaultAdapter();
    RemoteViews remoteViews;
    List<Device> deviceList = DeviceRepository.getAllDevices(context);

    String degree = (SettingsHelper.getCelsius(context) ? " \u2103" : " \u2109");
//    if (SettingsHelper.getShowVoltage(context)) {
//      degree = "C";
//    }
//    else {
//      degree = "F";
//    }

    if (deviceList.size() == 0) {
      remoteViews = UpdateService.createRemoteViewWithMessage(context.getPackageName(), "No device is connected", degree);
    }
    else if (!bluetoothadapter.isEnabled()) {
      remoteViews = UpdateService.createRemoteViewWithMessage(context.getPackageName(), "Bluetooth OFF", degree);
    }
    else {
      //RemoteViews remoteViews;
      if (deviceList.size() > 0) {

        Device device1 = deviceList.get(0);
        Device device = DeviceRepository.getDeviceForAddress(context, device1.getAddress());

        SoCData soc = SoCUtils.getLatestSocValue(device);
        long percent = 0;
        if (soc != null) {
          percent = round(SoCUtils.getPercentFromSoc(soc));
        }

        //final double percent = SoCUtils.getPercentFromSoc(soc);

        DecimalFormat df = new DecimalFormat("#.00");
        DecimalFormat df1 = new DecimalFormat("#");

        String strVoltage = "N/A";
        String strTemperature = "N/A";
        String strPercent = "N/A";
        if (device.getVoltageList("voltagelist").size() > 0) {
          Voltage voltage = device.getVoltageList("voltagelist").get(0);
          strVoltage = df.format(voltage.getValue());


          double temperatureC = voltage.getTemperature();
          boolean bCelsius = SettingsHelper.getCelsius(context);
          if (!bCelsius){
            temperatureC = SettingsHelper.convertCelcius2Fahrenheit((float)temperatureC);
          }

          strTemperature = df.format(temperatureC);


        }
        //strTemperature = df.format(temperatureC);
        strPercent = df1.format(percent);//"15";//String.valueOf(percent);
        remoteViews = UpdateService.createRemoteViews(context.getPackageName(), device.getName(),strVoltage,strTemperature,strPercent, degree);
      }
      else {
        remoteViews = UpdateService.createRemoteViewWithMessage(context.getPackageName(), "Looking for device", degree);
      }

    }

    return remoteViews;


  }

}

