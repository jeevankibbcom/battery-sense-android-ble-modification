package com.ctek.sba.bluetooth;

import android.util.Log;
import android.util.Pair;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import greendao.Device;

/**
 * Created by evgeny.akhundzhanov on 18.10.2016.
 */
public class DeviceUpdateList {

  public static final int NOT_FOUND = -1;

  private List<DeviceUpdateInfo> list = new ArrayList<>();
  private int iSelected4Update = -1;

  synchronized void clear() { list.clear(); iSelected4Update = -1; }
  synchronized void add (Device device) { list.add(new DeviceUpdateInfo(device)); }
  synchronized int size() {
    return list.size();
  }
  synchronized DeviceUpdateInfo getDeviceUpdateInfo (int index) { return list.get(index); }

  synchronized int getDeviceUpdateInfoIndex (String address) {
    for (int kk=0;kk<size();++kk) {
      DeviceUpdateInfo info = list.get(kk);
      if (info.device.getAddress().equals(address)) {
        return kk;
      }
    }
    return NOT_FOUND;
  }


  synchronized void registerResult (String TAG, String mac, boolean bResult) {
    int index = getDeviceUpdateInfoIndex(mac);
    if(index!=NOT_FOUND) {
      DeviceUpdateInfo info = getDeviceUpdateInfo(index);
      info.registerResult(bResult);
    }
    else {
      Log.d(TAG, "registerResult mac NOT FOUND: " + mac);
    }
    return;
  }

  synchronized DeviceUpdateInfo getNextDeviceUpdateInfo () {
    iSelected4Update++;
    if(iSelected4Update < size()) {
      return getDeviceUpdateInfo(iSelected4Update);
    }
    return null;
  }

  synchronized void reportList (String TAG) {
    // report to log
    int mDevicesToUpdate = size();
    Log.d(TAG, "buildDeviceUpdateList mDevicesToUpdate = " + mDevicesToUpdate);
    for(int kk=0;kk<mDevicesToUpdate;++kk) {
      Device deviceKK = getDeviceUpdateInfo(kk).device;
      Log.d(TAG, "Device " + (kk+1) + "/. serial = " + deviceKK.getSerialnumber() + " name = " + deviceKK.getName() + " " + deviceKK.getAddress());
    }
    return;
  }

  synchronized Pair<Integer, Integer> getUpdateSummary () {
    int countSuccess = 0;
    for (int kk=0;kk<size();++kk) {
      DeviceUpdateInfo info = list.get(kk);
      if(info.isUpdateSuccessfull()) {
        countSuccess++;
      }
    }
    return new Pair<Integer, Integer>(countSuccess, size());
  }

  synchronized void reportResults (String TAG) {
    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm:ss");

    Log.d(TAG, "DeviceUpdateList report START +++++++ Devices: " + size());
    for (int kk=0;kk<size();++kk) {
      DeviceUpdateInfo info = list.get(kk);
      Log.d(TAG, "" + (kk+1) + "/. Started: " + sdf.format(info.start_msecs) + " Duration " + info.getDurationSeconds() + " s. Result = " + info.result);
    }
    Log.d(TAG, "DeviceUpdateList report FINAL =======");
    return;
  }

  synchronized DeviceUpdateInfo findDeviceUpdateInfo4Mac (String mac) {
    for (int kk=0;kk<size();++kk) {
      DeviceUpdateInfo info = list.get(kk);
      if(info.device.getAddress().equals(mac)) {
        return info;
      }
    }
    return null;
  }

  synchronized void sortByUpdateTimeDESC () {
    if(size() > 1) {
      Collections.sort(list, DeviceUpdateInfo.compareByUpdateTimeDESC);
    }
  }

} // EOClass DeviceUpdateList
