package com.ctek.sba.ui.support;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by evgeny.akhundzhanov on 06.10.2016.
 */
public class MarshMallowPermission {

  public static final int REQUEST_CODE_LOCATION   = 1;
  public static final int REQUEST_CODE_IMAGES     = 2;
  public static final int REQUEST_CODE_CAMERA     = 3;
  public static final int REQUEST_CODE_BLUETOOTH   = 4;
  public static final int REQUEST_CODE_BLUETOOTH_ADMIN = 5;
  public static final int REQUEST_CODE_PERMISSIONS  = 9;



  private Activity  activity;
  // private String    permission;   // Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, etc.
  private List<String> permissions;
  private int       iResIdExplanation;
  private int       request_code;

  public MarshMallowPermission(Activity activity, String permission, int iResIdExplanation, int request_code) {
    this.activity = activity;
    this.permissions = new ArrayList<>();
    permissions.add(permission);
    this.iResIdExplanation = iResIdExplanation;
    this.request_code = request_code;
  }

  public MarshMallowPermission(Activity activity, String[] array, int iResIdExplanation, int request_code) {
    this.activity = activity;
    this.permissions = new ArrayList<>();
    for(String permission : array) {
      permissions.add(permission);
    }
    this.iResIdExplanation = iResIdExplanation;
    this.request_code = request_code;
  }

  public MarshMallowPermission addPermission (String permission) {
    permissions.add(permission);
    return this;
  }


  public boolean check (){
    for(String permission : permissions) {
      int result = ContextCompat.checkSelfPermission(activity, permission);
      if(result != PackageManager.PERMISSION_GRANTED)
        return false;
    }
    return true;
  }

  public void request (){
    if(iResIdExplanation!=0) {
      boolean bShowExplanation = false;
      for(String permission : permissions) {
        if(ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
          bShowExplanation = true;
          break;
        }
      }
      if(bShowExplanation) {
        String explanation = activity.getString(iResIdExplanation);
        Toast.makeText(activity, explanation, Toast.LENGTH_LONG).show();
      }
    }
    else {
      // do nothing
    }

    ActivityCompat.requestPermissions(activity, permissions.toArray(new String[0]), request_code);
    return;
  }

} // EOClass MarshMallowPermission
