package com.ctek.sba.bluetooth;

import java.util.ArrayList;
import java.util.List;

import greendao.Device;

/**
 * Created by evgeny.akhundzhanov on 18.10.2016.
 */
public class DeviceListUI {

  public List<DeviceInfoUI> list;

  public DeviceListUI (List<Device> devices) {
    list = new ArrayList<>();
    for(Device device : devices) {
      list.add(new DeviceInfoUI(device));
    }
  }

  public DeviceListUI (DeviceInfoUI item) {
    list = new ArrayList<>();
    list.add(item);
  }

  public int size () { return (list!=null) ? list.size() : 0; }
  public DeviceInfoUI getItem (int index) { return list.get(index); }

  public void cleanUpdatedNow () {
    int kk, kkSize = list!=null ? list.size() : 0;
    for(kk=0;kk<kkSize;++kk) {
      list.get(kk).setIsUpdatedNow(false);
    }
    return;
  }

} // EOClass DeviceListUI
