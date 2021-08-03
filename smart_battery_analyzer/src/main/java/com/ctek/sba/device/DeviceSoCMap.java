package com.ctek.sba.device;

import android.content.Context;
import com.ctek.sba.soc.VoltageSections;

import java.util.Map;
import java.util.TreeMap;

import greendao.Device;

/**
 * Created by evgeny.akhundzhanov on 02.05.2017.
 */

public class DeviceSoCMap {

  private static DeviceSoCMap instance;

  public static DeviceSoCMap initInstance (Context ctx) {
    if(instance == null) {
      instance = new DeviceSoCMap(ctx);
    }
    return instance;
  }

  public static DeviceSoCMap getInstance () {
    return instance;
  }

  private Context ctx;
  private Map<Long, DeviceSoC> map;

  private DeviceSoCMap(Context ctx) {
    this.ctx = ctx;
    restore();
  }


  /*
  private static final String PREFS_SOC = "device_soc_prefs";
  private static final String KEY_NUMBER_OF_DEVICES = "size";
  private static final String KEY_DEVICE_ID = "device_id_";

  private SharedPreferences getPrefs() {
    return ctx.getSharedPreferences(PREFS_SOC, Context.MODE_PRIVATE);
  }

  // EA. 02-May-2017. Actually only device IDs are saved.
  private void save () {
    SharedPreferences.Editor edit = getPrefs().edit();
    edit.clear();
    edit.putInt(KEY_NUMBER_OF_DEVICES, map.size());
    int kk = 0;
    for(Map.Entry<Long, DeviceSoC> entry : map.entrySet()) {
      kk++;
      edit.putLong (KEY_DEVICE_ID + kk, entry.getKey());
    }
    edit.commit();
    return;
  }

  // Device data is restored with empty cache.
  private void restore () {
    map = new TreeMap<>();

    SharedPreferences prefs = getPrefs();
    int kk, size = prefs.getInt(KEY_NUMBER_OF_DEVICES, 0);
    for(kk=1;kk<=size;++kk) {
      Long device_id = prefs.getLong(KEY_DEVICE_ID + kk, -1);
      if(device_id!=-1) {
        map.put(device_id, new DeviceSoC());
      }
    }
    return;
  }
  */

  private void save () {}
  private void restore () {
    map = new TreeMap<>();
  }

  public void setDeviceSoCSections (Device device, VoltageSections sections) {
    long device_id = device.getId();
    map.put(device_id, new DeviceSoC(device, sections)); // insert default values
    save();
    return;
  }

  public DeviceSoC getDeviceSoC (Device device) {
    long device_id = device.getId();
    if(map.containsKey(device_id)) {
      return map.get(device_id);
    }
    return null;
  }

  public void removeDevice (long device_id) {
    map.remove(device_id);
    save();
    return;
  }

  public void setDeviceSoC (Device device, DeviceSoC SoC) {
    long device_id = device.getId();
    if(map.containsKey(device_id)) {
      map.put(device_id, SoC);
      save();
    }
    return;
  }

} // EOClass DeviceSoCMap
