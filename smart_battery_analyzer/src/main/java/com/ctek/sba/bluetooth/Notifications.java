package com.ctek.sba.bluetooth;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.ctek.sba.R;
import com.ctek.sba.device.DeviceMap;
import com.ctek.sba.io.dao.DeviceRepository;
import com.ctek.sba.soc.SoCData;
import com.ctek.sba.soc.SoCUtils;
import com.ctek.sba.ui.DeviceDetailsActivity;
import com.ctek.sba.ui.EditDeviceActivity;

import java.util.List;

import greendao.Device;

import static com.ctek.sba.util.SettingsHelper.getNotificationData;
import static com.ctek.sba.util.SettingsHelper.getNotificationStatus;

public final class Notifications extends BroadcastReceiver {

  public static final String TAG = "Notifications";

  public static final String EXTRA_DEVICE_ID = "id";
  public static final String EXTRA_DEVICE_NAME = "name";

  private static final int NOTIFICATION_TYPE_NOT_SYNCED = 0;
  private static final int NOTIFICATION_TYPE_BATTERY_STATUS_CHANGED = 1;

  private static final String NOTIFICATION_NOT_SYNC_CHANNEL_ID = "NotSyncForLongTime";
  private static final String NOTIFICATION_STATUS_CHANGE_CHANNEL_ID = "BatteryStatusChange";


  private static int getHashCode4DeviceIdAndNotificationType (String strDeviceId, int NOTIFICATION_TYPE_) {
    int notificationId = (strDeviceId + "#" + NOTIFICATION_TYPE_).hashCode();
    return notificationId;
  }

  private static final long TIME_OVERDUE_MSECS = 1000*60*60*24*7; // 7 days in milliseconds
  // private static final long TIME_OVERDUE_MSECS = 1000*60*30;      // 30 minutes


  private static Intent getAlertIntent4Device (Context ctx, Device device) {
    Intent alertIntent = new Intent(ctx, Notifications.class);
    alertIntent.setAction("ACTION_FOR_DEVICE_ID_" + device.getId()); // see filterEquals
    alertIntent.putExtra(Notifications.EXTRA_DEVICE_ID, Long.toString(device.getId()));
    alertIntent.putExtra(Notifications.EXTRA_DEVICE_NAME, device.getName());
    return alertIntent;
  }

  private static void createAlert (Context ctx, Intent alert, Long fireTime) {
    PendingIntent pi_ = PendingIntent.getBroadcast(ctx, 0, alert, PendingIntent.FLAG_UPDATE_CURRENT);
    AlarmManager am_ = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
    am_.set(AlarmManager.RTC_WAKEUP, fireTime, pi_);
    return;
  }

  private static void deleteAlert (Context ctx, Intent alert) {
    PendingIntent pi_ = PendingIntent.getBroadcast(ctx, 0, alert, PendingIntent.FLAG_UPDATE_CURRENT);
    AlarmManager am_ = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
    am_.cancel(pi_);
    return;
  }

  public static void deleteAlert4Device (Context ctx, Device device) {
    deleteAlert(ctx, getAlertIntent4Device(ctx, device));
    return;
  }

  private static void createNotification (Context ctx, int notificationId, Notification notification) {
    NotificationManager nm_ = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
    nm_.notify(notificationId, notification);
    return;
  }

