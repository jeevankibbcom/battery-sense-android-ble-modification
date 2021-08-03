package com.ctek.sba.bluetooth;

import java.util.Comparator;

import greendao.Device;

/**
 * Created by evgeny.akhundzhanov on 18.10.2016.
 */
public class DeviceUpdateInfo {

  Device  device;
  long    start_msecs;
  long    final_msecs;
  Boolean result;

  public DeviceUpdateInfo (Device device) {
    this.device = device;
    start_msecs = 0;
    final_msecs = 0;
    result = null;
  }

  void registerStart () {
    start_msecs = System.currentTimeMillis();
  }


  void registerResult (boolean bResult) {
    result = new Boolean(bResult);
    final_msecs = System.currentTimeMillis();
  }

  long getDurationSeconds () {
    return (final_msecs - start_msecs) / 1000;
  }

  boolean isUpdatedNow () { return (start_msecs!=0) && (final_msecs==0); }

  boolean isUpdateSuccessfull () { return result!=null && result==true; }

  static Comparator<DeviceUpdateInfo> compareByUpdateTimeDESC = new Comparator<DeviceUpdateInfo>() {
    public int compare(DeviceUpdateInfo info1, DeviceUpdateInfo info2) {
      Long updated1 = info1.device.getUpdated();
      Long updated2 = info2.device.getUpdated();

      long msecs1 = (updated1!=null) ? updated1 : 0;
      long msecs2 = (updated2!=null) ? updated2 : 0;
      if (msecs1 < msecs2) return +1;
      if (msecs1 > msecs2) return -1;
      return 0;
    }
  };

} // EOClass DeviceUpdateInfo
