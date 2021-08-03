package com.ctek.sba.ui.support;

import android.Manifest;
import android.app.Activity;

import com.ctek.sba.R;

/**
 * Created by evgeny.akhundzhanov on 06.10.2016.
 */
public class MarshMallowPermissionImages extends MarshMallowPermission {

  public MarshMallowPermissionImages (Activity activity) {
    super(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.hint_permission_images, REQUEST_CODE_IMAGES);
  }

} // EOClass MarshMallowPermissionImages
