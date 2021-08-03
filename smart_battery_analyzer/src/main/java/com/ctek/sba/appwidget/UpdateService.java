package com.ctek.sba.appwidget;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.ctek.sba.R;
import com.ctek.sba.bluetooth.CTEK;
import com.ctek.sba.util.SettingsHelper;

import java.text.DecimalFormat;

public class UpdateService extends IntentService {


  public static final String BATTERY_PERCENTAGE_UPDATE = "BatteryPercentageUpdate";
  public static final String BATTERY_DEVICE_NAME = "BatteryDeviceName";
  public static final String WIDGET_BLUETOOTH_STATUS_ON = "WidgetBluetoothStatusOn";
  public static final String WIDGET_BLUETOOTH_STATUS_OFF = "WidgetBluetoothStatusOff";
  public static final String WIDGET_DEVICE_REMOVED = "WidgetDeviceRemoved";
  public static final String WIDGET_NEW_DEVICE_ADDED = "WidgetNewDeviceAdded";
  public static final String WIDGET_DEVICE_UPDATED = "WidgetDeviceUpdated";


  /**
   * Creates an IntentService.  Invoked by your subclass's constructor.
   */
  public UpdateService() {
    super("UpdateService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {

    if (intent != null) {

      String degree = (SettingsHelper.getCelsius(getApplicationContext()) ? " \u2103" : " \u2109");
//      if (SettingsHelper.getShowVoltage(getApplicationContext())) {
//        degree = "C";
//      } else {
//        degree = "F";
//      }

      final String action = intent.getAction();
      if (WIDGET_BLUETOOTH_STATUS_OFF.equals(action)) {
        RemoteViews remoteViews = UpdateService.createRemoteViewWithMessage(getPackageName(), "Bluetooth OFF", degree);
        ComponentName componentName = new ComponentName(this, BatteryStatusWidget.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(componentName, remoteViews);
      } else if (WIDGET_BLUETOOTH_STATUS_ON.equals(action)) {
//              RemoteViews remoteViews = createBluetoothStatusOFFViews(getPackageName(),true,degree);
        RemoteViews remoteViews = BatteryStatusWidget.createFirstTimeRemoteView(getApplicationContext());
        ComponentName componentName = new ComponentName(this, BatteryStatusWidget.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(componentName, remoteViews);
      } else if (CTEK.EXTRA_BLE_LIVE_VOLTAGE.equals(action)) {

        double currVolt = intent.getExtras().getDouble(CTEK.EXTRA_BLE_LIVE_VOLTAGE);
        double currTemp = intent.getExtras().getDouble(CTEK.EXTRA_BLE_LIVE_TEMPERATURE);
        boolean bCelsius = SettingsHelper.getCelsius(getApplicationContext());
        if (!bCelsius){
          currTemp = SettingsHelper.convertCelcius2Fahrenheit((float)currTemp);
        }
        long batteryPercent = intent.getExtras().getLong(UpdateService.BATTERY_PERCENTAGE_UPDATE);
        String deviceName = intent.getExtras().getString(UpdateService.BATTERY_DEVICE_NAME);

        DecimalFormat df = new DecimalFormat("#.00");
        String strVoltage = df.format(currVolt);
        String strTemperature = df.format(currTemp);
        String strPercent = String.valueOf(batteryPercent);

        RemoteViews remoteViews = UpdateService.createRemoteViews(getPackageName(), deviceName, strVoltage, strTemperature, strPercent, degree);
        ComponentName componentName = new ComponentName(this, BatteryStatusWidget.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(componentName, remoteViews);
      } else if (WIDGET_DEVICE_REMOVED.equals(action)) {
        RemoteViews remoteViews = BatteryStatusWidget.createFirstTimeRemoteView(getApplicationContext());
        ComponentName componentName = new ComponentName(this, BatteryStatusWidget.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(componentName, remoteViews);
      } else if (WIDGET_NEW_DEVICE_ADDED.equals(action)) {
        RemoteViews remoteViews = BatteryStatusWidget.createFirstTimeRemoteView(getApplicationContext());
        ComponentName componentName = new ComponentName(this, BatteryStatusWidget.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(componentName, remoteViews);
      } else if (WIDGET_DEVICE_UPDATED.equals(action)) {
        RemoteViews remoteViews = BatteryStatusWidget.createFirstTimeRemoteView(getApplicationContext());
        ComponentName componentName = new ComponentName(this, BatteryStatusWidget.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(componentName, remoteViews);
      }
    }
  }

  public static RemoteViews createRemoteViews(String strPackage, String deviceName, String voltage, String temperature, String percentage, String tempDegree) {
    RemoteViews remoteViews = new RemoteViews(strPackage, R.layout.battery_status_widget);
    remoteViews.setTextViewText(R.id.txtVwBatteryPercentage, percentage + " %");
    remoteViews.setTextViewText(R.id.txtVwTemperature, temperature + " " + tempDegree);
    remoteViews.setTextViewText(R.id.txtVwVoltage, voltage + "  V");
    remoteViews.setTextViewText(R.id.txt_device_name, deviceName);
    remoteViews.setViewVisibility(R.id.battery_status_layout, View.VISIBLE);

    int level = Integer.parseInt(percentage);
    //Update Battery Image
    remoteViews.setImageViewResource(R.id.battery_view, R.drawable.widgetbattery);
    remoteViews.setViewVisibility(R.id.percent100, (level <= 100 && level > 90) ?
        View.VISIBLE : View.INVISIBLE);
    remoteViews.setViewVisibility(R.id.percent90, (level <= 90 && level > 80) ?
        View.VISIBLE : View.INVISIBLE);
    remoteViews.setViewVisibility(R.id.percent80, (level <= 80 && level > 70) ?
        View.VISIBLE : View.INVISIBLE);
    remoteViews.setViewVisibility(R.id.percent70, (level <= 70 && level > 60) ?
        View.VISIBLE : View.INVISIBLE);
    remoteViews.setViewVisibility(R.id.percent60, (level <= 60 && level > 50) ?
        View.VISIBLE : View.INVISIBLE);
    remoteViews.setViewVisibility(R.id.percent50, (level <= 50 && level > 40) ?
        View.VISIBLE : View.INVISIBLE);
    remoteViews.setViewVisibility(R.id.percent40, (level <= 40 && level > 30) ?
        View.VISIBLE : View.INVISIBLE);
    remoteViews.setViewVisibility(R.id.percent30, (level <= 30 && level > 20) ?
        View.VISIBLE : View.INVISIBLE);
    remoteViews.setViewVisibility(R.id.percent20, (level <= 20 && level > 10) ?
        View.VISIBLE : View.INVISIBLE);
    remoteViews.setViewVisibility(R.id.percent10, (level <= 10 && level > 0) ?
        View.VISIBLE : View.INVISIBLE);
//      remoteViews.setViewVisibility(R.id.charge_view, isCharging ?
//          View.VISIBLE : View.INVISIBLE);

    remoteViews.setViewVisibility(R.id.charge_view, View.INVISIBLE);
    // remoteViews.setTextViewText(R.id.batterytext, String.valueOf(level) + "%");
    return remoteViews;
  }

//    public static RemoteViews createBluetoothStatusOFFViews (String strPackage, Boolean isBluetoothOn, String tempDegree) {
//      RemoteViews remoteViews = new RemoteViews(strPackage, R.layout.battery_status_widget);
//      remoteViews.setTextViewText(R.id.txtVwBatteryPercentage, "- %");
//      remoteViews.setTextViewText(R.id.txtVwTemperature, "- \u00B0"+tempDegree);
//      remoteViews.setTextViewText(R.id.txtVwVoltage, "-  V");
//
//      remoteViews.setViewVisibility(R.id.battery_status_layout, View.GONE);
//
//      if (isBluetoothOn == true) {
//        remoteViews.setTextViewText(R.id.txt_device_name, "Looking for device");
//      }
//      else {
//        remoteViews.setTextViewText(R.id.txt_device_name, "Bluetooth OFF");
//      }
//      return remoteViews;
//    }

  public static RemoteViews createRemoteViewWithMessage(String strPackage, String message, String tempDegree) {
    RemoteViews remoteViews = new RemoteViews(strPackage, R.layout.battery_status_widget);
    remoteViews.setTextViewText(R.id.txtVwBatteryPercentage, "- %");
    remoteViews.setTextViewText(R.id.txtVwTemperature, "- " + tempDegree);
    remoteViews.setTextViewText(R.id.txtVwVoltage, "-  V");
    remoteViews.setViewVisibility(R.id.battery_status_layout, View.GONE);
    remoteViews.setTextViewText(R.id.txt_device_name, message);
    return remoteViews;
  }
}
