package com.ctek.sba.rest;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.Pair;

import com.ctek.sba.device.DeviceMap;
import com.ctek.sba.device.DeviceSoCMap;
import com.ctek.sba.io.dao.DeviceRepository;
import com.ctek.sba.util.SettingsHelper;

import java.util.List;
import java.util.Locale;

import greendao.Device;

/**
 * Created by evgeny.akhundzhanov on 03.04.2018.
 */

public class SrvPostCapacity extends RESTIntentService {

  private final static String TAG = "SrvPostCapacity";

  private final static String DEVICE_ID = "device_id";
  private final static String CAPACITY_ = "capacity_";


  public static void postAllCapacities (Context ctx) {
    Log.d(TAG, "Posting ALL capacities on application startup.");
    List<Device> myDevices = DeviceRepository.getAllDevices(ctx);
    for (Device device : myDevices) {
      Long device_id = device.getId();
      SrvPostCapacity.start(ctx, device_id, DeviceMap.getInstance().getDeviceCapacity(device_id));
    }
    return;
  }


  public static void start (Context ctx, long device_id, double capacity) {
    String mess = "";
    if(SettingsHelper.isDataCollectionSet(ctx)) {
      if(Network.isOnline(ctx)) {
        ctx.startService(new Intent(ctx, SrvPostCapacity.class).putExtra(DEVICE_ID, device_id).putExtra(CAPACITY_, capacity));
        mess = "StartService called.";
      }
      else {
        mess = "Network is n/a.";
      }
    }
    else {
      mess = "Data collection is NOT set. Post capacity - IGNORED";
    }
    Log.d(TAG, mess + " device_id = " + device_id + " Capacity = " + capacity);
    return;
  }

  public SrvPostCapacity() {
    super(TAG);
  }


  private long    device_id;
  private double  capacity_;

  @Override
  protected void onHandleIntent(Intent i_) {
    super.onHandleIntent(i_);

    device_id = i_.getLongExtra(DEVICE_ID, 0);
    capacity_ = i_.getDoubleExtra(CAPACITY_, 0);

    postCapacity();

    timer.stop();
    Log.d(TAG, "Service Stopped ======= Total Seconds: " + timer.getSeconds());

    return;
  }

  private void postCapacity () {
    Device device = DeviceRepository.getDeviceForId(this, device_id);
    String serial = device.getSerialnumber();
    Log.d(TAG, "Device = " + serial + " Capacity =  " + capacity_);

    long msecs = System.currentTimeMillis();
    CSensor sensor3 = new CSensor(serial, "BatteryCapacity").add(msecs, String.format(Locale.ROOT,"%.2f", capacity_));
    CIngestData data = new CIngestData();

    Log.d(TAG, "Device = " + serial + " JSON = " + data.toString());data.addSensor(sensor3);

    Pair<Boolean, String> result = new HConnection().postJson(REST.CTEK_SERVER, token, data.toString());

    if(result.first) {
      Log.d(TAG, "Device = " + serial + " POST success.");
    }
    else {
      Log.d(TAG, "Device = " + serial + " POST failed: " + result.second);
    }
    return;
  }

} // EOClass SrvPostCapacity
