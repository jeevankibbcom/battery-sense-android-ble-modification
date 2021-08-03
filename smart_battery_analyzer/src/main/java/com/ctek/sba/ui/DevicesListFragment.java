package com.ctek.sba.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
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
import com.ctek.sba.util.BluetoothHelper;
import com.ctek.sba.widget.BatteryView;

import java.util.List;

import greendao.Device;

public class DevicesListFragment extends Fragment {

  private static final String TAG = DevicesListFragment.class.getName();

  private ListView list;
  private DeviceListAdapter listAdapter = null;
  private DeviceListUI devices;

  private BroadcastReceiver mBroadcastReceiver;

  private Context mContext;
  private LayoutInflater layoutInflater;
  private SwipeRefreshLayout swipeView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mContext = getActivity();
    layoutInflater = inflater;
    final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_device_list, container, false);

   // swipeView = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
//    if (swipeView != null) {
//      swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//        @Override
//        public void onRefresh() {
//          if (BluetoothHelper.getInstance().checkBluetoothAdapterActive(mContext, BluetoothHelper.SHOW_DIALOG_YES)) {
//            if(devices!=null) {
//              devices.cleanUpdatedNow();
//              DeviceManagerHiQ.getInstance().refreshAllDevices("Pull2Refresh");
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
//
//      });
//    }

    mBroadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive, action = " + action);

        if (listAdapter == null) {
          Log.d(TAG, "onReceive, listAdapter is null");
          return;
        }

        if (devices == null) {
          Log.d(TAG, "onReceive, deviceList is null");
          return;
        }

        if(DeviceManager.ACTION_DEVICE_FOUND.equals(action)) {
          Long deviceId = intent.getLongExtra(DeviceManager.EXTRA_DEVICE_ID, -1);
          updateDevice(deviceId, true);
          updateUi();
        }
        else if(DeviceManager.ACTION_DEVICE_UPDATED.equals(action)) {
          Long deviceId = intent.getLongExtra(DeviceManager.EXTRA_DEVICE_ID, -1);
          updateDevice(deviceId, false);
          updateUiDelayed();
        }
        else if(DeviceManagerHiQ.ACTION_UPDATE_COMPLETE.equals(action)) {
          // CTEKBATTERYSENSESLA-26 Android - Time for last read not immediately updated.
          // One more chance is code from DeviceListActivity.onResume().
          updateUiDelayed();
        }
        else if(DeviceManager.ACTION_DEVICE_DISCONNECTED.equals(action)) {
          Long deviceId = intent.getLongExtra(DeviceManager.EXTRA_DEVICE_ID, -1);
          updateDevice(deviceId, false);
          updateUi();
        }
        else if(DeviceManagerHiQ.ACTION_UPDATE_DEVICES_STATE.equals(action)) {
          int count = intent.getIntExtra(DeviceManagerHiQ.EXTRA_UPDATE_DEVICES_COUNT, -1);
          if(count==0) {
            for (DeviceInfoUI deviceUI : devices.list) {
              deviceUI.setIsUpdatedNow(false);
            }
            updateUi();
            return;
          }
          for(int kk=0;kk<count;++kk) {
            Long deviceId = intent.getLongExtra(DeviceManager.EXTRA_DEVICE_ID + kk, -1);
            boolean isUpdatedNow = intent.getBooleanExtra(DeviceManagerHiQ.EXTRA_UPDATE_DEVICE_STATE + kk, false);
            updateDevice(deviceId, isUpdatedNow);
          }
          updateUi();
        }
        return;
      }
    };

    list = (ListView) rootView.findViewById(R.id.list);
    list.setOnItemClickListener(onListItemClickListener);
    listAdapter = new DeviceListAdapter();
    list.setAdapter(listAdapter);

    getActivity().registerReceiver(mBroadcastReceiver, makeUpdateIntentFilter());
    return rootView;
  }

  private void updateUiDelayed () {
    // EA 10-Nov-2017. Didn't help.
    /*
    (new Handler()).postDelayed(new Runnable() {
      @Override
      public void run() {
        updateUi();
      }
    }, 1000);
    */
    updateUi();
  }


  private void updateDevice (Long deviceId, boolean isUpdatedNow) {
    DeviceInfoUI deviceInfo = findDeviceById(deviceId);
    if(deviceInfo!=null) {
      deviceInfo.setIsUpdatedNow(isUpdatedNow);
    }
  }

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
    DeviceManagerHiQ.getInstance().resendUpdateStates();  //Orginally present
  }

  @Override
  public void onPause() {
    super.onPause();
  }

  private IntentFilter makeUpdateIntentFilter() {
    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(DeviceManager.ACTION_DEVICE_FOUND);
    intentFilter.addAction(DeviceManager.ACTION_DEVICE_UPDATED);
    intentFilter.addAction(DeviceManagerHiQ.ACTION_UPDATE_COMPLETE);
    intentFilter.addAction(DeviceManager.ACTION_DEVICE_DISCONNECTED);
    intentFilter.addAction(DeviceManagerHiQ.ACTION_UPDATE_DEVICES_STATE);
    return intentFilter;
  }

  /*
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mContext = activity;
  }
  */

  @Override
  public void onDestroy() {
    if(mBroadcastReceiver!=null) { getActivity().unregisterReceiver(mBroadcastReceiver); mBroadcastReceiver = null; }
    super.onDestroy();
  }

  /*
  public void setDeviceList(List<Device> deviceList) {
    devices = new DeviceListUI(deviceList);
    updateUi();
  }
  */

  private void updateUi() {
    if (getActivity() == null) {
      Log.d(TAG, "updateUi: getActivity = null");
      return;
    }

    Log.d(TAG, "updateUi: regetDevices");
    regetDevices();
    if (listAdapter!=null) {
      listAdapter.notifyDataSetChanged();
    }
  }

  private AdapterView.OnItemClickListener onListItemClickListener = new AdapterView.OnItemClickListener() {
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      DeviceListActivity.startDetailsActivity(getActivity(), id);
    }
  };

  private DeviceInfoUI findDeviceById (Long deviceId) {
    for (DeviceInfoUI deviceUI : devices.list) {
      if (deviceUI.getDevice().getId().equals(deviceId)) {
        return deviceUI;
      }
    }
    return null;
  }

  //
  // class DeviceListAdapter +++
  //
  public class DeviceListAdapter extends BaseAdapter {

    public DeviceListAdapter () {
    }

    @Override public int getCount() { return devices!=null ? devices.size() : 0; }
    @Override public Object getItem(int position) { return devices.getItem(position); }
    @Override public long getItemId(int position) { return devices.getItem(position).getDevice().getId(); }

    @Override
    public View getView(final int iPos, View convertView, ViewGroup parent) {
      if(convertView==null) {
        convertView = layoutInflater.inflate(R.layout.list_item_device, parent, false);
      }

      BatteryView battery = (BatteryView) convertView.findViewById(R.id.device_battery_image);
      TextView name = (TextView) convertView.findViewById(R.id.device_name);
      TextView updateString = (TextView) convertView.findViewById(R.id.device_update_string);
      TextView percentLabel = (TextView) convertView.findViewById(R.id.battery_percent_label);
     // ProgressBar mSpinner = (ProgressBar) convertView.findViewById(R.id.search_progress);

      DeviceInfoUI deviceUI = devices.getItem(iPos);
      Device device = deviceUI.getDevice();
      final Long deviceId = device.getId();

      SoCData soc = SoCUtils.getLatestSocValue(device);
      //Added
      //BluetoothLeManager.getInstance(mContext).startBTSearch();
      ///
      SoCUtils.updateBattery(battery, soc);
      SoCUtils.updatePercentLabel(percentLabel, soc);

      String deviceName = device.getName();
      name.setText(deviceName!=null ? deviceName : "");

      if(deviceUI.getIsUpdatedNow()) {
      //  mSpinner.setVisibility(View.VISIBLE);
        //updateString.setText(R.string.updating_sender);
        updateString.setText("");
      }
      else {
       // mSpinner.setVisibility(View.INVISIBLE);
        updateString.setText(device.getUpdateString(mContext));
      }

      convertView.setTag(getItem(iPos));
      return convertView;
    }

  } // EOClass DeviceListAdapter

} // EOClass DevicesListFragment
