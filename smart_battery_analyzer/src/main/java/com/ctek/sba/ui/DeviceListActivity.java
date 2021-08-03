package com.ctek.sba.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ctek.sba.BuildConfig;
import com.ctek.sba.R;
import com.ctek.sba.appwidget.MonitorService;
import com.ctek.sba.bluetooth.BluetoothLeManager;
import com.ctek.sba.bluetooth.CTEK;
import com.ctek.sba.bluetooth.DeviceManager;
import com.ctek.sba.bluetooth.DeviceManagerHiQ;
import com.ctek.sba.bluetooth.SBADevice;
import com.ctek.sba.device.DeviceMapIntervals;
import com.ctek.sba.device.TimeInterval;
import com.ctek.sba.io.dao.DeviceRepository;
import com.ctek.sba.rest.SrvPostSocs;
import com.ctek.sba.ui.support.MarshMallowPermission;
import com.ctek.sba.util.BluetoothHelper;
import com.ctek.sba.util.SettingsHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import greendao.Device;
import greendao.Voltage;
import greendao.VoltageDao;


public class DeviceListActivity extends BaseActivity {

  public static void start (Context ctx) {
    Intent i_ = new Intent(ctx, DeviceListActivity.class);
    ctx.startActivity(i_);
  }

  private static final String TAG = DeviceListActivity.class.getName();

  private static final String CONTENT = "CONTENT";

  private boolean mAddDeviceGuideAlreadyShown = false;

