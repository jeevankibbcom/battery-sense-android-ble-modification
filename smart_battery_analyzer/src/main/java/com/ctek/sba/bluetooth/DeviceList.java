package com.ctek.sba.bluetooth;

import java.util.ArrayList;
import java.util.List;

import greendao.Device;

/**
 * Created by Mats Fahlén on 2016-06-30.
 *
 * Device List class with possibility to find device with a specific BLE-address.
 * Used for search result and devices in need of update.
 */
public class DeviceList {

  // List of devices found when scanning
  private List<Device> mFoundDevices = new ArrayList<>();

  // Empty the list
  public void clear() {
    mFoundDevices.clear();
  }

  // Add a new device to the list
  public void add(Device device) {
    mFoundDevices.add(device);
  }

  // Get a device with a given BLE address.
  // Return null if no device found
  public Device getDevice(String address) {
    for (Device device : mFoundDevices) {
      if (device.getAddress().equals(address)) {
        return device;
      }
    }
    return null;
  }

  // Get number of devices in list.
  public int size() {
    return mFoundDevices.size();
  }

  public Device getDevice (int index) { return mFoundDevices.get(index); }

}
