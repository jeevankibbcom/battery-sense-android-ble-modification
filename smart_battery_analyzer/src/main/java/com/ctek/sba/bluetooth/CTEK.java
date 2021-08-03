package com.ctek.sba.bluetooth;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ctek.sba.BuildConfig;
import com.ctek.sba.io.dao.DeviceRepository;
import com.ctek.sba.soc.SoCUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import greendao.Device;
import greendao.Voltage;

/**
 * Created by evgeny.akhundzhanov on 20.10.2016.
 */
public class CTEK {

  // public static final boolean bUseBluetoothLeManager = true;

  public final static String                            PACKAGE = "com.ctek.sba.bluetooth";

  public final static String ACTION_SERVICE_STOPPED   = PACKAGE + ".ACTION_SERVICE_STOPPED";

  public final static String ACTION_KEY_UNLOCK_FAIL   = PACKAGE + ".KEY_UNLOCK_FAIL";
  public final static String ACTION_KEY_UNLOCKED      = PACKAGE + ".KEY_UNLOCKED";

  public final static String EXTRA_DATA               = PACKAGE + ".EXTRA_DATA";
  public final static String EXTRA_BLE_DEVICE_ID      = PACKAGE + ".BLE_DEVICE_ID";
  public final static String EXTRA_BLE_DEVICE_ADDRESS = PACKAGE + ".BLE_DEVICE_ADDRESS";
  public final static String EXTRA_BLE_REASON         = PACKAGE + ".BLE_REASON";
  public final static String EXTRA_BLE_DEVICE_SERIAL  = PACKAGE + ".BLE_DEVICE_SERIAL";
  public final static String EXTRA_BLE_DEVICE_NAME    = PACKAGE + ".BLE_DEVICE_NAME";
  public final static String EXTRA_BLE_DEVICE_RSSI    = PACKAGE + ".BLE_DEVICE_RSSI";

  public final static String ACTION_DEVICE_UPDATED    = PACKAGE + ".VOLTAGE_HISTORY";
  public final static String ACTION_DEVICE_UPDATE_FAIL= PACKAGE + ".DEVICE_UPDATE_FAIL";

  public static final String EXTRA_CURSOR             = PACKAGE + ".CURSOR";
  /** double[] */
  public static final String EXTRA_VOLTAGE_ARRAY      = PACKAGE + ".VOLTAGE_ARRAY";
  public static final String EXTRA_GAP_IN_DATA_DETECTED = PACKAGE + ".RESET_DATA";

  public final static String ACTION_GET_SERIAL_FAIL   = PACKAGE + ".GET_SERIAL_FAIL";
  public static final String ACTION_SERIAL_RECEIVED   = PACKAGE + ".GET_SERIAL_SUCCESS";

  public static final String EXTRA_SERIAL_NUMBER      = PACKAGE + ".SERIAL_NUMBER";

  public final static String ACTION_LIVE_MODE         = PACKAGE + ".LIVE_MODE";
  public final static String EXTRA_BLE_LIVE_VOLTAGE   = PACKAGE + ".LIVE_VOLTAGE";
  public final static String EXTRA_BLE_LIVE_TEMPERATURE= PACKAGE + ".LIVE_TEMPERATURE";

  //Added
  public final static String ACTION_DEVICE_NOT_FOUND = PACKAGE + ".DEVICE_NOT_FOUND";

  private static final long INTERVAL_45_SEC_TIMEOUT_MSECS  = 45*1000L;
  // CBS-70 Change timeout for trying to connect to 10 seconds.
   public static final long INTERVAL_30_SEC_TIMEOUT_MSECS  = 30*1000L;
  private static final long INTERVAL_10_SEC_TIMEOUT_MSECS  = 10*1000L;
   public static final long INTERVAL_15_SEC_TIMEOUT_MSECS  = 15*1000L; // live mode 'no data' timeout

  // public final static int MODE_GET_TEMPERATURE      = 1;
  public final static int MODE_GET_BATTERY_LEVEL    = 2;
  public final static int MODE_GET_BATTERY_AND_TEMP = 3;

  public static final long getGattServiceTimeoutLIVE () {
    return INTERVAL_30_SEC_TIMEOUT_MSECS;  //Added 10 sec more 30 sec did not work on  samsung device
  }  //30 sec timeout

  public static final long getGattServiceTimeoutNORM () {
    // EA 05-Apr-2018. Timeout during updates occures. Roy has problems on Galaxy S7 Edge, etc.
    long msecs = INTERVAL_15_SEC_TIMEOUT_MSECS;  //Reduced overall timeout from 1.5 min to 15 sec
    return msecs;
  }


