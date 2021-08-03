package com.ctek.sba.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.ctek.sba.R;
import com.ctek.sba.bluetooth.DeviceList;

import greendao.Device;

public class BluetoothDevicesListAdaptor extends RecyclerView.Adapter<BluetoothListVH> {

  private DeviceList mDeviceList;
  private onConnectClickListener mOnConnectClickListener;

  public interface onConnectClickListener{
    void connect(int pos);
  }

  public BluetoothDevicesListAdaptor(DeviceList mDeviceList, onConnectClickListener onConnectClickListener) {
    this.mOnConnectClickListener = onConnectClickListener;
    setData(mDeviceList);
  }

  public void setData(DeviceList mDeviceList) {
    this.mDeviceList = mDeviceList;
    notifyDataSetChanged();
  }

  @Override
  public BluetoothListVH onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth_device,parent,false);
    return new BluetoothListVH(view);
  }

  @Override
  public void onBindViewHolder(BluetoothListVH holder, int position) {
    holder.bind(mDeviceList.getDevice(position),mOnConnectClickListener,position);
  }

  @Override
  public int getItemCount() {
    return mDeviceList.size();
  }
}

class BluetoothListVH extends RecyclerView.ViewHolder {

  TextView tvBleName;
  TextView tvBleMacAddress;
  Button btnConnect;

  public BluetoothListVH(View itemView) {
    super(itemView);

    tvBleName = (TextView) itemView.findViewById(R.id.tv_ble_device_name);
    tvBleMacAddress = (TextView) itemView.findViewById(R.id.tv_ble_mac);
    btnConnect = (Button) itemView.findViewById(R.id.btn_ble_connect);
  }


  public void bind(Device device, BluetoothDevicesListAdaptor.onConnectClickListener mOnConnectClickListener, int position) {
    if (device.getName() != null) tvBleName.setText(device.getName());
    if (device.getAddress() != null) tvBleMacAddress.setText(device.getAddress());

    btnConnect.setOnClickListener(v -> {
      mOnConnectClickListener.connect(position);
    });

  }
}
