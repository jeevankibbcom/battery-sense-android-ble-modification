package com.ctek.sba.util;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

public class PermissionCheck {

  public  static final int CTEK_CAMERA_PERMISSION_CODE = 100;


  //This method can check the camera permission and return true if permission granted else return false
  //And shows camera permission popup on behalf of respective activiy, Results of permission requests should be handled from the Activity
  public static boolean isCameraPermissionGranted(Activity requestActivity) {
    if (ActivityCompat.checkSelfPermission(requestActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
      return true;
    } else {
      ActivityCompat.requestPermissions(requestActivity, new
          String[]{Manifest.permission.CAMERA}, CTEK_CAMERA_PERMISSION_CODE);
      return false;
    }

  }

}
