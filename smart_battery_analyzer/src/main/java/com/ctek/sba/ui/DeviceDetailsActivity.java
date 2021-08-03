package com.ctek.sba.ui;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ctek.sba.R;
import com.ctek.sba.bluetooth.BluetoothLeManager;
import com.ctek.sba.bluetooth.CTEK;
import com.ctek.sba.bluetooth.DeviceManager;
import com.ctek.sba.bluetooth.DeviceManagerHiQ;
import com.ctek.sba.device.DeviceMap;
import com.ctek.sba.io.dao.DeviceRepository;
import com.ctek.sba.soc.SoCData;
import com.ctek.sba.soc.SoCUtils;
import com.ctek.sba.ui.support.ChartHelper;
import com.ctek.sba.util.BluetoothHelper;
import com.ctek.sba.util.SettingsHelper;
import com.ctek.sba.widget.BatteryView;
import com.github.mikephil.charting.charts.LineChart;

import java.util.ArrayList;

import greendao.Device;


public class DeviceDetailsActivity extends BaseActivity {

  public final String TAG = this.getClass().getSimpleName();
  public final String CB_START_LIVE_MODE = "start_live_mode";

  public static final int SHOW_TEMPERATURE_GRAPH = 0;
  public static final int SHOW_SOC_GRAPH = 1;
  public static final int SHOW_VOLTAGE_GRAPH = 2;

  private int selectedGraphType = 2;

  private Handler mHandler = new Handler();
  protected boolean bUseTestLiveData = false;
  private LiveBroadcastReceiver liveBroadcastReceiver;
  private double currVolt;
  private double currTemp;

  private BatteryView battery;
  private boolean isActivityLive = false;

//  private RelativeLayout layoutAllStatusContainer;
  private LinearLayout allStatusContainer;
  private RelativeLayout layoutShowAllStatusPopup;

  private RadioButton radioBtnTemperature;
  private RadioButton radioBtnSoc;
  private RadioButton radioBtnVoltage;
  private RadioGroup radioGrpShowGraph;

  private Button btnShowAllStatus;
  private Button btnCloseAllStatusPopup;
  private Button btnReconnect;

  private ImageView imgBatterystatus;

  private TextView txtBatteryStatus;
  private TextView batteryPercentLabel;
  public  TextView connectionStatus;
  private TextView txtVolt;
  private TextView txtTemp;
//  private TextView liveModeTimer;


  public LiveModeEnded liveModeEnded;
  public IsDeviceAvailable isDeviceAvailable;

  public StartLiveModeBR startLiveModeBR; ///

  private LineChart mChart;
  private LinearLayout batteryParentLayout;  // CBS-9 Add sender name to landscape view of history graph.
  private long mDeviceId;
  private Device device;

  private ChartHelper chartHelperLive;

  private boolean bLiveView = false;
  private boolean bShowVoltage;
  private boolean bLandscape;
  private boolean bStartLiveMode;

  private ProgressBar progressBar;

  private CountDownTimer liveModeCountdown;

  private RelativeLayout batteryStatus;

  @Override
  protected void onCreate(Bundle sis) {
    super.onCreate(sis);

    setContentView(R.layout.activity_device_details);

    bShowVoltage = SettingsHelper.getShowVoltage(this);
    bLandscape   = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    bStartLiveMode = (sis!=null) ? sis.getBoolean(CB_START_LIVE_MODE) : (bShowVoltage && !bLandscape);
    liveModeCountdown= new CountDownTimer(20000,1000) {
      @Override
      public void onTick(long millisUntilFinished) {
//        liveModeTimer.setText("Searching for\ndevice "+millisUntilFinished/1000);
//        liveModeTimer.setVisibility(View.VISIBLE);
      }
      @Override
      public void onFinish() {
        btnReconnect.setVisibility(View.VISIBLE);
//        liveModeTimer.setText("No CTEK\ndevice found");
        connectionStatus.setText(R.string.conn_timeout);
        progressBar.setVisibility(View.INVISIBLE);
//        liveModeTimer.setVisibility(View.VISIBLE);
      }
    };

    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      mDeviceId = extras.getLong(EditDeviceActivity.EXTRA_DEVICE_ID);
    }

    txtVolt = findViewById(R.id.txtVoltage);
    txtTemp = findViewById(R.id.txtTemperature);

    txtVolt.setVisibility(bShowVoltage ? View.VISIBLE : View.GONE);
    txtTemp.setVisibility(bShowVoltage ? View.VISIBLE : View.GONE);
    battery = findViewById(R.id.battery);
    batteryPercentLabel = findViewById(R.id.battery_percent_label);

