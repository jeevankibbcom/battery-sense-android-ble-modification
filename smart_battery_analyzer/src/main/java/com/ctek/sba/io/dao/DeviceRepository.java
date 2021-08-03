package com.ctek.sba.io.dao;

import android.content.Context;
import android.util.Log;

import com.ctek.sba.BuildConfig;
import com.ctek.sba.R;
import com.ctek.sba.application.CtekApplication;
import com.ctek.sba.rest.SrvPostSocs;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import de.greenrobot.dao.query.CountQuery;
import de.greenrobot.dao.query.DeleteQuery;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.event.EventBus;
import greendao.Device;
import greendao.DeviceDao;
import greendao.Voltage;
import greendao.VoltageDao;

public class DeviceRepository {

  private static final String TAG = DeviceRepository.class.getSimpleName();

  public static class DeviceUpdatedEvent {

    public final long deviceId;

    public DeviceUpdatedEvent(long deviceId) {
      this.deviceId = deviceId;
    }

  } // EOClass

  private static int getStoreDataIntervalInDays () {
    return BuildConfig.DEBUG ? 7 : 180;
  }

  public static long insertOrUpdate(Context context, Device device) {
    long deviceId = getDeviceDao(context).insertOrReplace(device);
    EventBus.getDefault().post(new DeviceUpdatedEvent(deviceId));
    return deviceId;
  }


  public static long insertOrUpdateWithVoltages(Context context, Device device) {
    long deviceId = getDeviceDao(context).insertOrReplace(device);

    if (device.getVoltageList("insertOrUpdateWithVoltages 1") != null) {
      getVoltageDao(context).insertOrReplaceInTx(device.getVoltageList("insertOrUpdateWithVoltages 2"));
    }

    performCleanUp(context, device);

    String reason = "insertOrUpdateWithVoltages - new data.";
    SrvPostSocs.start(context, reason, SrvPostSocs.MODE_HISTORY_DATA);

    EventBus.getDefault().post(new DeviceUpdatedEvent(deviceId));
    device.getVoltageList("insertOrUpdateWithVoltages 3"); // LOG
    return deviceId;
  }

  public static void performCleanUp (Context context) {
    List<Device> devices = getAllDevices(context);
    int kk, kkSize = devices!=null ? devices.size(): 0;
    Log.d(TAG, "Devices = " + kkSize);
    for(kk=0;kk<kkSize;++kk) {
      Device device = devices.get(kk);
      // long count = countVoltages(context, device.getId());
      // Log.d(TAG, "Device " + (kk+1) + "/. " + device.getName() + " id = " + device.getId() + " - BEFORE CLEANUP. count = " + count);
      performCleanUp(context, device);
    }

    cleanDaoSession(context);

    /*
    for(kk=0;kk<kkSize;++kk) {
      Device device = devices.get(kk);
      long count = countVoltages(context, device.getId());
      Log.d(TAG, "Device " + (kk+1) + "/. " + device.getName() + " id = " + device.getId() + " - AFTER CLEANUP. count = " + count);
    }
    */
    return;
  }

  public static void performCleanUp (Context context, Device device) {
    long deviceId = device.getId();

    Calendar cal = getMidnightNDaysAgo(getStoreDataIntervalInDays());
    QueryBuilder<Voltage> queryBuilder = getVoltageDao(context).queryBuilder();
    queryBuilder.where(VoltageDao.Properties.DeviceId.eq(deviceId), VoltageDao.Properties.Timestamp.le(cal.getTimeInMillis()));
    // Build the query
    DeleteQuery<Voltage> query = queryBuilder.buildDelete();
    // Run query
    query.executeDeleteWithoutDetachingEntities();
    return;
  }