  public static final long getGattServiceTimeout (boolean bLiveMode) {
    return bLiveMode ? getGattServiceTimeoutLIVE() : getGattServiceTimeoutNORM();
  }

  static void sendBCOnDeviceDataReady (Context ctx, String mac, List<Double> voltages, int historyCursor, boolean bGapInData) {
    Intent intent = new Intent(ACTION_DEVICE_UPDATED);
    intent.putExtra(EXTRA_CURSOR, historyCursor);
    double[] voltagesArray = toPrimitiveArray(voltages);
    intent.putExtra(EXTRA_VOLTAGE_ARRAY, voltagesArray);
    intent.putExtra(EXTRA_BLE_DEVICE_ADDRESS, mac);
    intent.putExtra(EXTRA_GAP_IN_DATA_DETECTED, bGapInData);
    ctx.sendBroadcast(intent);
  }

  static void sendBCOnSerialNumberReceived(Context ctx, String mac, String serial) {
    Intent intent = new Intent(ACTION_SERIAL_RECEIVED);
    intent.putExtra(EXTRA_BLE_DEVICE_ADDRESS, mac);
    intent.putExtra(EXTRA_SERIAL_NUMBER, serial);
    ctx.sendBroadcast(intent);
  }

  static void broadcastUpdate (Context ctx, String mac, String action) {
    final Intent intent = new Intent(action);
    intent.putExtra(EXTRA_BLE_DEVICE_ADDRESS, mac);
    ctx.sendBroadcast(intent);
  }

  static void testbroadcastUpdate (Context ctx, String mac, String action, String mseg) {
    final Intent intent = new Intent(action);
    intent.putExtra(EXTRA_BLE_DEVICE_ADDRESS, mac);
    intent.putExtra("DATA", mseg);

    ctx.sendBroadcast(intent);
  }

  static void broadcastUpdateServiceStopped (Context ctx, String mac, String action, String reason) {
    final Intent intent = new Intent(action);
    intent.putExtra(EXTRA_BLE_DEVICE_ADDRESS, mac);
    intent.putExtra(EXTRA_BLE_REASON, reason);
    ctx.sendBroadcast(intent);
  }

  static void broadcastUpdate(Context ctx, String TAG, String action, Device device, BluetoothGattCharacteristic characteristic) {
    final Intent intent = new Intent(action);

    if (SBADevice.Characteristic.UPTIME_UUID.equals(characteristic.getUuid())) {
      if (ACTION_KEY_UNLOCKED.equals(action)) {
        intent.putExtra(EXTRA_BLE_DEVICE_ID, device.getId());
        intent.putExtra(EXTRA_BLE_DEVICE_ADDRESS, device.getAddress());
        intent.putExtra(EXTRA_BLE_DEVICE_SERIAL, device.getSerialnumber());
        intent.putExtra(EXTRA_BLE_DEVICE_NAME, device.getName());
        intent.putExtra(EXTRA_BLE_DEVICE_RSSI, device.getRssi());
      }
    } else if (SBADevice.Characteristic.KEY_HOLE_UUID.equals(characteristic.getUuid())) {
      Log.d(TAG, String.format("Key hole response: %s", new String(characteristic.getValue())));
    } else {
      // For all other profiles, writes the data formatted in HEX.
      final byte[] data = characteristic.getValue();
      if (data != null && data.length > 0) {
        intent.putExtra(EXTRA_DATA, new String(data));
      }
    }
    ctx.sendBroadcast(intent);
  }

  static void broadcastLiveVoltage (Context ctx, String mac, Double value) {
    Intent intent = new Intent(ACTION_LIVE_MODE);
    intent.putExtra(EXTRA_BLE_DEVICE_ADDRESS, mac);
    intent.putExtra(EXTRA_BLE_LIVE_VOLTAGE, value.doubleValue());
    ctx.sendBroadcast(intent);
    return;
  }

  static void broadcastLiveTemperature (Context ctx, String mac, Double value) {
    Intent intent = new Intent(ACTION_LIVE_MODE);
    intent.putExtra(EXTRA_BLE_DEVICE_ADDRESS, mac);
    intent.putExtra(EXTRA_BLE_LIVE_TEMPERATURE, value.doubleValue());
    ctx.sendBroadcast(intent);
    return;
  }

  static double[] toPrimitiveArray(List<Double> doubles) {
    double[] target = new double[doubles.size()];
    for (int i = 0; i < target.length; ++i) {
      target[i] = doubles.get(i);
    }
    return target;
  }

