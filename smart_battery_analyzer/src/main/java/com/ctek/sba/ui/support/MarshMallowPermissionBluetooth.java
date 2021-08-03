package com.ctek.sba.ui.support;

import android.Manifest;
import android.app.Activity;

import com.ctek.sba.R;

/**
 * Created by evgeny.akhundzhanov on 06.10.2016.
 */
public class MarshMallowPermissionBluetooth extends MarshMallowPermission {

  public MarshMallowPermissionBluetooth (Activity activity) {
    super(activity, Manifest.permission.BLUETOOTH, R.string.hint_permission_bluetooth, REQUEST_CODE_BLUETOOTH);
  }

} // EOClass MarshMallowPermissionBluetooth
