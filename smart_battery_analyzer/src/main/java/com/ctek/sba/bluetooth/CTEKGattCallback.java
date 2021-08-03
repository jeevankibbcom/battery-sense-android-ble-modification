package com.ctek.sba.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import greendao.Device;


/**
 * Created by evgeny.akhundzhanov on 20.10.2016.
 *
 * BluetoothLeManager class is too big. Trying to refactor.
 */
public class CTEKGattCallback extends BluetoothGattCallback {

  private Context ctx;
  private String  TAG;
  private Device  device;
  private String  mac;
  private CTEKGattHolderInterface icb;
  CTEKBLEManagerInterface.Task task;
  private BluetoothGatt mBluetoothGatt;

  private BluetoothGattService baseService;
  private BluetoothGattService infoService;

  // These are read from the device
  // Defaults are valid but over written on successfulk read
  private long uptime;
  private int interval;
  private int historyCursor;

  // Updates have been don up to this position/time
  private int currentCursor;
  private long timeOf_currentCursor;
  private boolean bGapInData;
  private int expectedRecords;

  private final List<Double> voltages = new ArrayList<>();

  // EA 14-Oct-2016. Special real-time mode.
  private long   lastRealLiveVoltageCall;
  private long   lastRealLiveTemperaCall;

  long getLastRealLiveVoltageCall () { return lastRealLiveVoltageCall; }
  long getLastRealLiveTemperaCall () { return lastRealLiveTemperaCall; }


  BluetoothGattService getBaseService () { return baseService; }

  CTEKGattCallback (Context ctx, String TAG, CTEKGattHolderInterface icb, CTEKBLEManagerInterface.Task task, Device device) {
    this.ctx = ctx;
    this.TAG = TAG;
    this.icb = icb;
    this.device = device;
    this.mac = device.getAddress();
    this.task = task;

    lastRealLiveVoltageCall = 0;
    lastRealLiveTemperaCall = 0;

    final Integer deviceReadCursor = device.getReadCursor();
    final Long deviceGetUpdated = device.getUpdated();
    init (deviceReadCursor, deviceGetUpdated);
  }

  private void init (Integer deviceReadCursor, Long deviceGetUpdated) {
    historyCursor = -1;
    voltages.clear();
    currentCursor = deviceReadCursor != null ? deviceReadCursor : 0;
    timeOf_currentCursor = deviceGetUpdated != null ? deviceGetUpdated : -1;
    // Set the expected number in writeVoltageCursor()
    expectedRecords = 0;
    bGapInData = false;

    uptime = 0;
    interval = 5;
  }

  void setBluetoothGatt (BluetoothGatt gatt) {
    mBluetoothGatt = gatt;
  }

