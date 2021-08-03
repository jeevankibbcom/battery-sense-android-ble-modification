package com.ctek.sba.ui.support;

import android.Manifest;
import android.app.Activity;

import com.ctek.sba.R;

/**
 * Created by evgeny.akhundzhanov on 06.10.2016.
 */
public class MarshMallowPermissionBluetoothAdmin extends MarshMallowPermission {

  public MarshMallowPermissionBluetoothAdmin (Activity activity) {
    super(activity, Manifest.permission.BLUETOOTH_ADMIN, R.string.hint_permission_bluetooth, REQUEST_CODE_BLUETOOTH_ADMIN);
  }

} // EOClass MarshMallowPermissionBluetoothAdmin
