package com.ctek.sba.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.ctek.sba.R;
import com.ctek.sba.ui.SplashScreenActivity;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by evgeny.akhundzhanov on 27.10.2016.
 */
public class Notifier {

  private static final int NOTIFICATION_BLE = 101;
  private static final String NOTIFICATION_CHANNEL_ID = "BatterUpdate";

  public static Intent getLaunchIntent (Context ctx) {
    Intent launcher = new Intent(Intent.ACTION_MAIN);
    launcher.addCategory(Intent.CATEGORY_LAUNCHER);
    String packageName = ctx.getPackageName();
    String mainActivity = packageName +".ui."+ SplashScreenActivity.class.getSimpleName();
    launcher.setComponent(ComponentName.unflattenFromString(packageName + "/" + mainActivity));
    return launcher;
  }


  protected static void cancelNotification (Service srv_) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

      NotificationManager notificationManager =
          (NotificationManager) srv_.getSystemService(Context.NOTIFICATION_SERVICE);
      notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID);
      notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID);

    }
    else {
      NotificationManager nm = (NotificationManager) srv_.getSystemService(Context.NOTIFICATION_SERVICE);
      nm.cancel(NOTIFICATION_BLE);
    }

  }

  protected static void notifyServiceRunning (Service srv_, String titl, String text, String subText) {

    Context ctx = srv_.getApplicationContext();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

      int importance = NotificationManager.IMPORTANCE_DEFAULT;
      NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, titl, importance);
      channel.setDescription(text);
      // Register the channel with the system; you can't change the importance
      // or other notification behaviors after this
      NotificationManager notificationManager = srv_.getSystemService(NotificationManager.class);
      notificationManager.createNotificationChannel(channel);



      PendingIntent pi_ = PendingIntent.getActivity(ctx, 0, getLaunchIntent(ctx), PendingIntent.FLAG_UPDATE_CURRENT);

      NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL_ID);

      builder.setAutoCancel(false);
      builder.setTicker(text);
      builder.setContentTitle(titl);
      builder.setContentText(text);
      builder.setSmallIcon(R.drawable.ic_notification);
      builder.setContentIntent(pi_);
      builder.setOngoing(true);

      builder.setSubText(subText);
      // b_.setNumber(100);
      builder.build();

      notificationManager.notify(NOTIFICATION_BLE, builder.build());


    }
    else {


      Notification notification = createNotificationSmall(ctx, titl, text, subText);
      // Notification notification = createNotificationLarge(ctx, titl, text, subText, -1);

      NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
      nm.notify(NOTIFICATION_BLE, notification);

      srv_.startForeground(NOTIFICATION_BLE, notification);

    }
    return;
  }

  private static Notification createNotificationSmall (Context ctx, String titl, String text, String subText) {

    PendingIntent pi_ = PendingIntent.getActivity(ctx, 0, getLaunchIntent(ctx), PendingIntent.FLAG_UPDATE_CURRENT);

    Notification.Builder b_ = new Notification.Builder(ctx);

    b_.setAutoCancel(false);
    b_.setTicker(text);
    b_.setContentTitle(titl);
    b_.setContentText(text);
    b_.setSmallIcon(R.drawable.ic_notification);
    b_.setContentIntent(pi_);
    b_.setOngoing(true);

    b_.setSubText(subText);
    // b_.setNumber(100);
    b_.build();

    Notification not_ = b_.getNotification();
    not_.flags |= Notification.FLAG_ONGOING_EVENT;
    not_.flags |= Notification.FLAG_NO_CLEAR;
    return not_;
  }

  /*
  // icon_service_running is not confirmed by customer.
  private static Notification createNotificationLarge (Context ctx, String titl, String text, String subText, int number) {
    // Create the style object with BigPictureStyle subclass.
    NotificationCompat.BigPictureStyle notiStyle = new NotificationCompat.BigPictureStyle();
    notiStyle.setBigContentTitle(titl);
    notiStyle.setSummaryText(subText);
    notiStyle.bigPicture(BitmapFactory.decodeResource(ctx.getResources(), R.drawable.icon_service_running)); // 2:1 ratio image is required.


    // start activity intent
    PendingIntent pi = PendingIntent.getActivity(ctx, 0, getLaunchIntent(ctx), PendingIntent.FLAG_UPDATE_CURRENT);
    NotificationCompat.Builder b_ = new NotificationCompat.Builder(ctx)
        .setSmallIcon(R.drawable.ic_launch)
        .setAutoCancel(true)
        .setContentIntent(pi)
        .setContentTitle(titl)
        .setContentText (text)
        // .setDeleteIntent(RcvWakeup.getPIDeleteDROP(ctx))
        .setPriority(Notification.PRIORITY_MAX)	// requires minSdkVersion="16"
        .setStyle(notiStyle)
        .setOngoing(true)
        ;
    if(number > 1) {
      b_.setNumber(number);
    }

    Notification not_ = b_.build();
    not_.flags |= Notification.FLAG_ONGOING_EVENT;
    not_.flags |= Notification.FLAG_NO_CLEAR;
    return not_;
  }
  */

} // EOClass Notifier