  public static Calendar getMidnightNDaysAgo (int nDays) {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    Long midnightMsecs = cal.getTimeInMillis();
    SimpleDateFormat sdf1 = new SimpleDateFormat("dd MMM HH:mm:ss");
    Log.d(TAG, "midnightMsecs = " + midnightMsecs + " - " + sdf1.format(midnightMsecs));
    if(nDays!=0) {
      cal.add(Calendar.DAY_OF_MONTH, -nDays);
      midnightMsecs = cal.getTimeInMillis();
      Log.d(TAG, "nDaysAgo = " + nDays);
      Log.d(TAG, "midnightMsecs = " + midnightMsecs + " - " + sdf1.format(midnightMsecs));
    }
    return cal;
  }
  /*
  public static void clearDevices(Context context) {
    getDeviceDao(context).deleteAll();
  }
  */

  public static long countVoltages (Context context, long device_id) {
    QueryBuilder<Voltage> queryBuilder = getVoltageDao(context).queryBuilder();
    queryBuilder.where(VoltageDao.Properties.DeviceId.eq(device_id));
    CountQuery<Voltage> query = queryBuilder.buildCount();
    long count = query.count();
    Log.d(TAG, "countVoltages: device_id = " + device_id + " count = " + count);
    return count;
  }

  public static long countVoltagesAll (Context context) {
    QueryBuilder<Voltage> queryBuilder = getVoltageDao(context).queryBuilder();
    queryBuilder.where(VoltageDao.Properties.DeviceId.gt(0));
    CountQuery<Voltage> query = queryBuilder.buildCount();
    long count = query.count();
    Log.d(TAG, "countVoltages: ALL devices. count = " + count);
    return count;
  }



  public static void deleteDeviceWithId(Context context, long id) {

    // countVoltagesAll(context);
    // long count1 = countVoltages(context, id);

    // Create query to remove voltage data from the device.
    QueryBuilder<Voltage> queryBuilder = getVoltageDao(context).queryBuilder();
    queryBuilder.where(VoltageDao.Properties.DeviceId.eq(id));
    // Build the query
    DeleteQuery<Voltage> query = queryBuilder.buildDelete();
    // Run query
    query.executeDeleteWithoutDetachingEntities();

    // Delete the device.
    getDeviceDao(context).delete(getDeviceForId(context, id));

    // countVoltagesAll(context);
    // long count2 = countVoltages(context, id);
    // Log.d(TAG, "deleteDeviceWithId: count1 = " + count1 + " count2 = " + count2);

    cleanDaoSession(context);
    return;
  }

  public static Device getDeviceForId(Context context, long id) {
    return getDeviceDao(context).load(id);
  }

  public static Device getDeviceForAddress(Context context, String address) {
    List<Device> allDevices = getAllDevices(context);
    for (Device device : allDevices) {
      if (device.getAddress().equals(address)) {
        return device;
      }
    }
    return null;
  }

  public static Long getDeviceIdForAddress(Context context, String address) {
    List<Device> allDevices = getAllDevices(context);
    for (Device device : allDevices) {
      if (device.getAddress().equals(address)) {
        return device.getId();
      }
    }
    return null;
  }

  public static Device getDeviceForSerialNumber(Context context, String serial) {
    List<Device> allDevices = getAllDevices(context);
    for (Device d : allDevices) {
      if (d.getSerialnumber().equals(serial)) {
        return d;
      }
    }
    return null;
  }

  public static List<Device> getAllDevices(Context context) {
    return getDeviceDao(context).loadAll();
  }

  private static DeviceDao getDeviceDao(Context ctx) {
    CtekApplication theApp = (CtekApplication) ctx.getApplicationContext();
    return theApp.getDaoSession().getDeviceDao();
  }

  private static VoltageDao getVoltageDao(Context ctx) {
    CtekApplication theApp = (CtekApplication) ctx.getApplicationContext();
    return theApp.getDaoSession().getVoltageDao();
  }

  private static void cleanDaoSession (Context ctx) {
    CtekApplication theApp = (CtekApplication) ctx.getApplicationContext();
    theApp.getDaoSession().clear();
    theApp.recreateDaoSession();
    return;
  }

} // EOClass DeviceRepository