  private boolean mAskPermission = true;
  private boolean mGotPermission = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_device_list);

    // Use this check to determine whether BLE is supported on the device.
    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
      Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    Toolbar actionBar = (Toolbar) findViewById(R.id.action_bar);
    setSupportActionBar(actionBar);

    SettingsHelper.getPrefs(this).registerOnSharedPreferenceChangeListener(onPreferenceChangeListener);
    updateContents();

    if(BuildConfig.DEBUG) {
      insertDummyDevices();
      DeviceMapIntervals.logIntervalsAll(this);
      SrvPostSocs.start(this, "TEST call in DEBUG mode.", SrvPostSocs.MODE_HISTORY_DATA);
    }
    startService(new Intent(this, MonitorService.class));

    return;
  }


  /*
  private Double getRandomVoltage () {
    return 12. + Math.random();
  }


  private List<Voltage> generateVoltageList4Device (long deviceId) {
    Log.d(TAG, "generateVoltageList4Device +++++++ START deviceId = " + deviceId);
    final int NPoints = 7 * SBADevice.NUMBER_OF_READ_PERIODS_IN_DAY;
    final int iSkipStart[] = { 300, 800, 1100, 1500, };
    final int iSkipFinal[] = { 400, 900, 1200, 1600, };
    Long timestamp = System.currentTimeMillis() - NPoints*SBADevice.READ_PERIOD_MSECS;
    List<Voltage> list = new ArrayList<>();
    for(int ii=0;ii<NPoints;++ii) {
      boolean bSkip = false;
      for(int nn=0;nn<iSkipStart.length;++nn) {
        if((iSkipStart[nn]<=ii) && (ii<iSkipFinal[nn])) {
          bSkip = true;
          break;
        }
      }
      if(bSkip) {
        // skip
      }
      else {
        Double volt = getRandomVoltage();
        Log.d(TAG, "generateVoltageList4Device timestamp = " + timestamp + " V = " + volt.doubleValue());
        list.add(new Voltage(null, timestamp, volt, deviceId, VoltageDao.DFLT_TEMPERATURE));
      }
      timestamp += SBADevice.READ_PERIOD_MSECS;
    }
    Log.d(TAG, "generateVoltageList4Device +++++++ FINAL");
    return list;
  }
  */

  /*
    final int NPoints = 200;
    final int iSkipStart = 77;
    final int iSkipFinal = 150;

  Sections 4 +++++++
  Section 1/. Points = 2 DUMMY.
  Section 2/. Points = 77
  Section 3/. Points = 75 DUMMY.
  Section 4/. Points = 50
  Sections 4 =======
   */

  private void insertDummyDevices ()
  {
    // if(DeviceRepository.getAllDevices(this).size() == 0) {
      /*
      Device device1 = new Device(null, "Device 1", DeviceManagerHiQ.DUMMY_DEVICE_MAC_1, "", "00000001", true, 0L, 0, 0, 1, 11.0);
      device1.setVoltage(11.);
      DeviceRepository.insertOrUpdate(this, device1);
      */

      /*
      Device device2 = new Device(null, "Device 2", DeviceManagerHiQ.DUMMY_DEVICE_MAC_2, "", "00000002", true, 0L, 0, 0, 1, 12.0);
      device2.setVoltage(12.);
      DeviceRepository.insertOrUpdate(this, device2);

      device1.setVoltageList(generateVoltageList4Device(device1.getId()));
      DeviceRepository.insertOrUpdateWithVoltages(this, device1);

      device2.setVoltageList(generateVoltageList4Device(device2.getId()));
      DeviceRepository.insertOrUpdateWithVoltages(this, device2);
      */

      /*
      device1.setVoltageList(getTestVoltageList4Device(device1.getId()));
      DeviceRepository.insertOrUpdateWithVoltages(this, device1);
      */


    // }
    return;
  }

  @Override
  public void onResume() {
    super.onResume();

    // TEST // SrvPostSocs.start(this, "TEST from OnResume.", SrvPostSocs.MODE_HISTORY_DATA);


    // TEST THE NEW STUFF
    if (mAskPermission) {
      // Do not ask again, regardless of the answer.
      mAskPermission = false;


      //
      // CBS-155 Android - Investigate and remove location permission if possible. FAILED.
      //
      // Do not remove request for ACCESS_COARSE_LOCATION, the sense will not pair.
      // For details see
      // https://developer.android.com/guide/topics/connectivity/bluetooth-le.html
      //
      String permissions[] = new String[]{
          // Manifest.permission.BLUETOOTH,
          // Manifest.permission.BLUETOOTH_ADMIN,
          // stucks // Manifest.permission.BLUETOOTH_PRIVILEGED,
          Manifest.permission.ACCESS_COARSE_LOCATION,
      };
      MarshMallowPermission perm = new MarshMallowPermission(this, permissions, 0, MarshMallowPermission.REQUEST_CODE_LOCATION);

      if (!perm.check()) {

        mGotPermission = false;
        perm.request();

        /*
        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
          Log.d(TAG, "should request permission");

          // Show an explanation to the user *asynchronously* -- don't block
          // this thread waiting for the user's response! After the user
          // sees the explanation, try again to request the permission.

          // Ask anyway without explanation
          perm.request();
        }
        else {
          // No explanation needed, we can request the permission.
          Log.d(TAG, "request permission now");
          perm.request();
        }
        */

      } else {
        // We already got the permission
        mGotPermission = true;
        // Let BluetoothHelper know
        BluetoothHelper.getInstance().setPermission(true);
      }
    }

    // get mDevices list from db
    /*
    List<Device> devices = DeviceRepository.getAllDevices(this);
    int kk, kkSize = devices!=null ? devices.size(): 0;
    Log.d(TAG, "Devices = " + kkSize);
    for(kk=0;kk<kkSize;++kk) {
      Device device = devices.get(kk);
      Log.d(TAG, "Device " + (kk+1) + "/. " + device.getName() + " id = " + device.getId() + " serial = " + device.getSerialnumber() + " address = " + device.getAddress());
    }

    Fragment fragment = getSupportFragmentManager().findFragmentByTag(CONTENT);
    if (fragment instanceof DevicesListFragment) {
      ((DevicesListFragment) fragment).setDeviceList(devices);
    } else if (fragment instanceof DevicesPagerFragment) {
      ((DevicesPagerFragment) fragment).setDeviceList(devices);
    } else {
      throw new IllegalStateException();
    }
    */

    // Avoid the guide if already shown or if there are devices already paired.
    mAddDeviceGuideAlreadyShown = mAddDeviceGuideAlreadyShown || (DeviceRepository.getAllDevices(this).size() > 0);

    if (mGotPermission) {
      // Show guide or scan for devices
      getStarted();
    }

    if(BuildConfig.DEBUG) {
      // testCalcSoc();
    }
    return;
  }

  @Override
  protected void onDestroy() {
    stopService(new Intent(this, MonitorService.class));
    super.onDestroy();
  }

  /*
  private class LogCallback implements Calculate.ILogCallback {

    @Override
    public void LogD(String tag, String mess) {
      Log.d(tag, mess);
    }
  };

  private LogCallback mLogCallback = new LogCallback();

  private void testCalcSoc () {
    List<Double> voltageList = new ArrayList<>();
    voltageList.add(12.02468316286724);
    voltageList.add(12.799024702182274);
    voltageList.add(12.675921166977613);
    voltageList.add(12.76385580882498);
    voltageList.add(12.842455398113557);
    voltageList.add(12.683552918303244);
    Calculate calcSoc = new Calculate(voltageList.size(), mLogCallback);
    // SoCData.StateFlag flag =
    calcSoc.testVoltageList(voltageList);

    SoCData[] soc_data = calcSoc.getSoCData();
    return;
  }
  */

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    boolean success = (grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED);
    switch (requestCode) {
      case MarshMallowPermission.REQUEST_CODE_LOCATION: {
        // If request is cancelled, the result arrays are empty.
        if (success) {
          Log.d(TAG, "Location permission was granted");
          mGotPermission = true;
          // Now! Show guide or scan for devices
          getStarted();
        } else {

          Log.d(TAG, "Location permission was denied");
          mGotPermission = false;
        }
        // Let BluetoothHelper know
        BluetoothHelper.getInstance().setPermission(mGotPermission);
        return;
      }
    }
  }

  private void getStarted() {
    if (!mAddDeviceGuideAlreadyShown) {
      mAddDeviceGuideAlreadyShown = true;
      NewDeviceActivity.start(this, true);
    }
    else {
      if(sbDeviceDetailsStarted) {
        sbDeviceDetailsStarted = false;
//        new Handler().postDelayed(new Runnable() {
//          @Override
//          public void run() {
//            //DeviceManagerHiQ.getInstance().resendUpdateStates();   //Orginally presenr
//          }
//        }, 500);
      }
      else {
        if (BluetoothHelper.getInstance().checkBluetoothAdapterActive(this, BluetoothHelper.SHOW_DIALOG_NOT)) {
         // DeviceManagerHiQ.getInstance().updateAllDevices("Startup");

          //Added check
        //  BluetoothLeManager.getInstance(getBaseContext()).startBTSearch();
        }
        else {
          // do nothing
        }
      }
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  /*
  @Override
  public void onBackPressed() {
    moveTaskToBack(true);
  }
  */

  private void updateContents() {
    // FragmentManager will throw if trying to do something when activity is destroyed
    // This line is necessary since we can get here from onPreferenceChangeListener while
    // activity is destroyed
    if (isDestroyed()) return;

    int mode = SettingsHelper.getDeviceListMode(getApplicationContext());
    FragmentManager manager = getSupportFragmentManager();
    Fragment fragment = manager.findFragmentByTag(CONTENT);
    if (!rightFragmentInstalled(fragment, mode)) {
      fragment = createRightFragment(mode);
      FragmentTransaction transaction = manager.beginTransaction();
      transaction.replace(R.id.content, fragment, CONTENT);
      transaction.commitAllowingStateLoss();
      manager.executePendingTransactions();
    }
  }

  private Fragment createRightFragment(int mode) {
    if (mode == SettingsHelper.MODE_LIST) {
      return new DevicesListFragment();
    } else if (mode == SettingsHelper.MODE_PAGER) {
      return new DevicesPagerFragment();
    } else {
      throw new IllegalStateException();
    }
  }

  private boolean rightFragmentInstalled(Fragment fragment, int mode) {
    if (fragment instanceof DevicesPagerFragment && mode == SettingsHelper.MODE_PAGER) {
      return true;
    } else if (fragment instanceof DevicesListFragment && mode == SettingsHelper.MODE_LIST) {
      return true;
    } else {
      return false;
    }
  }

  public void addDevice(View view) {
    // Stop updating before adding new (might be handler later by the device
    // manager but this needs to be verified)
    DeviceManager.getInstance().stop();
    // Now launch the guide.
    NewDeviceActivity.start(this, false);
  }

  public void openSettings(View view) {
    DeviceManager.getInstance().stop();
    SettingsActivity.start(this);
  }

  private SharedPreferences.OnSharedPreferenceChangeListener onPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      if (key.equals(SettingsHelper.getDeviceListModeKey(getApplicationContext()))) {
        updateContents();
      }
    }
  };

  private static boolean sbDeviceDetailsStarted = false;
  public static void startDetailsActivity (Context ctx, long id) {
    sbDeviceDetailsStarted = true;
    // DeviceDetailsActivity.start(ctx, id);

    Intent i_ = new Intent(ctx, DeviceDetailsActivity.class);
    i_.putExtra(EditDeviceActivity.EXTRA_DEVICE_ID, id);
    ctx.startActivity(i_);

    return;
  }


  /*
  // Add data from excel file to assets.
  //
  // CTEKBATTERYSENSESLA-51
  //
  private List<Voltage> getTestVoltageList4Device (long deviceId) {
    List<Voltage> list = new ArrayList<Voltage>();
    // 2018.01.10 07:15
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.US);
    BufferedReader reader = null;
    int nLine = 0;
    try {
      reader = new BufferedReader(new InputStreamReader(getAssets().open("test_data_set.txt")));

      String line;
      while ((line = reader.readLine()) != null) {
        String part[] = line.split("\t");
        double volt = Double.parseDouble(part[0].replace(",", "."));
        double soc_ = Double.parseDouble(part[1].replace(",", "."));
        Date time = null;
        try {
          time = sdf.parse(part[2]);
        } catch (ParseException ignore) {}
        long timestamp = time.getTime();
        list.add(new Voltage(null, timestamp, volt, deviceId, VoltageDao.DFLT_TEMPERATURE));
        Log.d(TAG, "V = " + volt + " SoC = " + soc_ + " Time = " + part[2] + " Msecs = " + timestamp);
        nLine++;
      }
    } catch (IOException e) {
      //log the exception
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          //log the exception
        }
      }
    }
    Log.d(TAG, "nLines = " + nLine);
    return list;
  }
  */

} // EOClass DeviceListActivity
