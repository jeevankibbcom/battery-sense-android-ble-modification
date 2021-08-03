package com.ctek.sba.device;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by evgeny.akhundzhanov on 04.10.2016.
 *
 * We need some new device fields, like
 * 1. current temperature
 * 2. device capacity
 *
 * BUT we do not want to change existing database to avoid problems with update from old app to new.
 * class DeviceMap stores new device properties in SharedPreferences.
 *
 */
public class DeviceMap {

  private static DeviceMap instance;

  public static DeviceMap initInstance (Context ctx) {
    if(instance == null) {
      instance = new DeviceMap(ctx);
    }
    return instance;
  }

  public static DeviceMap getInstance () {
    return instance;
  }

  private Context ctx;
  private Map<Long, DeviceProperties> map;

  private DeviceMap(Context ctx) {
    this.ctx = ctx;
    restore();
  }


  private static final String PREFS = "device_props_prefs";
  private static final String KEY_NUMBER_OF_DEVICES = "size";
  private static final String KEY_DEVICE_ID = "device_id_";
  private static final String KEY_CAPACITY  = "capacity_";
  private static final String KEY_CURRTEMP  = "currtemp_";
  private static final String KEY_TIME_ADD_ = "timeadd_";
  private static final String KEY_UPDATE_TIME_BAT_STATUS_ = "update_time_bat_status_";
  private static final String KEY_UPDATE_TIME_NOT_SYNCED_ = "update_time_not_synced_";

  private SharedPreferences getPrefs() {
    return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }

  private void save () {
    SharedPreferences.Editor edit = getPrefs().edit();
    edit.clear();
    edit.putInt(KEY_NUMBER_OF_DEVICES, map.size());
    int kk = 0;
    for(Map.Entry<Long, DeviceProperties> entry : map.entrySet()) {
      kk++;
      edit.putLong (KEY_DEVICE_ID + kk, entry.getKey());
      edit.putFloat(KEY_CAPACITY + kk, (float) entry.getValue().dCapacity);
      edit.putFloat(KEY_CURRTEMP + kk, (float) entry.getValue().dCurrTemp);
      edit.putLong (KEY_TIME_ADD_+ kk, entry.getValue().msecsAdded);
      edit.putLong (KEY_UPDATE_TIME_BAT_STATUS_+ kk, entry.getValue().updatedTimeWhenNotified_BAT_STATUS);
      edit.putLong (KEY_UPDATE_TIME_NOT_SYNCED_+ kk, entry.getValue().updatedTimeWhenNotified_NOT_SYNCED);
    }
    edit.commit();
    return;
  }

  private void restore () {
    map = new TreeMap<>();

    SharedPreferences prefs = getPrefs();
    int kk, size = prefs.getInt(KEY_NUMBER_OF_DEVICES, 0);
    for(kk=1;kk<=size;++kk) {
      Long device_id = prefs.getLong(KEY_DEVICE_ID + kk, -1);
      if(device_id!=-1) {
        float dCapacity = prefs.getFloat(KEY_CAPACITY + kk, DeviceProperties.DFLT_CAPACITY);
        float dCurrTemp = prefs.getFloat(KEY_CURRTEMP + kk, DeviceProperties.DFLT_CURRTEMP);
        long  msecsAdded= prefs.getLong(KEY_TIME_ADD_+ kk, DeviceProperties.DFLT_MSECS_ADDED);
        long updatedTimeWhenNotified_BAT_STATUS = prefs.getLong(KEY_UPDATE_TIME_BAT_STATUS_+ kk, DeviceProperties.DFLT_updatedTime);
        long updatedTimeWhenNotified_NOT_SYNCED = prefs.getLong(KEY_UPDATE_TIME_NOT_SYNCED_+ kk, DeviceProperties.DFLT_updatedTime);
        map.put(device_id, new DeviceProperties(dCapacity, dCurrTemp, msecsAdded, updatedTimeWhenNotified_BAT_STATUS, updatedTimeWhenNotified_NOT_SYNCED));
      }
    }
    return;
  }

  private DeviceProperties getDeviceProperties4DeviceId (long device_id) {
    if(!map.containsKey(device_id)) {
      map.put(device_id, new DeviceProperties()); // insert default values
      save();
    }
    return map.get(device_id);
  }

  public double getDeviceCapacity (long device_id) {
    return getDeviceProperties4DeviceId(device_id).dCapacity;
  }

  public double getDeviceCurrTemp (long device_id) {
    return getDeviceProperties4DeviceId(device_id).dCurrTemp;
  }

  public long getDeviceTimestamp (long device_id) {
    return getDeviceProperties4DeviceId(device_id).msecsAdded;
  }

  public void setDeviceCapacity (long device_id, double dCapacity) {
    DeviceProperties props = getDeviceProperties4DeviceId(device_id);
    if(props.dCapacity != dCapacity) {
      props.dCapacity = dCapacity;
      save();
    }
  }

  public void setDeviceCurrTemp (long device_id, double dCurrTemp) {
    DeviceProperties props = getDeviceProperties4DeviceId(device_id);
    if(props.dCurrTemp != dCurrTemp) {
      props.dCurrTemp = dCurrTemp;
      save();
    }
  }

  public long getUpdatedTimeWhenNotified_BAT_STATUS (long device_id) {
    return getDeviceProperties4DeviceId(device_id).updatedTimeWhenNotified_BAT_STATUS;
  }

  public void setUpdatedTimeWhenNotified_BAT_STATUS (long device_id, long updated) {
    DeviceProperties props = getDeviceProperties4DeviceId(device_id);
    if(props.updatedTimeWhenNotified_BAT_STATUS != updated) {
      props.updatedTimeWhenNotified_BAT_STATUS = updated;
      save();
    }
  }


  public long getUpdatedTimeWhenNotified_NOT_SYNCED (long device_id) {
    return getDeviceProperties4DeviceId(device_id).updatedTimeWhenNotified_NOT_SYNCED;
  }

  public void setUpdatedTimeWhenNotified_NOT_SYNCED (long device_id, long updated) {
    DeviceProperties props = getDeviceProperties4DeviceId(device_id);
    if(props.updatedTimeWhenNotified_NOT_SYNCED != updated) {
      props.updatedTimeWhenNotified_NOT_SYNCED = updated;
      save();
    }
  }



  public void removeDevice (long device_id) {
    map.remove(device_id);
    save();
  }

} // EOClass DeviceMap