  @Override
  public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
    // Log.d(TAG, "onConnectionStateChange: " + newState + " status = " + status);
    if (status == BluetoothGatt.GATT_SUCCESS) {
      if (newState == BluetoothProfile.STATE_CONNECTED) {
        // OK, we are connected
        Log.i(TAG, "1: Connected to GATT server.");

        // Attempt to discover services after successful connection.
        Log.i(TAG, "2: Discover services.");
        icb.discoverServices();

      } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        String reason = "Disconnected from GATT server.";
        Log.i(TAG, reason);
        icb.broadcastUpdateServiceStopped(CTEK.ACTION_SERVICE_STOPPED, mac, reason);
      }
    }
    else {
      String reason = "Error when connecting to GATT server.";
      Log.i(TAG, reason + " status = " + status);
      icb.broadcastUpdateConnectFailed(CTEK.ACTION_SERVICE_STOPPED, mac, reason);
    }
    return;
  }

  @Override
  public void onServicesDiscovered(BluetoothGatt gatt, int status) {
    // Assume no services found
    baseService = null;
    infoService = null;

    if (status == BluetoothGatt.GATT_SUCCESS) {

      baseService = icb.getBaseService();
      infoService = icb.getInfoService();

      if (baseService != null && infoService != null) {
        Log.i(TAG, "2: Services discovered.");
        if (task == CTEKBLEManagerInterface.Task.READING_SERIAL_NUMBER) {
          readSerialNumber();
        }
        else {
          Log.i(TAG, "3: Write keyhole.");
          writeToKeyHole();
        }
      }
    }

    if (baseService == null || infoService == null) {
      String reason = "onServicesDiscovered failed.";
      Log.d(TAG, reason);
      icb.broadcastUpdateServiceStopped(CTEK.ACTION_SERVICE_STOPPED, mac, reason);
    }
    return;
  }

  @Override
  public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
    UUID uuid = characteristic.getUuid();
    Log.d(TAG, String.format("onCharacteristicChanged: %s", SBADevice.Characteristic.uuid2Name(uuid)));
    if(SBADevice.Characteristic.VOLTAGE_HISTORY_UUID.equals(uuid)) {
      byte[] data = characteristic.getValue();
      onVoltageHistory(data);
    }
  }

  private void onReadUptimeFirst (BluetoothGattCharacteristic gattChar, int status) {
    // First time there should a specific status to indicate successful pairing.
    if (SBADevice.Status.CORRECT_KEY == status) {
      // Now we need to read uptime again to be sure it worked.
      Log.i(TAG, "10: Pairing confirmed (not completed).");
      readUptime();
      return;
    }

    // Handle pairing success or failure
    if (task == CTEKBLEManagerInterface.Task.PAIRING_DEVICE) {
      if (BluetoothGatt.GATT_SUCCESS == status) {
        Log.i(TAG, "11: Pairing confirmed and completed.");
        icb.broadcastUpdate(CTEK.ACTION_KEY_UNLOCKED, mac, gattChar);
      }
      else {
        Log.w(TAG, "Device key wrong. Device still locked and not paired");
        icb.broadcastUpdate(CTEK.ACTION_KEY_UNLOCK_FAIL, mac);
      }
    }
    return;
  }

  private void onReadUptime (BluetoothGattCharacteristic gattChar) {
    uptime = gattChar.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
    // old readInterval();
    if(icb.isInLiveMode()) {
      readLiveVoltage();
      icb.getHandler().postDelayed(new Runnable() { public void run() { readLiveTemperature(); }}, 500);
    }
    else {
      readInterval();
    }
    return;
  }

  private void onReadInterval (BluetoothGattCharacteristic gattChar) {
    interval = gattChar.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
    readHistoryCursor();
    return;
  }

  private void onReadHistoryCursor (BluetoothGattCharacteristic gattChar) {
    historyCursor = gattChar.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);

    long numberOfRecords;
    long timeOf_historyCursor;

    int oldestCursor;
    long timeOf_oldestCursor;

    // Establish number of records and cursor to oldest and newest
    if (uptime >= interval * 60 * 30000) {
      // Device storage wrapped, setup to read until end first.
      numberOfRecords = 30000;
      oldestCursor = historyCursor + 1;

      if (oldestCursor >= 30000) {
        oldestCursor = 0;
      }
    }
    else {
      // Not, wrapped. Read from start.
      numberOfRecords = historyCursor;
      oldestCursor = 0;
    }

    // Calculate time of device oldest and newest data
    if (interval == 0) {
      timeOf_historyCursor = -1;
      timeOf_oldestCursor = -1;
    }
    else {
      timeOf_historyCursor = System.currentTimeMillis() - (uptime % (interval * 60)) * 1000;
      timeOf_oldestCursor = timeOf_historyCursor - (numberOfRecords * interval * 60) * 1000;
    }

    if (timeOf_currentCursor > timeOf_historyCursor) {
      readLiveTemperature();

      // We are already up to date. Signal to UI that we did not get any new data.
      // ### onDeviceDataReady();
      icb.onDeviceDataReady(mac, voltages, historyCursor, bGapInData);
    }
    else if (numberOfRecords == 0){
      // There is no data on the device, read out a live voltage value
      if (uptime > 60) {
        readLiveVoltage();
        readLiveTemperature();
      }
      else {
        icb.onDeviceUpdateFail(mac);
      }
    }
    else {
      if (timeOf_currentCursor < timeOf_oldestCursor) {
        // There is a gap in data, indicate gap to get timestamps correctly set
        bGapInData = true;
        currentCursor = oldestCursor;
      }
      // Prepare reading history data using notifications
      enableHistoryNotifications();
    }
  }

  private void onReadSerial (BluetoothGattCharacteristic gattChar, int status) {
    if (status == BluetoothGatt.GATT_SUCCESS) {
      String serialNumber = gattChar.getStringValue(0);
      Log.d(TAG, String.format("serial number: %s", serialNumber));
      icb.onSerialNumberReceived(mac, serialNumber);
    }
    else {
      icb.onReadSerialFail(mac);
    }
    return;
  }

  private void onVoltageHistory (byte[] data) {

    CTEKData ctek = CTEK.parseVoltagesArray(CTEK.MODE_GET_BATTERY_AND_TEMP, data);
    Double[] voltagesArray = ctek.getVoltages();
    Log.d(TAG, "Voltages array: " + Arrays.toString(voltagesArray));
    Integer[] temp_ = ctek.getTemperatures();
    Log.d(TAG, "Temperatures array: " + Arrays.toString(temp_));

    voltages.addAll(Arrays.asList(voltagesArray));

    int totalLengthOfDataRead = voltages.size();

    if (currentCursor > historyCursor) {
      // We are reading to end of buffer, check against max
      if (totalLengthOfDataRead == expectedRecords) {
        // Ask for the rest
        currentCursor = 0;
        writeVoltageCursor();
      }
    }
    else {
      if (totalLengthOfDataRead == expectedRecords) {
        icb.onDeviceDataReady(mac, voltages, historyCursor, bGapInData);
        Log.d(TAG, "Total data length: " + totalLengthOfDataRead);
      }
    }
  }

  private void onVoltageCurrent (byte[] data) {
    CTEKData ctek = CTEK.parseVoltagesArray(CTEK.MODE_GET_BATTERY_LEVEL, data);
    Double[] voltagesArray = ctek.getVoltages();
    // Double[] temp_ = ctek.getTemperatures();
    // Log.d(TAG, "Temperatures array: " + Arrays.toString(temp_));
    Log.d(TAG, "onVoltageCurrent: Voltages: " + Arrays.toString(voltagesArray));

    if(icb.isInLiveMode()) {
      if(lastRealLiveVoltageCall!=0) {
        long now_msecs = System.currentTimeMillis();
        long passed = now_msecs - lastRealLiveVoltageCall;
        long delta  = 1000 - passed;
        if(delta < 0) delta = 0;
        icb.getHandler().postDelayed(new Runnable() { public void run() { readLiveVoltage(); }}, delta);

        if(voltagesArray!=null && (voltagesArray.length > 0) ) {
          icb.broadcastLiveVoltage(mac, voltagesArray[0]);
        }
      }
      else {
        readLiveVoltage();
      }
    }
    else {
      // This should not be the case in usual mode. But still.
      // icb.getHandler().postDelayed(new Runnable() { public void run() { readLiveVoltage(); }}, 1000); //Originally commented
    }
  }

  private void onTemperatureCurrent (double dLiveTemperature) {
    // ### DeviceMap.getInstance().setDeviceCurrTemp(mDevice.getId(), dLiveTemperature);
    if(icb.isInLiveMode()) {
      icb.broadcastLiveTemperature(mac, dLiveTemperature);

      if(lastRealLiveTemperaCall!=0) {
        long now_msecs = System.currentTimeMillis();
        long passed = now_msecs - lastRealLiveTemperaCall;
        long delta  = 1000 - passed;
        if(delta < 0) delta = 0;
        icb.getHandler().postDelayed(new Runnable() { public void run() { readLiveTemperature(); }}, delta);
      }
      else {
        readLiveTemperature();
      }
    }
    else {
      // This should not be the case in usual mode. But still.
      icb.getHandler().postDelayed(new Runnable() { public void run() { readLiveTemperature(); }}, 1000);
    }
    return;
  }

  @Override
  public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    UUID uuid = characteristic.getUuid();
    Log.i(TAG, String.format("onCharacteristicRead: %s, status: %d, value: %s ",
        SBADevice.Characteristic.uuid2Name(uuid),
        status,
        Arrays.toString(characteristic.getValue())));

    // Handle uptime first to manage paring.
    if (SBADevice.Characteristic.UPTIME_UUID.equals(uuid)) {

      // ### onReadUptimeFirst(characteristic, status);

      // First time there should a specific status to indicate successful pairing.
      if (SBADevice.Status.CORRECT_KEY == status) {
        // Now we need to read uptime again to be sure it worked.
        Log.i(TAG, "10: Pairing confirmed (not completed).");
        readUptime();
        return;
      }

      // Handle pairing success or failure
      if (task == CTEKBLEManagerInterface.Task.PAIRING_DEVICE) {
        if (BluetoothGatt.GATT_SUCCESS == status) {
          Log.i(TAG, "11: Pairing confirmed and completed.");
          icb.broadcastUpdate(CTEK.ACTION_KEY_UNLOCKED, mac, characteristic);
        }
        else {
          Log.w(TAG, "Device key wrong. Device still locked and not paired");
          icb.broadcastUpdate(CTEK.ACTION_KEY_UNLOCK_FAIL, mac);
        }
        return;
      }
    }

    // Handle serial number
    if (SBADevice.Characteristic.SERIAL_NUMBER_UUID.equals(uuid)) {
      onReadSerial(characteristic, status);
    }

    if (status != BluetoothGatt.GATT_SUCCESS) {
      // GATT read failed
      icb.onDeviceUpdateFail(mac);
      return;
    }

    // Proceed with normal battery data update

    // Update is started by reading uptime after keyhole write and bonding
    if (SBADevice.Characteristic.UPTIME_UUID.equals(uuid)) {
      onReadUptime(characteristic);
    }
    // Next: get the measurement interval
    else if (SBADevice.Characteristic.MEASUREMENT_INTERVAL_UUID.equals(uuid)) {
      onReadInterval(characteristic);
    }
    // Next: get the writepos
    else if (SBADevice.Characteristic.VOLTAGE_HISTORY_CURSOR_UUID.equals(uuid)) {
      onReadHistoryCursor(characteristic);
    }

    // Live voltage value was requested
    else if (SBADevice.Characteristic.VOLTAGE_CURRENT_UUID.equals(uuid)) {
      byte[] data = characteristic.getValue();
      onVoltageCurrent(data);
    }
    else if (SBADevice.Characteristic.TEMPERATURE_CURRENT_UUID.equals(uuid)) {
      byte[] data = characteristic.getValue();
      if((data!=null) && (data.length > 0)) {
        int iLiveTemp = (int) data[0];
        double dLiveTemperature = Math.round(((double)iLiveTemp) / 2.f);
        Log.e(TAG, "CURRENT TEMPERATURE = " + dLiveTemperature);
        onTemperatureCurrent(dLiveTemperature);
      }
    }
    return;
  }

  @Override
  public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
    Log.d(TAG, String.format("onDescriptorRead, uuid: %s", descriptor.getUuid().toString()));
  }

  @Override
  public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
    Log.d(TAG, String.format("onDescriptorWrite, uuid: %s", descriptor.getUuid().toString()));
    if(BluetoothGatt.GATT_SUCCESS == status) {
      icb.getHandler().postDelayed(new Runnable() { public void run() { subscribeToVoltageNotifications(); }}, 0);
      // EA 31-Oct-2016. onDescriptorWrite is not used in LIVE mode.
      // ### icb.getHandler().postDelayed(new Runnable() { public void run() { subscribeToTemperatureNotifications(); }}, 500);
    }
  }

  @Override
  public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    UUID uuid = characteristic.getUuid();
    Log.d(TAG, String.format("onCharacteristicWrite: %s, status: %d, value: %s ",
        SBADevice.Characteristic.uuid2Name(uuid),
        status,
        Arrays.toString(characteristic.getValue())));
    if (status == BluetoothGatt.GATT_SUCCESS) {
      if (SBADevice.Characteristic.KEY_HOLE_UUID.equals(uuid)) {
        Log.i(TAG, "3: Keyhole written.");
        icb.triggerBonding(baseService);
      }
    }
    else {
      Log.w(TAG, "Characteristic write failed");
    }
  }

  private boolean checkBluetoothGatt () {
    if(mBluetoothGatt!=null) {
      return true;
    }
    Log.e(TAG, "Invalid call. getBluetoothGatt() returns null.");
    return false;
  }

  private boolean checkBaseService () {
    if(baseService!=null) {
      return true;
    }
    Log.e(TAG, "Our base service is null. Invalid device.");
    return false;
  }

  private boolean checkInfoService () {
    if(infoService!=null) {
      return true;
    }
    Log.e(TAG, "Our info service is null. Invalid device.");
    return false;
  }


  private void readSerialNumber() {
    if (checkBluetoothGatt() && checkInfoService()) {
      BluetoothGattCharacteristic gattChar = infoService.getCharacteristic(SBADevice.Characteristic.SERIAL_NUMBER_UUID);
      boolean success = mBluetoothGatt.readCharacteristic(gattChar);
      if(!success) {
        Log.e(TAG, String.format("Serial read: %s", success));
      }
    }
    return;
  }

  private boolean writeToKeyHole() {
    if (checkBluetoothGatt() && checkBaseService()) {
      BluetoothGattCharacteristic gattChar = baseService.getCharacteristic(SBADevice.Characteristic.KEY_HOLE_UUID);
      String serial = device.getSerialnumber();
      if (serial != null) {
        Log.d(TAG, String.format("Unlocking with serial %s", serial));
        gattChar.setValue(BondingHelper.MD5EncryptPasscode(serial));
        boolean success = mBluetoothGatt.writeCharacteristic(gattChar);
        if(!success) {
          Log.e(TAG, String.format("Write to key hole result: %s", success));
        }
        return success;
      }
    }
    return false;
  }

  private void subscribeToVoltageNotifications() {
    if (checkBluetoothGatt() && checkBaseService()) {
      BluetoothGattCharacteristic gattChar = baseService.getCharacteristic(SBADevice.Characteristic.VOLTAGE_HISTORY_UUID);
      boolean successVoltage = mBluetoothGatt.setCharacteristicNotification(gattChar, true);
      Log.d(TAG, String.format("Subscribe to voltage service: %s", successVoltage));
      writeVoltageCursor();
    }
  }

  /*
  private void subscribeToTemperatureNotifications() {
    if (checkBluetoothGatt() && checkBaseService()) {
      BluetoothGattCharacteristic gattChar = baseService.getCharacteristic(SBADevice.Characteristic.TEMPERATURE_CURRENT_UUID);
      boolean success = mBluetoothGatt.setCharacteristicNotification(gattChar, true);
      Log.e(TAG, String.format("Subscribe to temperature service: %s", success));
    }
  }
  */

  private void enableHistoryNotifications() {
    if (checkBluetoothGatt() && checkBaseService()) {
      BluetoothGattCharacteristic voltageCharacteristic = baseService.getCharacteristic(SBADevice.Characteristic.VOLTAGE_HISTORY_UUID);
      List<BluetoothGattDescriptor> descriptors = voltageCharacteristic.getDescriptors();
      BluetoothGattDescriptor descriptor = descriptors.get(0);
      descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

      // on the onDescriptorWrite() above we call subscribeToVoltageNotifications()
      boolean success = mBluetoothGatt.writeDescriptor(descriptor);
      if(!success) {
        Log.e(TAG, String.format("Write to notification descriptor: %s", success));
      }
    }
  }

  private void readUptime() {
    if (checkBluetoothGatt() && checkBaseService()) {
      BluetoothGattCharacteristic gattChar = baseService.getCharacteristic(SBADevice.Characteristic.UPTIME_UUID);
      boolean success = mBluetoothGatt.readCharacteristic(gattChar);
      if(!success) {
        Log.e(TAG, String.format("Uptime read: %s", success));
      }
    }
  }

  private void readLiveVoltage() {
    if (checkBluetoothGatt() && checkBaseService()) {
      boolean success = mBluetoothGatt.readCharacteristic(baseService.getCharacteristic(SBADevice.Characteristic.VOLTAGE_CURRENT_UUID));
      if(!success) {
        Log.e(TAG, String.format("Live voltage read: %s", success));
      }
      if(success) {
        lastRealLiveVoltageCall = System.currentTimeMillis();
      }
    }
  }

  private void readLiveTemperature() {
    if (checkBluetoothGatt() && checkBaseService()) {
      boolean success = mBluetoothGatt.readCharacteristic(baseService.getCharacteristic(SBADevice.Characteristic.TEMPERATURE_CURRENT_UUID));
      if(!success) {
        Log.e(TAG, String.format("Live temperature read: %s", success));
      }
      if(success) {
        lastRealLiveTemperaCall = System.currentTimeMillis();
      }
    }
  }

  private void readInterval() {
    if (checkBluetoothGatt() && checkBaseService()) {
      boolean success = mBluetoothGatt.readCharacteristic(baseService.getCharacteristic(SBADevice.Characteristic.MEASUREMENT_INTERVAL_UUID));
      if(!success) {
        Log.e(TAG, String.format("Measurement interval read: %s", success));
      }
    }
  }

  private void readHistoryCursor() {
    if (checkBluetoothGatt() && checkBaseService()) {
      boolean success = mBluetoothGatt.readCharacteristic(baseService.getCharacteristic(SBADevice.Characteristic.VOLTAGE_HISTORY_CURSOR_UUID));
      if(!success) {
        Log.e(TAG, String.format("Cursor position read: %s", success));
      }
    }
  }

  private void writeVoltageCursor() {
    if (currentCursor == -1 || historyCursor == -1 ) {
      icb.onDeviceUpdateFail(mac);
      return;
    }

    // Prepare history read command
    byte[] history;
    if (currentCursor > historyCursor) {
      // We need to read until end first
      expectedRecords = 30000 - currentCursor;
      history = CTEK.getHistoryCommand(currentCursor, 30000);
    }
    else {
      expectedRecords += historyCursor - currentCursor;
      history = CTEK.getHistoryCommand(currentCursor, historyCursor);
    }

    if (checkBluetoothGatt() && checkBaseService()) {
      BluetoothGattCharacteristic voltageWriteCharacteristic = baseService.getCharacteristic(SBADevice.Characteristic.VOLTAGE_HISTORY_WRITE_UUID);
      voltageWriteCharacteristic.setValue(history);
      boolean success = mBluetoothGatt.writeCharacteristic(voltageWriteCharacteristic);
      if(!success) {
        Log.e(TAG, String.format("Write to voltage: %s", success));
      }
    }
  }

} // EOClass CTEKGattCallback
