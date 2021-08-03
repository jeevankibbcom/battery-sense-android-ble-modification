package com.ctek.sba.ui;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ctek.sba.R;
import com.ctek.sba.bluetooth.DeviceList;
import com.ctek.sba.bluetooth.DeviceManager;
import com.ctek.sba.ui.adapter.BluetoothDevicesListAdaptor;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

// The LostSenderController provides the UI and logic to find
// serial number from a sender. The sender provides serial number
// during the first 5 minutes after power on.

public class LostSenderController {

  // Called to start connecting to the serial number found.
  public interface OnSuccessListener {
    void onSuccess(String serial);
  }

  public interface FoundDevicesListener{
    void found(DeviceList mDeviceList);
  }

  private final View view;
  private final Context context;

  // showSuccess means that we found a serial number and will show the connect buttong
  private boolean showSuccess;
  // searching means that we will show the search progress view
  private boolean searching;

  private String serial;
  private boolean shown;

  private OnSuccessListener onSuccessListener;
  private FoundDevicesListener foundDevicesListener;
  private BluetoothDevicesListAdaptor.onConnectClickListener mOnConnectClickListener;

  @InjectView(R.id.content) View content;
  @InjectView(R.id.progress) View progress;
  @InjectView(R.id.tutorial) View tutorial;
  @InjectView(R.id.success) View success;
  @InjectView(R.id.tv_serial) TextView serialTextView;
  @InjectView(R.id.start_button) TextView startButton;
  @InjectView(R.id.rv_found_devices) RecyclerView rvFoundDevice;

  public LostSenderController(Context context, View view) {
    this.context = context.getApplicationContext();
    this.view = view;
    ButterKnife.inject(this, view);

    // Show the normal content and hide the search progress view
    showSuccess = false;
    searching = false;
    updateUI();
  }

  // Show the lost code view. Called by NewDeviceActivity.
  public void show() {
    shown = true;
    view.setVisibility(VISIBLE);

    showSuccess = false;
    searching = false;
    updateUI();
  }

  // Hide the lost code view when done. Called by NewDeviceActivity.
  public void hide() {
    shown = false;
    view.setVisibility(INVISIBLE);
  }

  public void stop() {
    DeviceManager.getInstance().stop();
    EventBus.getDefault().unregister(this);
  }

  private RecyclerView.Adapter bleListAdapter;

  public void initRecyclerView(Context context,BluetoothDevicesListAdaptor.onConnectClickListener onConnectClickListener, DeviceList mDeviceList) {
    bleListAdapter = new BluetoothDevicesListAdaptor(mDeviceList,onConnectClickListener);

    this.mOnConnectClickListener = onConnectClickListener;
    rvFoundDevice.setVisibility(VISIBLE);
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
    linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
    rvFoundDevice.setLayoutManager(linearLayoutManager);

    rvFoundDevice.setAdapter(bleListAdapter);
  }

  public void setOnSuccessListener(OnSuccessListener onSuccessListener) {
    this.onSuccessListener = onSuccessListener;
  }

  public void setFoundDevicesListener(FoundDevicesListener foundDevicesListener) {
    this.foundDevicesListener = foundDevicesListener;
  }

  private void updateUI() {
    if (searching) {
      // Show the search progress spinner
      content.setVisibility(INVISIBLE);
      progress.setVisibility(VISIBLE);
    } else {
      // Show the normal content
      if(content!=null) {
        content.setVisibility(VISIBLE);
      }
      progress.setVisibility(INVISIBLE);

      if (showSuccess) {
        // Show the serial number found plus connect button
        tutorial.setVisibility(INVISIBLE);
        success.setVisibility(VISIBLE);
        startButton.setText(R.string.connect);
      } else {
        // Show tutorial plus search button
        tutorial.setVisibility(VISIBLE);
        success.setVisibility(INVISIBLE);
        startButton.setText(R.string.start_search);
      }
    }
  }

  @OnClick(R.id.start_button)
  void startSearch() {
    // The button has two functions dependent of mode.
    if (showSuccess) {
      // We got a serial number and will proceed to connect.
      if (onSuccessListener != null) {
        onSuccessListener.onSuccess(serial);
      }
    } else {
      // We shall start the search for a serial number
      EventBus.getDefault().register(this);
      DeviceManager.getInstance().getSerialNumber();

      searching = true;
      updateUI();
    }
  }


  public void onEventMainThread(DeviceManager.SearchComplete event) {
    // Check if we got a serial number
    if (event.serial == null) {
      showSuccess = false;
    }
    else {
      showSuccess = true;
      serial = event.serial;
//      String cuttedSerial = serial.replaceFirst(content.getResources().getString(R.string.add_device_code_prefix), "");
      serialTextView.setText(serial);
    }
    // We are no longer searching
    searching = false;
    // Update to show result of search
    updateUI();

    if (showSuccess == false && shown) {
      // Indicate to NewDeviceActivity that we failed.
      onSuccessListener.onSuccess(null);
    }
  }

  public void onEventMainThread(DeviceManager.GotDevicesScanList event) {
    // Check if we got a serial number
    if (event.mDeviceList != null && event.mDeviceList.size()>0) {
      foundDevicesListener.found(event.mDeviceList);
    }
  }
}
