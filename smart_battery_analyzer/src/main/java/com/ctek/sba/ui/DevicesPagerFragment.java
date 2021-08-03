package com.ctek.sba.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioGroup;

import java.util.List;

import greendao.Device;

import com.ctek.sba.R;
import com.ctek.sba.bluetooth.BluetoothLeManager;
import com.ctek.sba.bluetooth.DeviceListUI;
import com.ctek.sba.bluetooth.DeviceManagerHiQ;
import com.ctek.sba.io.dao.DeviceRepository;
import com.ctek.sba.rest.SrvPostSocs;
import com.ctek.sba.util.BluetoothHelper;


public class DevicesPagerFragment extends Fragment {

  private static final String TAG = DevicesPagerFragment.class.getName();

  private ViewPager mPager;
  private DevicePagerAdapter mPagerAdapter;
  private RadioGroup mPageIndicator;
  private View mShowDetailsButton;
  private DeviceListUI devices;

  private Context mContext;

private SwipeRefreshLayout swipeView;

  private SrvPostSocs mSrvPostSocs = new SrvPostSocs();

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mContext = getActivity();
    final View rootView = inflater.inflate(R.layout.fragment_device_pager, container, false);

   // swipeView = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
//    if (swipeView != null) {
//      swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//        @Override
//        public void onRefresh() {
//          if (BluetoothHelper.getInstance().checkBluetoothAdapterActive(mContext, BluetoothHelper.SHOW_DIALOG_YES)) {
//            if (devices != null) {
////              //Added
////              DeviceDetailsActivity.connectionStatus.setText("");
////              ///
//            //  devices.cleanUpdatedNow();
//            //  DeviceManagerHiQ.getInstance().refreshAllDevices("Pull2Refresh"); //Orhinally Present to refresh on main page
//            }
//            (new Handler()).postDelayed(new Runnable() {
//              @Override
//              public void run() {
//                swipeView.setRefreshing(false);
//              }
//            }, 1000);
//          }
//          else {
//            swipeView.setRefreshing(false);
//          }
//        }
//      });
//    }

    // Instantiate a ViewPager and a PagerAdapter.
    mPager = (ViewPager) rootView.findViewById(R.id.pager);
    mPagerAdapter = new DevicePagerAdapter(getChildFragmentManager());
    mPager.setAdapter(mPagerAdapter);

    // Page indicator
    mPageIndicator = (RadioGroup) rootView.findViewById(R.id.onboarding_page_indicator_radio_group);

    mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      public void onPageSelected(int i) {
        updatePageIndicator();
      }
    });

    mShowDetailsButton = rootView.findViewById(R.id.show_details_button);
    mShowDetailsButton.setOnClickListener(onShowDetailsClickListener);

    return rootView;
  }

  /*
  public void setDeviceList(List<Device> deviceList) {
    devices = new DeviceListUI(deviceList);
    updateUi();
  }
  */
  private void regetDevices () {
    List<Device> deviceList = DeviceRepository.getAllDevices(getActivity());
    int kk, kkSize = deviceList!=null ? deviceList.size(): 0;
    Log.d(TAG, "Devices = " + kkSize);
    for(kk=0;kk<kkSize;++kk) {
      Device device = deviceList.get(kk);
      Log.d(TAG, "Device " + (kk+1) + "/. " + device.getName() + " id = " + device.getId() + " serial = " + device.getSerialnumber() + " address = " + device.getAddress());
    }
    devices = new DeviceListUI(deviceList);
    return;
  }

  @Override
  public void onResume() {
    super.onResume();
    updateUi();
//    DeviceManagerHiQ.getInstance().resendUpdateStates();
  }


  private void updateUi() {
    if (getActivity() == null) {
      Log.d(TAG, "updateUi: getActivity = null");
    }

    Log.d(TAG, "updateUi: regetDevices");
    regetDevices();  // Will stop UI page update

    if (mPagerAdapter != null) {
      mPagerAdapter.notifyDataSetChanged();

      mPageIndicator.removeAllViews();
      for (int i = 0; i < mPagerAdapter.getCount(); i++) {
        LayoutInflater.from(getActivity()).inflate(R.layout.component_page_indicator, mPageIndicator);
      }
      mPageIndicator.setVisibility((mPageIndicator.getChildCount() > 1) ? View.VISIBLE : View.GONE);
      updatePageIndicator();
    }

    boolean bShowDetails = (devices != null && devices.size() > 0);
    mShowDetailsButton.setVisibility(bShowDetails ? View.VISIBLE : View.INVISIBLE);
    return;
  }

  private void updatePageIndicator() {
    int currentSelection = mPager.getCurrentItem();
    if (currentSelection >= 0 && currentSelection < mPageIndicator.getChildCount()) {
      ((CompoundButton) mPageIndicator.getChildAt(currentSelection)).setChecked(true);
    }
  }

  private View.OnClickListener onShowDetailsClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View btn) {
      if (mPager.getChildCount() > 0) {
        DeviceListActivity.startDetailsActivity(getActivity(), devices.getItem(mPager.getCurrentItem()).getDevice().getId());
        //Added
       // DeviceManagerHiQ.getInstance().refreshAllDevices("LIVE");
        ///

      }
    }
  };


  private class DevicePagerAdapter extends FragmentStatePagerAdapter {

    public DevicePagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      DeviceOverviewFragment fragment = new DeviceOverviewFragment();
      fragment.setDevice(devices.getItem(position));
      return fragment;
    }

    @Override
    public int getCount() {
      return (devices!=null) ? devices.size() : 0;
    }

    @Override
    public int getItemPosition(Object object) {
      // return POSITION_NONE if item was removed
      // this is not-trivial behavior of FragmentStatePagerAdapter
      if (object instanceof DeviceOverviewFragment) {
        DeviceOverviewFragment fragment = (DeviceOverviewFragment) object;
        Device device = fragment.getDevice();

        if (device  == null)
          return POSITION_NONE;

        for (int i = 0; i < devices.size(); i++) {
          Device iteratedDevice = devices.getItem(i).getDevice();
          if (iteratedDevice.getId().equals(device.getId())) {
            return POSITION_UNCHANGED;
          }
        }
      }

      return POSITION_NONE;
    }
  }

} // EOClass DevicesPagerFragment
