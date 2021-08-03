/*
 *  Copyright 2015 Erkan Molla
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.ctek.sba.appwidget;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.ctek.sba.bluetooth.BluetoothLeManager;
import com.ctek.sba.bluetooth.CTEK;
import com.ctek.sba.bluetooth.DeviceManager;
import com.ctek.sba.io.dao.DeviceRepository;
import com.ctek.sba.soc.SoCData;
import com.ctek.sba.soc.SoCUtils;

import greendao.Device;

import static java.lang.Math.round;

/**
 * This service is used to monitor the battery information.
 */
public class MonitorService extends Service {

  private double voltage;
  private double temperature;
  private double percentage;

  final private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {

      //Bluetooth action change
      //New device connected
      //Device details updates
      //Device removed

      if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

        Intent updateIntent = new Intent(context, UpdateService.class);
        if (state == BluetoothAdapter.STATE_ON) {
          updateIntent.setAction(UpdateService.WIDGET_BLUETOOTH_STATUS_ON);
        } else {
          updateIntent.setAction(UpdateService.WIDGET_BLUETOOTH_STATUS_OFF);
        }
        context.startService(updateIntent);

      } else if (CTEK.ACTION_LIVE_MODE.equals(intent.getAction())) {
        //Added
        //BluetoothLeManager.getInstance(context).startBTSearch();
        ///
        String address = intent.getStringExtra(CTEK.EXTRA_BLE_DEVICE_ADDRESS);
        Device device = DeviceRepository.getDeviceForAddress(context, address);
        final SoCData soc = SoCUtils.getLatestSocValue(device);
        long percent = round(SoCUtils.getPercentFromSoc(soc));


        if (intent.getExtras().containsKey(CTEK.EXTRA_BLE_LIVE_VOLTAGE)) {
          voltage = intent.getExtras().getDouble(CTEK.EXTRA_BLE_LIVE_VOLTAGE);
        } else if (intent.getExtras().containsKey(CTEK.EXTRA_BLE_LIVE_TEMPERATURE)) {
          temperature = intent.getExtras().getDouble(CTEK.EXTRA_BLE_LIVE_TEMPERATURE);
        }

        Intent updateIntent = new Intent(context, UpdateService.class);
        updateIntent.setAction(CTEK.EXTRA_BLE_LIVE_VOLTAGE);
        updateIntent.putExtra(CTEK.EXTRA_BLE_LIVE_VOLTAGE, voltage);
        updateIntent.putExtra(CTEK.EXTRA_BLE_LIVE_TEMPERATURE, temperature);
        updateIntent.putExtra(UpdateService.BATTERY_PERCENTAGE_UPDATE, percent);
        updateIntent.putExtra(UpdateService.BATTERY_DEVICE_NAME, device.getName());
        context.startService(updateIntent);

      } else if (DeviceManager.ACTION_DEVICE_FOUND.equals(intent.getAction())) { //New device added
        Long deviceId = intent.getLongExtra(DeviceManager.EXTRA_DEVICE_ID, -1);
        Device device = DeviceRepository.getDeviceForId(context, deviceId);
      } else if (DeviceManager.ACTION_DEVICE_DISCONNECTED.equals(intent.getAction())) { //Device removed
        Long deviceId = intent.getLongExtra(DeviceManager.EXTRA_DEVICE_ID, -1);

      } else if (UpdateService.WIDGET_DEVICE_REMOVED.equals(intent.getAction())) {
        Intent updateIntent = new Intent(context, UpdateService.class);
        updateIntent.setAction(UpdateService.WIDGET_DEVICE_REMOVED);
        context.startService(updateIntent);

      } else if (UpdateService.WIDGET_NEW_DEVICE_ADDED.equals(intent.getAction())) {
        Intent updateIntent = new Intent(context, UpdateService.class);
        updateIntent.setAction(UpdateService.WIDGET_NEW_DEVICE_ADDED);
        context.startService(updateIntent);

      } else if (UpdateService.WIDGET_DEVICE_UPDATED.equals(intent.getAction())) {
        Intent updateIntent = new Intent(context, UpdateService.class);
        updateIntent.setAction(UpdateService.WIDGET_DEVICE_UPDATED);
        context.startService(updateIntent);

      }


    }
  };

  /**
   * Creates the MonitorService.
   */
  public MonitorService() {
    super();
  }

  @Override
  public void onCreate() {
    super.onCreate();
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(CTEK.ACTION_LIVE_MODE);
    intentFilter.addAction(DeviceManager.ACTION_DEVICE_FOUND);
    intentFilter.addAction(DeviceManager.ACTION_DEVICE_DISCONNECTED);
    intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    intentFilter.addAction(UpdateService.WIDGET_DEVICE_REMOVED);
    intentFilter.addAction(UpdateService.WIDGET_NEW_DEVICE_ADDED);
    intentFilter.addAction(UpdateService.WIDGET_DEVICE_UPDATED);
    //Added
   // BluetoothLeManager.getInstance(this).startBTSearch();
    ///
    registerReceiver(broadcastReceiver, intentFilter);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    unregisterReceiver(broadcastReceiver);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    // We want this service to continue running until it is explicitly
    // stopped, so return sticky.
    return Service.START_STICKY;
  }

}
