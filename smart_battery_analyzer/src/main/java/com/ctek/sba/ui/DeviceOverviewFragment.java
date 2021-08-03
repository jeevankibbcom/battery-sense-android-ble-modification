package com.ctek.sba.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ctek.sba.R;
import com.ctek.sba.bluetooth.BluetoothLeManager;
import com.ctek.sba.bluetooth.DeviceInfoUI;
import com.ctek.sba.bluetooth.DeviceListUI;
import com.ctek.sba.bluetooth.DeviceManager;
import com.ctek.sba.bluetooth.DeviceManagerHiQ;
import com.ctek.sba.io.dao.DeviceRepository;
import com.ctek.sba.soc.SoCData;
import com.ctek.sba.soc.SoCUtils;
import com.ctek.sba.util.Utils;
import com.ctek.sba.widget.BatteryView;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import greendao.Device;

//@SuppressLint("ValidFragment")
public class DeviceOverviewFragment extends Fragment {

  private static final String TAG = DeviceOverviewFragment.class.getName();

  private Context mContext;
  private DeviceInfoUI info;
  private Device device;

  private ImageView photo;
  private TextView name;

  private ProgressBar mSpinner;
  //private TextView mSpinnerLabel;
  private BatteryView battery;
  private TextView batteryPercentLabel;

  //Added
  //private EasyImage easyImage;
  private TextView liveModeTimer;


  @InjectView(R.id.search_progress_label) TextView updateString;

  private BroadcastReceiver mBroadcastReceiver;

  public DeviceOverviewFragment() {
  }
  public void setDevice(DeviceInfoUI info) {
    this.info = info;
    device = (info!=null) ? info.getDevice() : null;
  }
  public Device getDevice () { return device; }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mContext = getActivity();
    //Added
   // BluetoothLeManager.getInstance(mContext).startBTSearch();
    ///
    final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_device_overview, container, false);
    ButterKnife.inject(this, rootView);

    photo = (ImageView) rootView.findViewById(R.id.device_photo);
    name = (TextView) rootView.findViewById(R.id.device_name);

  //  mSpinner = (ProgressBar) rootView.findViewById(R.id.search_progress);
    //mSpinnerLabel = (TextView) rootView.findViewById(R.id.search_progress_label);
    battery = (BatteryView) rootView.findViewById(R.id.device_battery_image);
    batteryPercentLabel = (TextView) rootView.findViewById(R.id.battery_percent_label);

//    mSpinner.setVisibility(View.INVISIBLE);

    mBroadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(DeviceManagerHiQ.ACTION_UPDATE_DEVICES_STATE.equals(action)) {
          int count = intent.getIntExtra(DeviceManagerHiQ.EXTRA_UPDATE_DEVICES_COUNT, -1);
          if(count==0) {
            info.setIsUpdatedNow(false);
            updateDeviceRelatedUi();
            return;
          }
          for(int kk=0;kk<count;++kk) {
            //Added
            Long deviceId = intent.getLongExtra(DeviceManager.EXTRA_DEVICE_ID + kk, -1);
            if (device != null && deviceId == device.getId()) {
              boolean isUpdatedNow = intent.getBooleanExtra(DeviceManagerHiQ.EXTRA_UPDATE_DEVICE_STATE + kk, false);
              info.setIsUpdatedNow(isUpdatedNow);
              updateDeviceRelatedUi();
              break;

            }
          }
          return;
        }

        Long deviceId = intent.getLongExtra(DeviceManager.EXTRA_DEVICE_ID, -1);
        if (deviceId == -1) {
          return;
        }

        if (device != null && deviceId == device.getId()) {
          if(DeviceManager.ACTION_DEVICE_FOUND.equals(action)) {
            info.setIsUpdatedNow(true);
            updateDeviceRelatedUi();
          }
          else if(DeviceManager.ACTION_DEVICE_UPDATED.equals(action)) {
            info.setIsUpdatedNow(false);
            updateDeviceRelatedUi();
          }
          else if(DeviceManager.ACTION_DEVICE_DISCONNECTED.equals(action)) {
            info.setIsUpdatedNow(false);
            updateDeviceRelatedUi();
          }
        }
        return;
      }
    };

    getActivity().registerReceiver(mBroadcastReceiver, makeUpdateIntentFilter());
    return rootView;
  }

  @Override
  public void onResume() {
    super.onResume();
    if (info == null) {
      return;
    }

    //Added

    String deviceName = device.getName();
    name.setText(deviceName!=null ? deviceName : "");

    String path = device.getImagePath();
    if(path!=null) {
      //Added
      photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
      photo.setImageBitmap(Utils.rotateImage(new File(path)));

    }
    updateDeviceRelatedUi();

    //DeviceManagerHiQ.getInstance().resendUpdateStates();
  }

  @Override
  public void onPause() {
    super.onPause();
  }

  private void updateDeviceRelatedUi() {
    if (getActivity()!=null && device!=null) {
      SoCData soc = SoCUtils.getLatestSocValue(device);
//      //Added
//      BluetoothLeManager.getInstance(mContext).startBTSearch();
//      ///
      SoCUtils.updateBattery(battery, soc);
      SoCUtils.updatePercentLabel(batteryPercentLabel, soc);

      if(info.getIsUpdatedNow()) {
//        mSpinner.setVisibility(View.VISIBLE);
  //      updateString.setText(R.string.updating_sender);
      }
      else {
     //   mSpinner.setVisibility(View.INVISIBLE);
        updateString.setText(device.getUpdateString(mContext));
      }
    }
    return;
  }

  @Override
  public void onDestroy() {
    if(mBroadcastReceiver!=null) { getActivity().unregisterReceiver(mBroadcastReceiver); mBroadcastReceiver = null; }
    super.onDestroy();
  }

  @Override
  public void onStart() {
    super.onStart();
    EventBus.getDefault().register(this);
  }

  @Override
  public void onStop() {
    super.onStop();
    EventBus.getDefault().unregister(this);
  }

  private IntentFilter makeUpdateIntentFilter() {
    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(DeviceManager.ACTION_DEVICE_FOUND);
    intentFilter.addAction(DeviceManager.ACTION_DEVICE_UPDATED);
    intentFilter.addAction(DeviceManager.ACTION_DEVICE_DISCONNECTED);
    intentFilter.addAction(DeviceManagerHiQ.ACTION_UPDATE_DEVICES_STATE);
    return intentFilter;
  }

  public void onEventMainThread(DeviceRepository.DeviceUpdatedEvent event) {
    if (getActivity() == null) return;

    if (device != null && event.deviceId == device.getId()) {
      device = DeviceRepository.getDeviceForId(getActivity().getApplication(), device.getId());
      info = new DeviceInfoUI(device);
      updateDeviceRelatedUi();
    }
  }

} // EOClass DeviceOverviewFragment
