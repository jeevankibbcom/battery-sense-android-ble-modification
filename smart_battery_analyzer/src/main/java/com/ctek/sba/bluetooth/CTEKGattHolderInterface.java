package com.ctek.sba.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by evgeny.akhundzhanov on 20.10.2016.
 */
public interface CTEKGattHolderInterface {

  Handler getHandler ();               // handler
  boolean isInLiveMode ();

  // onConnectionStateChange
  void discoverServices ();                           // mBluetoothGatt.discoverServices();

  // onServicesDiscovered
  BluetoothGattService getBaseService ();             // mBluetoothGatt.getService(SBADevice.Service.BASE_SERVICE_UUID);
  BluetoothGattService getInfoService ();             // mBluetoothGatt.getService(SBADevice.Service.DEVICE_INFORMATION_SERVICE_UUID);

  void broadcastUpdate (String action, String mac);   // broadcastUpdate(ACTION_SERVICE_STOPPED);
  void broadcastUpdateServiceStopped (String action, String mac, String reason);
  void broadcastUpdateConnectFailed (String action, String mac, String reason);

  // onDescriptorWrite
  void triggerBonding (BluetoothGattService baseService);

  // onCharacteristicRead
  void broadcastUpdate (String action, String mac, BluetoothGattCharacteristic characteristic);
  void onSerialNumberReceived (String mac, String serial);
  void onReadSerialFail (String mac);

  void onDeviceDataReady (String mac, List<Double> voltages, int historyCursor, boolean bGapInData);
  void onDeviceUpdateFail (String mac);

  void broadcastLiveVoltage (String mac, double value);
  void broadcastLiveTemperature (String mac, double value);   // MUST call this --> DeviceMap.getInstance().setDeviceCurrTemp(mDevice.getId(), dLiveTemperature);

} // EOI