  private static void deleteNotification (Context ctx, int notificationId) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationManager notificationManager =
          (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
      notificationManager.deleteNotificationChannel(NOTIFICATION_NOT_SYNC_CHANNEL_ID);
      notificationManager.deleteNotificationChannel(NOTIFICATION_STATUS_CHANGE_CHANNEL_ID);

    } else {

      NotificationManager nm_ = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
      nm_.cancel(notificationId);
    }
    return;
  }

  public static void deletePendingNotifications4DeviceId (Context ctx, Long device_id) {
    String deviceID = String.valueOf(device_id);
    int NOTIFICATION_TYPE_ [] = { NOTIFICATION_TYPE_NOT_SYNCED, NOTIFICATION_TYPE_BATTERY_STATUS_CHANGED, };
    for(int ii=0;ii<NOTIFICATION_TYPE_.length;++ii) {
      int notificationId = getHashCode4DeviceIdAndNotificationType(deviceID, NOTIFICATION_TYPE_[ii]);
      deleteNotification(ctx, notificationId);
    }
    return;
  }

  //
  // This method will schedule new time based notifications. Calculation of
  // the number of active notifications is skipped since Android don't use that pattern.
  //
  public static void setNewNotifications(Context ctx) {
    // Only bother if selected by the user
    if (getNotificationData(ctx) == true) {
      // Get all devices
      List<Device> devices = DeviceRepository.getAllDevices(ctx);
      // Get current time
      Long now = System.currentTimeMillis();

      for (Device device : devices) {
        Long fireTime = null;
        if (device.getUpdated() != null) {
          Long elapsed = now - device.getUpdated();
          // Find devices not already older than 7 days (assuming their alerts have already fired)
          if (elapsed < TIME_OVERDUE_MSECS) {
            // Create notifications and schedule to fire after 7 days
            // Long fireTime = now + TIME_OVERDUE_MSECS; // CBS-64 'No data since 7 days' works incorrectly.
            Log.d(TAG, "setNewNotifications: device_id = " + device.getId() + " updated: " + CTEK.getMillisFormatted(device.getUpdated()));
            fireTime = device.getUpdated() + TIME_OVERDUE_MSECS;
          }
          else {
            Log.d(TAG, "setNewNotifications: device_id = " + device.getId() + " skipped.");
          }
        }
        else {
          long msecsAdded = DeviceMap.getInstance().getDeviceTimestamp(device.getId());
          Log.d(TAG, "setNewNotifications: device_id = " + device.getId() + " added: " + CTEK.getMillisFormatted(msecsAdded));
          if(msecsAdded > 0) {
            fireTime = msecsAdded + TIME_OVERDUE_MSECS;
          }
        }
        if(fireTime!=null) {
          createAlert(ctx, getAlertIntent4Device(ctx, device), fireTime);
          Log.d(TAG, "setNewNotifications: Alert created device_id = " + device.getId() + " fireTime = " + CTEK.getMillisFormatted(fireTime));
        }
      }
    }
  }

  public static void removePendingNotifications(Context ctx) {
    // Ensure they are deselected by the user
    if (getNotificationData(ctx) == false) {
      // Get all device
      List<Device> devices = DeviceRepository.getAllDevices(ctx);
      // Get current time
      Long now = System.currentTimeMillis();

      for (Device device : devices) {
        if (device.getUpdated() != null) {
          String deviceID = Long.toString(device.getId());
          Long elapsed = now - device.getUpdated();
          // Remove potentially pending notifications
          if (elapsed < TIME_OVERDUE_MSECS) {
            deleteAlert(ctx, getAlertIntent4Device(ctx, device));
          }
          // And now get rid of any already fired notifications (build ID in the same way as below).
          int notificationId = getHashCode4DeviceIdAndNotificationType(deviceID, NOTIFICATION_TYPE_NOT_SYNCED); // (deviceID + "#" + NOTIFICATION_TYPE_NOT_SYNCED).hashCode();
          deleteNotification(ctx, notificationId);
        }
      }
    }
  }

  //
  // Receiver for time based alarms/alerts
  @Override
  public void onReceive(Context ctx, Intent intent) {
    String deviceID = intent.getStringExtra(Notifications.EXTRA_DEVICE_ID);
    String deviceName = intent.getStringExtra(Notifications.EXTRA_DEVICE_NAME);

    // Protect agains any of the parameters being null
    if (deviceID != null && deviceName != null) {
      showBatteryHasNotBeenSyncedForALongTimeNotification(ctx, deviceID, deviceName);
    }
  }

  private static Intent getStartDetailsActivityIntent4Device (Context ctx, long device_id) {
    Intent i_ = new Intent(ctx, DeviceDetailsActivity.class);
    i_.setAction(Intent.ACTION_VIEW);
    i_.putExtra(EditDeviceActivity.EXTRA_DEVICE_ID, device_id);
    i_.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    return i_;
  }

  //
  // Create and fire the time based notification
  //
  public static void showBatteryHasNotBeenSyncedForALongTimeNotification(Context ctx, String deviceID, String deviceName) {

    Long device_id = Long.parseLong(deviceID);
    Device device = DeviceRepository.getDeviceForId(ctx, device_id);
    if(device==null) return;

    Long lastUpdated = device.getUpdated();
    lastUpdated = (lastUpdated!=null) ? lastUpdated : 0;
    long updatedTimeWhenNotified_NOT_SYNCED = DeviceMap.getInstance().getUpdatedTimeWhenNotified_NOT_SYNCED(device_id);
    if(updatedTimeWhenNotified_NOT_SYNCED==lastUpdated) {
      // already notified
      Log.d(TAG, "NOTIFICATION_TYPE_NOT_SYNCED skipped. device_id = " + device_id + " lastUpdated = " + CTEK.getMillisFormatted(lastUpdated));
      return;
    }

    Resources resources = ctx.getResources();
    String title = resources.getString(R.string.notification_title);
    String message = resources.getString(R.string.notification_message);

    // TODO: Create complete message. Oops - becomes too long. Did not get big message to work...
    // String vehicle = resources.getString(R.string.vehicle);
    // message = vehicle + " " + deviceName + ": " + message;
    int notificationId = getHashCode4DeviceIdAndNotificationType(deviceID, NOTIFICATION_TYPE_NOT_SYNCED); // (deviceID + "#" + NOTIFICATION_TYPE_NOT_SYNCED).hashCode();


    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

      int importance = NotificationManager.IMPORTANCE_DEFAULT;
      // Where to go when user clicks the notification
      Intent onClickIntent = getStartDetailsActivityIntent4Device(ctx, device_id);

      // Create ID for this notification (can be used to cancel the notification)
      PendingIntent contentIntent = PendingIntent.getActivity(ctx, notificationId, onClickIntent, PendingIntent.FLAG_ONE_SHOT);

      NotificationChannel channel = new NotificationChannel(NOTIFICATION_NOT_SYNC_CHANNEL_ID, title , importance);
      // Register the channel with the system; you can't change the importance
      // or other notification behaviors after this
      NotificationManager notificationManager = ctx.getSystemService(NotificationManager.class);
      notificationManager.createNotificationChannel(channel);

      NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, NOTIFICATION_NOT_SYNC_CHANNEL_ID);

      builder.setAutoCancel(false);
      builder.setTicker(title);
      builder.setContentTitle(title);
      builder.setContentText(message);
      builder.setSmallIcon(R.drawable.ic_notification);
      builder.setContentIntent(contentIntent);

      builder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND);
      // b_.setNumber(100);
      builder.build();

      notificationManager.notify(notificationId, builder.build());

    }
    else {


      // Where to go when user clicks the notification
      Intent onClickIntent = getStartDetailsActivityIntent4Device(ctx, device_id);

      // Create ID for this notification (can be used to cancel the notification)

      PendingIntent contentIntent = PendingIntent.getActivity(ctx, notificationId, onClickIntent, PendingIntent.FLAG_ONE_SHOT);

      Notification notification = new NotificationCompat.Builder(ctx)
          .setContentTitle(title)
          .setContentText(message)
          .setTicker(title)
          .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
          .setSmallIcon(R.drawable.ic_notification)
          .setContentIntent(contentIntent)
          // DID NOT GET THIS TO WORK!
          //        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
          .build();

      createNotification(ctx, notificationId, notification);
      DeviceMap.getInstance().setUpdatedTimeWhenNotified_NOT_SYNCED(device_id, lastUpdated);
      Log.d(TAG, "NOTIFICATION_TYPE_NOT_SYNCED created. device_id = " + device_id + " lastUpdated = " + CTEK.getMillisFormatted(lastUpdated));
    }
    return;
  }

  //
  // Create the battery status change notifications
  //
  public static void setLevelNotification(Context ctx, Device device) {
    if (getNotificationStatus(ctx) == true) {

      SoCData.StateFlag oldFlag = SoCData.integer2Flag(device.getIndicatorColor());
      // Assume battery was ok at first
      if (oldFlag != SoCData.StateFlag.FLAG_NONE) {
        oldFlag = SoCData.StateFlag.FLAG_GREEN;
      }

      SoCData soc = SoCUtils.getLatestSocValue(device);
      //Added
      //BluetoothLeManager.getInstance(ctx).startBTSearch();
      ///
      if (soc != null) {
        SoCData.StateFlag newFlag = SoCData.getFlag4Value(soc.d_estSoC);

        if (oldFlag != newFlag) {
          if ((newFlag == SoCData.StateFlag.FLAG_RED) ||
            ((newFlag == SoCData.StateFlag.FLAG_YELLOW) && (oldFlag != SoCData.StateFlag.FLAG_RED)) ) {
            // Notify if wew get to Red or Yellow except transition from Red to Yellow
            showBatteryStatusChangeNotification(ctx, device, newFlag);
          }
          // Remove when green. No! User can remove manually.
          device.setIndicatorColor(newFlag.ordinal());
        }
      }
      else {
        device.setIndicatorColor(oldFlag.ordinal()); // EA 06-Oct-2016. Crash fixed.
      }

      // Alerts used for test.
      //    showBatteryStatusChangeNotification(ctx, device, Calculate.State.Red);
      //    showBatteryHasNotBeenSyncedForALongTimeNotification(ctx, "1", "Fiat");
    }
  }

  //
  // Create the battery status change notifications
  //
  public static void showBatteryStatusChangeNotification(Context ctx, Device device, SoCData.StateFlag newFlag) {
    Resources resources = ctx.getResources();

    String title;
    String name = device.getName();
    String message = name + ": ";

    if (newFlag == SoCData.StateFlag.FLAG_YELLOW) {
      title = resources.getString(R.string.reminder_title);
      message += resources.getString(R.string.reminder_message);
    }
    else if (newFlag == SoCData.StateFlag.FLAG_RED) {
      title = resources.getString(R.string.warning_title);
      message += resources.getString(R.string.warning_message);
    }
    else {
      return;
    }
    showBatteryStatusChangeNotificationMessage(ctx, device, title, message);
  }

  public static void showBatteryStatusChangeNotificationMessage(Context ctx, Device device, String title, String message) {
    Long device_id = device.getId();

    Long lastUpdated = device.getUpdated();
    lastUpdated = (lastUpdated!=null) ? lastUpdated : 0;
    long updatedTimeWhenNotified_BAT_STATUS = DeviceMap.getInstance().getUpdatedTimeWhenNotified_BAT_STATUS(device_id);
    if(updatedTimeWhenNotified_BAT_STATUS==lastUpdated) {
      // already notified
      return;
    }

    int notificationId = getHashCode4DeviceIdAndNotificationType(Long.toString(device_id), NOTIFICATION_TYPE_BATTERY_STATUS_CHANGED); // (Long.toString(device.getId()) + "#" + NOTIFICATION_TYPE_BATTERY_STATUS_CHANGED).hashCode();




    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

      int importance = NotificationManager.IMPORTANCE_DEFAULT;

      Intent onClickIntent = getStartDetailsActivityIntent4Device(ctx, device.getId());
      PendingIntent contentIntent = PendingIntent.getActivity(ctx, notificationId, onClickIntent, PendingIntent.FLAG_ONE_SHOT);


      NotificationChannel channel = new NotificationChannel(NOTIFICATION_STATUS_CHANGE_CHANNEL_ID, title , importance);
      // Register the channel with the system; you can't change the importance
      // or other notification behaviors after this
      NotificationManager notificationManager = ctx.getSystemService(NotificationManager.class);
      notificationManager.createNotificationChannel(channel);

      NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, NOTIFICATION_STATUS_CHANGE_CHANNEL_ID);

      builder.setAutoCancel(false);
      builder.setTicker(title);
      builder.setContentTitle(title);
      builder.setContentText(message);
      builder.setSmallIcon(R.drawable.ic_notification);
      builder.setContentIntent(contentIntent);
      builder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND);


      notificationManager.notify(notificationId, builder.build());

    }
    else {


      Intent onClickIntent = getStartDetailsActivityIntent4Device(ctx, device.getId());
      PendingIntent contentIntent = PendingIntent.getActivity(ctx, notificationId, onClickIntent, PendingIntent.FLAG_ONE_SHOT);

      Notification notification = new Notification.Builder(ctx)
          .setContentTitle(title)
          .setContentText(message)
          .setSmallIcon(R.drawable.ic_notification)
          .setTicker(title)
          .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
          .setContentIntent(contentIntent)
          .setStyle(new Notification.BigTextStyle().bigText(message))
          .build();

      createNotification(ctx, notificationId, notification);
      DeviceMap.getInstance().setUpdatedTimeWhenNotified_BAT_STATUS(device_id, lastUpdated);

    }
    return;
  }

  public static void removeLevelNotification(Context ctx) {
    // Ensure they are deselected b the user
    if (!getNotificationStatus(ctx)) {
      // Get all device
      List<Device> devices = DeviceRepository.getAllDevices(ctx);

      for (Device device : devices) {
        if (device.getUpdated() != null) {
          String deviceID = Long.toString(device.getId());

          // Get rid of any already fired notifications (build ID in the same way as below.
          int notificationId = getHashCode4DeviceIdAndNotificationType(deviceID, NOTIFICATION_TYPE_BATTERY_STATUS_CHANGED); // (deviceID + "#" + NOTIFICATION_TYPE_BATTERY_STATUS_CHANGED).hashCode();
          deleteNotification(ctx, notificationId);
        }
      }
    }
  }

} // EOClass Notifications
