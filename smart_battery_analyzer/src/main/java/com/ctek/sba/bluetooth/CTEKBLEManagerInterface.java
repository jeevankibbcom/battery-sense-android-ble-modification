package com.ctek.sba.bluetooth;

import android.bluetooth.BluetoothGatt;

import greendao.Device;

/**
 * Created by evgeny.akhundzhanov on 20.10.2016.
 */
public interface CTEKBLEManagerInterface {

  boolean initialize ();
  void close ();


  void getDeviceSerialNumber (Device device); // Task.READING_SERIAL_NUMBER
  void updateBatteryData (Device device);     // Task.UPDATING_DEVICE

  // DeviceManagerHiQ doesn't support pairing for the moment.
  void pairDevice (Device device);            // Task.PAIRING_DEVICE

  void startLiveMode (String mac);            // live mode

  enum Task {
    UPDATING_DEVICE,
    READING_SERIAL_NUMBER,
    PAIRING_DEVICE
  }

} // EOI
