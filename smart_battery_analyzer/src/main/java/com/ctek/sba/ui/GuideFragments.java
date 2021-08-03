package com.ctek.sba.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by mfahlen on 2016-07-07.
 */

// Class used for the guide  fragments in NewDevicePager.
public class GuideFragments extends Fragment {

  private int resourceID;

  // newInstance constructor for creating fragment with arguments
  public static GuideFragments newInstance(int id) {
    GuideFragments fragmentFirst = new GuideFragments();
    Bundle args = new Bundle();
    args.putInt("resourceID", id);
    fragmentFirst.setArguments(args);
    return fragmentFirst;
  }

  // Store instance variables based on arguments passed
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    resourceID = getArguments().getInt("resourceID", 0);
  }

  // Inflate the view for the fragment based on layout XML
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(resourceID, container, false);
    return view;
  }
}
