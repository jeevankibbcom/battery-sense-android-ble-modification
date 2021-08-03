package com.ctek.sba.util;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

import com.ctek.sba.R;

/**
 * Created by elab on 06/02/15.
 */
public class BluetoothHelper {

  private final static String TAG = BluetoothHelper.class.getSimpleName();

  private static BluetoothHelper instance;

  public static BluetoothHelper getInstance() {
    if (instance == null) {
      instance = new BluetoothHelper();
    }
    return instance;
  }

  public final static boolean SHOW_DIALOG_YES = true;
  public final static boolean SHOW_DIALOG_NOT = false;

  private Boolean mGotPermission = false;
  private BluetoothHelper() {
  }

  public void setPermission(Boolean permission) {
    mGotPermission = permission;
  }
  /**
   * Callback for bluetooth activation
   */
  public interface OnBluetoothActivatedCallback {
    void onBluetoothActivated();
  }

  /**
   * Checks if the bluetooth adapter is active.
   * Otherwise prompt user for activating.
   */

  public boolean checkBluetoothAdapterActive (Context ctx, boolean bShowDialog) {
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (bluetoothAdapter == null) {
      return false; // something went wrong with bluetooth adapter, maybe no bluetooth adapter!
    }

    if(!bluetoothAdapter.isEnabled()) {
      if(bShowDialog) {
        showBluetoothOFFDialog(ctx);
      }
      return false;
    }

    Log.d(TAG, "Bluetooth already turned ON");
    // We need the location permission also...
    return bluetoothAdapter.isEnabled() && mGotPermission;
  }

  public void showBluetoothOFFDialog (Context ctx) {
    int titlId = R.string.bluetooth;
    int messId = R.string.bluetooth_appears_to_be_turned_off;
    DialogHelper.showOk(ctx, titlId, messId);
  }

} // EOClass BluetoothHelper