    imgBatterystatus = findViewById(R.id.img_batter_status);
    txtBatteryStatus = findViewById(R.id.txt_battery_status);
    txtBatteryStatus.setText("");

    progressBar = findViewById(R.id.search_progress);

    batteryStatus = findViewById(R.id.battery_legends);
    //Added
//    liveModeTimer = findViewById(R.id.live_mode_timer);
//    liveModeTimer.setVisibility(View.GONE);

    btnReconnect = findViewById(R.id.btn_reconnect);
    btnReconnect.setVisibility(View.INVISIBLE);
    btnReconnect.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startBleScan();
      }
    });
    ///
    //Added
    connectionStatus = (TextView) findViewById(R.id.connectionStatus);
    connectionStatus.setText(getString(R.string.searching));
    progressBar.setVisibility(View.VISIBLE);
    liveBroadcastReceiver = new LiveBroadcastReceiver();
    IntentFilter if_ = new IntentFilter();
    if_.addAction(CTEK.ACTION_LIVE_MODE);
//    if_.addAction(CTEK.ACTION_SERVICE_STOPPED);
    if_.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    if_.addAction(DeviceManager.ACTION_DEVICE_DISCONNECTED);
    registerReceiver(liveBroadcastReceiver, if_);

    //Added
    startLiveModeBR = new StartLiveModeBR();
    registerReceiver(startLiveModeBR,new IntentFilter(DeviceManagerHiQ.START_LIVE_MODE));

    liveModeEnded = new LiveModeEnded();
    registerReceiver(liveModeEnded,new IntentFilter(DeviceManagerHiQ.LIVE_MODE_ENDED));

    isDeviceAvailable = new IsDeviceAvailable();
    registerReceiver(isDeviceAvailable,new IntentFilter(BluetoothLeManager.DEVICE_CONNECTED));
    ///

    //Added test
    device = DeviceRepository.getDeviceForId(this, mDeviceId);
    final SoCData soc2 = SoCUtils.getLatestSocValue(device);
    SoCUtils.updateBattery(battery,  txtBatteryStatus, imgBatterystatus,null); //Original soc "null"
    SoCUtils.updatePercentLabel(batteryPercentLabel, null);  //Original soc "null"
    txtVolt.setText("");
    txtTemp.setText("");
    batteryPercentLabel.setText("Battery percentage unavailable");

    radioBtnTemperature = findViewById(R.id.radio_btn_temperature);
    radioBtnSoc = findViewById(R.id.radio_btn_soc);
    radioBtnTemperature = findViewById(R.id.radio_btn_voltage);
    radioGrpShowGraph = findViewById(R.id.radiogrp_graph);

    //If user not selected show voltage and temperature
    //  then hide the radio group button
    //  and show only SoC graphs
    if (SettingsHelper.getShowVoltage(getApplicationContext()) == false) {
      radioGrpShowGraph.setVisibility(View.GONE);
      selectedGraphType = 1;
    }

    radioGrpShowGraph.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
        int id = radioGrpShowGraph.getCheckedRadioButtonId();

        if (id == R.id.radio_btn_temperature) {
          selectedGraphType = 0;
        }
        else if (id == R.id.radio_btn_soc) {
          selectedGraphType = 1;
        }
        else if (id == R.id.radio_btn_voltage) {
          selectedGraphType = 2;
        }

        //when screen orientation changes method "onCheckedChanged" is called that time device is null and crashed. To handle the crash added this condition
        if (device != null) {
          loadDeviceDetails(device, selectedGraphType);
        }

      }
    });

    allStatusContainer = (LinearLayout)findViewById(R.id.layout_all_status);

    btnShowAllStatus = (Button)findViewById(R.id.btn_show_allstatus);
    btnShowAllStatus.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {

        layoutShowAllStatusPopup.setVisibility(View.VISIBLE);

        Rect rectf = new Rect();
        btnShowAllStatus.getGlobalVisibleRect(rectf);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = rectf.top+30;
        params.rightMargin = 10;
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        allStatusContainer.setLayoutParams(params);
      }
    });

    layoutShowAllStatusPopup = (RelativeLayout)findViewById(R.id.layout_show_all_status);
    layoutShowAllStatusPopup.setVisibility(View.GONE);

    btnCloseAllStatusPopup = (Button)findViewById(R.id.btn_close_all_status_popup);
    btnCloseAllStatusPopup.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        layoutShowAllStatusPopup.setVisibility(View.GONE);
      }
    });
  }

  @Override
  protected void onSaveInstanceState (Bundle sis) {
    super.onSaveInstanceState(sis);
    sis.putBoolean(CB_START_LIVE_MODE, bStartLiveMode);
    return;
  }

  @Override
  protected void onPause() {
    isActivityLive = false;
    super.onPause();
    if(bShowVoltage && !bLandscape) {
      DeviceManagerHiQ.getInstance().stopLiveMode();
    }
    chartHelperLive = null;
    stopLiveThread();
  }

  @Override
  protected void onDestroy() {
    liveModeCountdown.cancel();
    BluetoothLeManager.getInstance(this).stopBTSearch();
    if(liveBroadcastReceiver!=null) { unregisterReceiver(liveBroadcastReceiver); liveBroadcastReceiver = null; }
    if (startLiveModeBR!=null) { unregisterReceiver(startLiveModeBR); startLiveModeBR = null; }
    if (liveModeEnded!=null) { unregisterReceiver(liveModeEnded); liveModeEnded = null; }
    if (isDeviceAvailable!=null) { unregisterReceiver(isDeviceAvailable); isDeviceAvailable = null; }
    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        DeviceManagerHiQ.getInstance().stop();
      }
    },800);
    super.onDestroy();
  }

  @Override
  public void onResume() {
    isActivityLive = true;
    super.onResume();

    device = DeviceRepository.getDeviceForId(this, mDeviceId);
    if (device != null) {
      if(bStartLiveMode) {
        // Check whether Bluetooth is ON.
        if(BluetoothHelper.getInstance().checkBluetoothAdapterActive(this, BluetoothHelper.SHOW_DIALOG_NOT)) {
          //Added test
          this.startBleScan();
        }
        else {
          // BT OFF dialog appears.

          // CBS-126 Android: Initially wrong date in Graph.
          // Dialog prevents Chart to set position, so we show it with a delay.
          mHandler.postDelayed(new Runnable() {
            public void run() {
              BluetoothHelper.getInstance().showBluetoothOFFDialog(DeviceDetailsActivity.this);
            }
          }, 400);
        }
      }
      else {
        this.startBleScan();
      }
      loadDeviceDetails(device, selectedGraphType);
    } else {
      // This can be after we delete a device
      finish();
    }
  }

  @Override
  public void onBackPressed() {
    back(null);
  }

  private void startBleScan() {
    btnReconnect.setVisibility(View.INVISIBLE);
    liveModeCountdown.cancel();
    liveModeCountdown.start();
    connectionStatus.setText(getString(R.string.searching));
    progressBar.setVisibility(View.VISIBLE);
    BluetoothLeManager.getInstance(this).startBTSearch();
  }

  private void stopBle() {
    liveModeCountdown.cancel();
    DeviceManagerHiQ.getInstance().stopBLEConnection();
  }
  private void loadDeviceDetails(Device device, int showGraphType) {

    Toolbar actionBar = (Toolbar) findViewById(R.id.action_bar);
    setSupportActionBar(actionBar);
    ((TextView) findViewById(R.id.title)).setText(device.getName());
    mChart = (LineChart) findViewById(R.id.chart);
    mChart.clear(); // CBS-42 chart empty after changing landscape/portrait a few times
    batteryParentLayout = (LinearLayout) findViewById(R.id.battery_parent_layout);
    // hide battery if in landscape
    if (bLandscape) {
      batteryParentLayout.setVisibility(View.GONE);
    }
    generateBatteryStatus(device);
    //Added
    // DeviceManagerHiQ.getInstance().updateAllDevices("LIVE MODE");
    if(bLiveView) {
      new ChartHelper(this, TAG).generateGraph(mChart, device, selectedGraphType);
      if(bUseTestLiveData) {
        startLiveThread();  // test live data
      }
      mChart.setKeepScreenOn(true);
    }
    else {
      chartHelperLive = null;
      final ChartHelper helper = new ChartHelper(this, TAG);
      helper.generateGraph(mChart, device, selectedGraphType);

      mHandler.postDelayed(new Runnable() {
        public void run() {
          helper.moveChartToEnd();
        }
      }, 200);
    }
    return;
  }

  private void setLiveView (boolean bLive) {
    if(bLive && bLive==bLiveView) return;
    bLiveView = bLive;
    mChart.setKeepScreenOn(bLive);
    if(bLive) {
      generateBatteryStatus(device);
    }
    else {
    }
    generateBatteryStatus(device);
    return;
  }

  public void editDevice(View view) {
    txtTemp.setVisibility(View.INVISIBLE);
    txtVolt.setVisibility(View.INVISIBLE);
    EditDeviceActivity.start(this, false, mDeviceId);
    stopBle();
    finish();
  }

  public void back(View view) {
    BluetoothLeManager.getInstance(this).stopBTSearch();
    stopBle();
    finish();
  }

  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    batteryParentLayout.setVisibility(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT ? View.VISIBLE : View.GONE);
  }

  private void generateBatteryStatus(Device device) {
    final SoCData soc = SoCUtils.getLatestSocValue(device);
    SoCUtils.updateBattery(battery, txtBatteryStatus, imgBatterystatus, soc);
    double temperatureC = DeviceMap.getInstance().getDeviceCurrTemp(device.getId());
    SoCUtils.updateSoC_Voltage_Temperature(this, batteryPercentLabel, txtVolt, txtTemp, soc, temperatureC, bLiveView);
  }

  private Thread thread = null;

  private void stopLiveThread () {
    if (thread != null) {
      thread.interrupt();
      thread = null;
    }
  }

  final Runnable runnableSetCurrVoltage = new Runnable() {
    @Override public void run() {
      SoCUtils.setUIVoltage(txtVolt, currVolt);
    }
  };

  final Runnable runnableSetCurrTemperature = new Runnable() {
    @Override public void run() {
      SoCUtils.setUITemperature(DeviceDetailsActivity.this, txtTemp, currTemp, bLiveView);
    }
  };

  // Don't generate garbage runnables inside the loop.
  final Runnable runnableAddEntry = new Runnable() {
    @Override public void run() {
      if(chartHelperLive!=null) {
        chartHelperLive.addLiveEntry(TAG, ChartHelper.getRandomVoltage());
      }
      if(bShowVoltage) {
        SoCUtils.setUIVoltage(txtVolt, ChartHelper.getRandomVoltage());
      }
    }
  };

  private void startLiveThread() {
    stopLiveThread();
    thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          for (int i = 0; ; i++) {
            // TODO: Need to check sleep is required or not
            Thread.sleep(1000);
            runOnUiThread(runnableAddEntry);
          }
        }
        catch (InterruptedException xpt) {
          xpt.printStackTrace();
        }
      }
    });
    thread.start();
    return;
  }

  private class LiveBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive (Context ctx, Intent i_) {
      if (isActivityLive) {
        String action = i_.getAction();
        if(CTEK.ACTION_LIVE_MODE.equals(action)) {
          String address = i_.getStringExtra(CTEK.EXTRA_BLE_DEVICE_ADDRESS);
          if (SettingsHelper.getShowVoltage(getApplicationContext())) {
            liveModeCountdown.cancel();
            connectionStatus.setText(R.string.live_mode);
            progressBar.setVisibility(View.INVISIBLE);
//            liveModeTimer.setVisibility(View.GONE);
          }

          btnReconnect.setVisibility(View.INVISIBLE);
          if(device.getAddress().equals(address)) {
            if(i_.getExtras().containsKey(CTEK.EXTRA_BLE_LIVE_VOLTAGE)) {
              currVolt = i_.getExtras().getDouble(CTEK.EXTRA_BLE_LIVE_VOLTAGE);
              mHandler.post(new Runnable() { public void run() { setLiveView(true); } });
              txtVolt.setVisibility(View.VISIBLE);
              runOnUiThread(runnableSetCurrVoltage);
            }
            else if(i_.getExtras().containsKey(CTEK.EXTRA_BLE_LIVE_TEMPERATURE)) {
              currTemp = i_.getExtras().getDouble(CTEK.EXTRA_BLE_LIVE_TEMPERATURE);
              mHandler.post(new Runnable() { public void run() { setLiveView(true); } });
              txtTemp.setVisibility(View.VISIBLE);
              runOnUiThread(runnableSetCurrTemperature);
            }
          }
        }
//        else if(CTEK.ACTION_SERVICE_STOPPED.equals(action)) {
//          String reason = i_.getStringExtra(CTEK.EXTRA_BLE_REASON);
//          Log.d(TAG, "Service stopped. Reason: " + reason);
//          bStartLiveMode = false;
//          mHandler.post(new Runnable() {
//            public void run() {
//              connectionStatus.setText(R.string.disconnected_live_mode);
//              liveModeTimer.setText("No CTEK\ndevice found");
//              liveModeTimer.setVisibility(View.VISIBLE);
//              btnReconnect.setVisibility(View.VISIBLE);
//              setLiveView(false);
//            }
//          });
//        }
        else if(DeviceManager.ACTION_DEVICE_DISCONNECTED.equals(action)) {

          if (i_.hasExtra("DATA")) {
            //this will be called when histroy is not available
            mHandler.post(new Runnable() {
              public void run() {
                liveModeCountdown.cancel();
                if (SettingsHelper.getShowVoltage(getApplicationContext())) {
//                  liveModeTimer.setVisibility(View.VISIBLE);
//                  liveModeTimer.setText("Fetching\nLive data");
                  DeviceManagerHiQ.getInstance().startLiveMode(device.getAddress());
                }
                else {
//                  liveModeTimer.setText("");
//                  liveModeTimer.setVisibility(View.VISIBLE);
                }
                //Battery percentage updating
                generateBatteryStatus(device);
                if (device != null) {
                  //Graph updating
                  loadDeviceDetails(device, selectedGraphType);
                }
              }
            });
          }
          else {
            Log.d(TAG, "Device disconnected.");
            bStartLiveMode = false;
            mHandler.post(new Runnable() {
              public void run() {
                connectionStatus.setText(R.string.disconnected_live_mode);
                btnReconnect.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
//                liveModeTimer.setText("No CTEK\ndevice found");
//                liveModeTimer.setVisibility(View.VISIBLE);
                setLiveView(false);
              }
            });
          }
        }
        else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
          int state =  i_.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

          if (state == BluetoothAdapter.STATE_OFF) {
            connectionStatus.setText(getString(R.string.bt_disconnected));
            progressBar.setVisibility(View.INVISIBLE);
            btnReconnect.setVisibility(View.INVISIBLE);
          }
          else if((state==BluetoothAdapter.STATE_ON) && (!bLiveView)) {
            if(bShowVoltage) {
              connectionStatus.setText(getString(R.string.connecting_live_mode));
              progressBar.setVisibility(View.VISIBLE);
              btnReconnect.setVisibility(View.INVISIBLE);
              DeviceManagerHiQ.getInstance().liveModeUpdateBattery(device.getAddress()); //Check
            }
          }
        }
      }
    }
  };

  //Added
  private class StartLiveModeBR extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      String msg = intent.getAction();
      if(DeviceManagerHiQ.START_LIVE_MODE.equals(msg)) {
        if (SettingsHelper.getShowVoltage(getApplicationContext())) {
//          liveModeTimer.setVisibility(View.VISIBLE);
////          mHandler.post(new Runnable() { public void run() { generateBatteryStatus(device); } });
//          liveModeTimer.setText("Fetching\nLive data");
          DeviceManagerHiQ.getInstance().startLiveMode(device.getAddress());
        }
        else {
//          liveModeTimer.setText("");
        }
        mHandler.post(new Runnable() {
          public void run() {
            //Battery percentage updating
            generateBatteryStatus(device);
            if (device != null) {
              //Graph updating
              loadDeviceDetails(device, selectedGraphType);
            }
          }
        });
      }
    }
  }

  private class LiveModeEnded extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      String msg = intent.getAction();
      if (DeviceManagerHiQ.LIVE_MODE_ENDED.equals(msg)) {
        DeviceManagerHiQ.getInstance().stopLiveMode();
      }
    }
  }

  private class IsDeviceAvailable extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      String msg = intent.getAction();
      ArrayList<String> availableDevices = intent.getStringArrayListExtra("available_devices");
      if (BluetoothLeManager.DEVICE_CONNECTED.equals(msg)) {
        Log.i("Intent","Intent Available devices: "+availableDevices.toString());
        if (availableDevices.contains(device.getAddress())) {
          connectionStatus.setText(R.string.connected_live_mode);
          progressBar.setVisibility(View.INVISIBLE);
          BluetoothLeManager.getInstance(context).stopBTSearch();
          btnReconnect.setVisibility(View.INVISIBLE);
          //Added
//          liveModeTimer.setVisibility(View.VISIBLE);
          liveModeCountdown.cancel();
//          liveModeTimer.setText("Connected\n"+device.getName());
//          liveModeTimer.setText("Updating\nBattery SoC");
          DeviceManagerHiQ.getInstance().liveModeUpdateBattery(device.getAddress());
        }
      }
    }
  }
} // EOClass DeviceDetailsActivity
