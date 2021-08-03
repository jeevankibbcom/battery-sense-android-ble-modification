package com.ctek.sba.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.ctek.sba.R;

/**
 * Provides fragments to the guide pages.
 */

public class NewDevicePager extends FragmentStatePagerAdapter {
  private static int NUM_ITEMS = 4;

  public NewDevicePager(FragmentManager fragmentManager) {
    super(fragmentManager);
  }

  // Returns total number of pages
  @Override
  public int getCount() {
    return NUM_ITEMS;
  }

  public boolean isDeviceCodePage(int pageNr) {
    return 3 == pageNr;
  }

  // Returns the fragment to display for that page
  @Override
  public Fragment getItem(int position) {
    switch (position) {
      case 0:
        return GuideFragments.newInstance(R.layout.fragment_add_device_step_introduction);
      case 1:
        return GuideFragments.newInstance(R.layout.fragment_add_device_step_find_device_id);
      case 2:
        return GuideFragments.newInstance(R.layout.fragment_add_device_step_connect_cables);
      case 3:
        return GuideFragments.newInstance(R.layout.fragment_add_device_step_device_code);
      default:
        return null;
    }
  }
}
