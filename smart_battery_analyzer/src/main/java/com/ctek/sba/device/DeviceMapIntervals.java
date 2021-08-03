package com.ctek.sba.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ctek.sba.io.dao.DeviceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import greendao.Device;

/**
 * Created by evgeny.akhundzhanov on 02.04.2018.
 */

public class DeviceMapIntervals {

  private static DeviceMapIntervals instance;

  public static DeviceMapIntervals initInstance (Context ctx) {
    if(instance == null) {
      instance = new DeviceMapIntervals(ctx);
    }
    return instance;
  }

  public static DeviceMapIntervals getInstance () {
    return instance;
  }

  private Context ctx;
  private Map<Long, List<TimeInterval>> map;

  private DeviceMapIntervals(Context ctx) {
    this.ctx = ctx;
    restore();
  }

  private static final String PREFS = "device_intervals";
  private static final String KEY_NUMBER_OF_DEVICES = "size";
  private static final String KEY_DEVICE_ID = "device_id_";
  private static final String KEY_DEVICE_LIST_SIZE_KK = "list_size_";
  private static final String KEY_INTERVAL_KK_II  = "interval_";


  private SharedPreferences getPrefs() {
    return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }

  private void save () {
    SharedPreferences.Editor edit = getPrefs().edit();
    edit.clear();
    edit.putInt(KEY_NUMBER_OF_DEVICES, map.size());
    int kk = 0;
    for(Map.Entry<Long, List<TimeInterval>> entry : map.entrySet()) {
      kk++;
      edit.putLong (KEY_DEVICE_ID + kk, entry.getKey());

      List<TimeInterval> list = entry.getValue();
      int ii, iiListSize = list.size();
      edit.putInt (KEY_DEVICE_LIST_SIZE_KK + kk, iiListSize);

      for(ii=0;ii<iiListSize;++ii) {
        edit.putString(KEY_INTERVAL_KK_II + kk + "_" + ii, list.get(ii).toString());
      }

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
        int ii, iiListSize = prefs.getInt(KEY_DEVICE_LIST_SIZE_KK + kk, 0);
        // Log.d("SrvPostSocs", "Restore device " + device_id + ". Intervals = " + iiListSize);
        List<TimeInterval> list = new ArrayList<TimeInterval>();
        for(ii=0;ii<iiListSize;++ii) {
          String packed = prefs.getString(KEY_INTERVAL_KK_II + kk + "_" + ii, "");
          TimeInterval interval = new TimeInterval(packed);
          if(interval.isValid()) {
            list.add(interval);
          }
        }
        map.put(device_id, list);
      }
    }
    return;
  }

  public void removeDevice (long device_id) {
    map.remove(device_id);
    save();
  }


  public static boolean containsIntervalExact (List<TimeInterval> intervals, TimeInterval interval) {
    int ii, iiListSize = intervals.size();
    for(ii=0;ii<iiListSize;++ii) {
      TimeInterval interval_II = intervals.get(ii);
      if(interval_II.isEqual(interval)) {
        return true;
      }
    }
    return false;
  }

  public static boolean containsIntervalInside (List<TimeInterval> intervals, TimeInterval interval) {
    int ii, iiListSize = intervals.size();
    for(ii=0;ii<iiListSize;++ii) {
      TimeInterval interval_II = intervals.get(ii);
      if(interval_II.containsInside(interval)) {
        return true;
      }
    }
    return false;
  }
  public static int containsIntervalStart (List<TimeInterval> intervals, TimeInterval interval) {
    int ii, iiListSize = intervals.size();
    for(ii=0;ii<iiListSize;++ii) {
      TimeInterval interval_II = intervals.get(ii);
      if(interval_II.hasSameStart(interval)) {
        return ii;
      }
    }
    return -1; // not found
  }

  public static int isNextInterval (List<TimeInterval> intervals, TimeInterval interval) {
    int ii, iiListSize = intervals.size();
    for(ii=0;ii<iiListSize;++ii) {
      TimeInterval interval_II = intervals.get(ii);
      if(interval_II.isPrevious(interval)) {
        return ii;
      }
    }
    return -1; // not found
  }

  public static boolean containsTimestamp (List<TimeInterval> intervals, long msecs) {
    int ii, iiListSize = intervals.size();
    for(ii=0;ii<iiListSize;++ii) {
      TimeInterval interval_II = intervals.get(ii);
      if(interval_II.isInside(msecs)) {
        return true;
      }
    }
    return false;
  }

  public static void logIntervalsAll (Context ctx) {
    List<Device> myDevices = DeviceRepository.getAllDevices(ctx);
    for (Device device : myDevices) {
      logIntervals(ctx, device.getId(), device.getSerialnumber());
    }
    return;
  }

  public static void logIntervals (Context ctx, Long device_id, String serial) {
    List<TimeInterval> intervals = DeviceMapIntervals.getInstance().getIntervals4Device(device_id);
    int ii, iiSize = intervals.size();
    Log.d("SrvPostSocs", "Device " + device_id + "/. " + serial + ". Intervals: " + iiSize);
    for(ii=0;ii<iiSize;++ii) {
      TimeInterval i_ = intervals.get(ii);
      Log.d("SrvPostSocs", "" + (ii+1) + "/. " + i_.toLog());
    }
  }




  public void registerSuccessInterval (long device_id, TimeInterval interval) {
    if(!map.containsKey(device_id)) {
      map.put(device_id, new ArrayList<TimeInterval>());
    }
    List<TimeInterval> list = map.get(device_id);

    if(DeviceMapIntervals.containsIntervalInside(list, interval) || DeviceMapIntervals.containsIntervalExact(list, interval)) {
      // do nothing
    }
    else {
      int ind = DeviceMapIntervals.containsIntervalStart(list, interval);
      if(ind>=0) {
        list.set(ind, interval);  // expand interval
        Log.d("SrvPostSocs", "Interval " + (ind+1) + " EXPANDED. " + interval.toLog());
      }
      else {
        ind = DeviceMapIntervals.isNextInterval(list, interval);
        if(ind>=0) {
          TimeInterval merged = new TimeInterval(list.get(ind).getStart(), interval.getFinal());
          list.set(ind, merged);
          Log.d("SrvPostSocs", "Interval " + (ind+1) + " MERGED. " + merged.toLog());
        }
        else {
          list.add(interval);        // add interval
          Log.d("SrvPostSocs", "New Interval ADDED. " + interval.toLog());
        }
      }
      save();
    }
    return;
  }

  public List<TimeInterval> getIntervals4Device (long device_id) {
    if(!map.containsKey(device_id)) {
      map.put(device_id, new ArrayList<TimeInterval>());
      save();
    }
    return map.get(device_id);
  }

} // EOClass DeviceMapIntervals
