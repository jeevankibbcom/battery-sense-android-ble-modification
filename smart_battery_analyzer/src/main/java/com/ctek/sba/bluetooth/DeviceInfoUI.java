package com.ctek.sba.bluetooth;

import greendao.Device;

/**
 * Created by evgeny.akhundzhanov on 18.10.2016.
 */
public class DeviceInfoUI {

  private Device device;
  private boolean isUpdatedNow;

  public DeviceInfoUI (Device device) {
    this.device = device;
    isUpdatedNow = false;
  }

  public Device getDevice () { return device; }
  public boolean getIsUpdatedNow () { return isUpdatedNow; }
  public void setIsUpdatedNow (boolean isUpdated) { this.isUpdatedNow = isUpdated; }

} // EOClass DeviceInfoUI