  static byte[] intToBytes(int theInt) {
    return ByteBuffer.allocate(4).putInt(theInt).array();
  }

  static byte[] getHistoryCommand (int start, int voltageCursor) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(bos);

    byte[] startBytes = intToBytes(start);

    int length = voltageCursor - start;
    byte[] lengthBytes = intToBytes(length);

    try {
      // dos.write(Integer.valueOf(MODE_GET_BATTERY_LEVEL));  // old
      dos.write(Integer.valueOf(MODE_GET_BATTERY_AND_TEMP));
      dos.write(startBytes[3]);
      dos.write(startBytes[2]);
      dos.write(lengthBytes[3]);
      dos.write(lengthBytes[2]);
      dos.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return bos.toByteArray();
  }

   /*
  static Double[] parseVoltagesArray (byte[] data) throws IllegalArgumentException {
    if (data.length % CTEK.MODE_GET_BATTERY_LEVEL != 0) throw new IllegalArgumentException("Data length should be even");

    Double[] doubleValues = new Double[data.length / 2];
    for (int i = 0; i < data.length; i += 2) {
      // java byte is signed so we need to convert to unsigned, & 0xFF does the trick
      doubleValues[i / 2] = ( (data[i] & 0xFF) + ((data[i+1] & 0xFF) * 256) ) / 2048d;
    }
    return doubleValues;
  }
  */
  // CBS-172 Android - Push battery data to back-end.
  static CTEKData parseVoltagesArray (int MODE_, byte[] data) throws IllegalArgumentException {
    return new CTEKData (MODE_, data);
  }

  private static List<Voltage> mergeVoltages(String TAG, List<Voltage> oldVoltages, double[] newVoltages, long deviceId, long currentTime, boolean gapDetected) {
    List<Voltage> result = new ArrayList<>();

    if (oldVoltages != null) {
      result.addAll(oldVoltages);
    }

    final long firstItemTimestamp;
    if (oldVoltages != null && oldVoltages.size() > 0 && !gapDetected) {
      firstItemTimestamp = getLast(oldVoltages).getTimestamp() + SBADevice.READ_PERIOD_MSECS;
    } else {
      firstItemTimestamp = currentTime - (newVoltages.length - 1) * SBADevice.READ_PERIOD_MSECS;
    }

    List<Voltage> newVoltagesList = SoCUtils.primitivesToVoltages(newVoltages, deviceId, firstItemTimestamp);
    result.addAll(newVoltagesList);

    int size = result.size();

    if (size > 30000)
    {
      Log.d(TAG, "Limit to 30000 records, size was = " + size);

      // Need to shrink
      result = result.subList(size-30000, size);
    }

    return result;
  }

  private static <T> T getLast(List<T> list) {
    if (list != null && !list.isEmpty()) {
      return list.get(list.size() - 1);
    } else {
      return null;
    }
  }

  static void storeData (Context context, String TAG, Device device, double[] newVoltages, int cursor, boolean gapDetected) {

    // Base for timestamp of new samples
    Long now = System.currentTimeMillis();

    Log.d(TAG, "cursor: " + cursor + ", voltages: " + Arrays.toString(newVoltages));

    List<Voltage> oldVoltages = device.getVoltageList("storeData");
    List<Voltage> resultVoltages = mergeVoltages(TAG, oldVoltages, newVoltages, device.getId(), now, gapDetected);

    if (gapDetected) {
      Log.w(TAG, "merging with gap");
    }
    Log.d(TAG, "voltages count old/new: " + oldVoltages.size() + " / " + resultVoltages.size());

    device.setVoltageList(resultVoltages);

    device.setReadCursor(cursor);
    if (newVoltages.length > 0) {
      // we rely on this field in update logic
      // so, update it only if new data has been received
      device.setUpdated(now);
    }
    DeviceRepository.insertOrUpdateWithVoltages(context, device);

    // Fire a notification in case we see a low lever.
    Notifications.setLevelNotification(context, device);
    // Trigger time based notifications (> 7 days)
    Notifications.setNewNotifications(context);
  }

  public static String getMillisFormatted (long msecs) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(msecs);
    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm:ss", Locale.US);
    return sdf.format(cal.getTime());
  }



  public static final Double MULT = 1000.0d;

  public static List<Double> packData (List<Double> volt_, List<Integer> temp_) {
    List<Double> list = new ArrayList<>();
    int ii, iiSize = volt_.size();
    for(ii=0;ii<iiSize;++ii) {
      list.add(temp_.get(ii) * MULT + volt_.get(ii));
    }
    return list;
  }

} // EOClass CTEK
