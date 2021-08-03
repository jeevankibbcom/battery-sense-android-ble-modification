package com.ctek.sba.ui;

import android.Manifest;
import  android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.AlertDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ctek.sba.R;
import com.ctek.sba.bluetooth.DeviceList;
import com.ctek.sba.bluetooth.DeviceManager;
import com.ctek.sba.io.dao.DeviceRepository;
import com.ctek.sba.ui.adapter.BluetoothDevicesListAdaptor;
import com.ctek.sba.util.BluetoothHelper;
import com.ctek.sba.util.DialogHelper;
import com.ctek.sba.widget.DeactivateableViewPager;
import com.ctek.sba.widget.font.CtekTextView;
import de.greenrobot.event.EventBus;
import greendao.Device;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class NewDeviceActivity extends BaseActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
  private static final String CB_SHOW_LEGAL = "showLegal";
  private static final int REQUEST_CAMERA_PERMISSION = 201;
  public static void start (Context ctx, boolean bShowLegal) {
    Intent i_ = new Intent(ctx, NewDeviceActivity.class);
    i_.putExtra(CB_SHOW_LEGAL, bShowLegal);
    ctx.startActivity(i_);
  }

  private static final String TAG = NewDeviceActivity.class.getName();

  private DeactivateableViewPager mViewPager;
  private NewDevicePager mViewPagerAdapter;
  private RadioGroup mPageIndicator;
  private ViewPager.OnPageChangeListener mPageChangeListener;
  private BroadcastReceiver mScanResultsReceiver;
  private TextWatcher mInputValidator;
  private EditText mDeviceCodeInputField;
  private boolean mInLostCodeFlow = false;
  private RelativeLayout mPagerLayout;
  private LostSenderController lostSenderController;
  private String discoveredSerial;

  private View connect_progress;

  private CtekTextView btnConnect;
  protected static final int MAX_SERIAL_LENGTH = 11;

  private boolean mShowLegalInfo = true;

  public NewDeviceActivity() {
    mPageChangeListener = new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageSelected(int i) {
        if (mViewPagerAdapter.isDeviceCodePage(i)) {
          mPageIndicator.setVisibility(View.INVISIBLE);
          mDeviceCodeInputField = (EditText) findViewById(R.id.device_code_input);
          mDeviceCodeInputField.getText().toString();
          mDeviceCodeInputField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_SERIAL_LENGTH)});
          mDeviceCodeInputField.addTextChangedListener(mInputValidator);
          btnConnect = (CtekTextView) findViewById(R.id.connect_button);
          btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToBonding();
            }
          });
        } else {
          ((CompoundButton) mPageIndicator.getChildAt(i)).setChecked(true);
          mPageIndicator.setVisibility(View.VISIBLE);
        }
      }

      @Override
      public void onPageScrolled(int i, float v, int i2) {
      }

      @Override
      public void onPageScrollStateChanged(int state) {
        if (state == ViewPager.SCROLL_STATE_IDLE) {
          if (mShowLegalInfo) {
            showLegal();
            mShowLegalInfo = false;
          }
        }
      }
    };


    mInputValidator = new TextWatcher() {

      private String buffer;
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }
      private boolean validateBuffer() {
        buffer = buffer.replaceAll(" ", "");
        if (buffer.length() > MAX_SERIAL_LENGTH) {
          buffer = buffer.substring(0, MAX_SERIAL_LENGTH);
        }
        return buffer.length() == MAX_SERIAL_LENGTH;
      }

      @Override
      public void afterTextChanged(Editable e) {
        EditText field = mDeviceCodeInputField;
        buffer = e.toString();
        if (validateBuffer()) {
          field.setTextColor(getResources().getColor(R.color.black));
          enableConnect(true);
        } else {
          field.setTextColor(getResources().getColor(R.color.ctek_red));
          enableConnect(false);
        }
        if (!buffer.equals(e.toString())) {
          field.setText(buffer);
          field.setSelection(buffer.length());
        }
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
    };
  }

  private void enableConnect (boolean enable) {
  }

  public void onEventMainThread(DeviceManager.PairingComplete event) {
    // Stop the device manager
    DeviceManager.getInstance().stop();
    EventBus.getDefault().unregister(this);
    if (event.device != null) {
      Log.d(TAG, "ACTION_DEVICE_ADDED " + event.device.getAddress());
      saveDeviceAndGoToEdit(event.device);
    }
    else {
      mViewPager.setVisibility(VISIBLE);
      connect_progress.setVisibility(INVISIBLE);

      String msg = getResources().getString(R.string.failed_to_connect_message)+"\n\n"+getResources().getString(R.string.if_paired_unpair)+". \n\n"+getResources().getString(R.string.properly_connected)+".";

      DialogHelper.showOk(context, R.string.failed_to_connect, msg);
    }
  }

  private void showLegal() {
    LabelingActivity.start(this);
  }
  private Context context;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_new_device);
    context = this;
    mViewPager = (DeactivateableViewPager) findViewById(R.id.pager);
    mViewPagerAdapter = new NewDevicePager(getSupportFragmentManager());
    mViewPager.setAdapter(mViewPagerAdapter);
    lostSenderController = new LostSenderController(this, findViewById(R.id.lost_sender_layout));
    lostSenderController.setOnSuccessListener(onLostSenderSuccess);
    lostSenderController.setFoundDevicesListener(mDeviceFoundListener);
    mPagerLayout = (RelativeLayout) findViewById(R.id.pager_layout);
    mPageIndicator = (RadioGroup) findViewById(R.id.onboarding_page_indicator_radio_group);
    for (int i = 0; i < mViewPagerAdapter.getCount(); i++) {
      LayoutInflater.from(NewDeviceActivity.this).inflate(R.layout.component_page_indicator, mPageIndicator);
    }
    ((CompoundButton) mPageIndicator.getChildAt(0)).setChecked(true);
    mViewPager.setOnPageChangeListener(mPageChangeListener);
    // Get the connect progress view
    connect_progress = findViewById(R.id.connect_progress);
    // Ensure it is not visible initially
    connect_progress.setVisibility(INVISIBLE);
    // Find out if the legal info shall be shown
    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      mShowLegalInfo = extras.getBoolean(CB_SHOW_LEGAL);
    }
    else {
      mShowLegalInfo = false;
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  public void onResume() {
    super.onResume();
  }

  @Override
  protected void onUserLeaveHint() {
    super.onUserLeaveHint();
    if (mScanResultsReceiver != null) {
      try {
        unregisterReceiver(mScanResultsReceiver);
      } catch (Exception ignored) {
      }
    }
    DeviceManager.getInstance().stop();
    EventBus.getDefault().unregister(this);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "New device activity destroyed");
    if (mScanResultsReceiver != null) {
      try {
        unregisterReceiver(mScanResultsReceiver);
      } catch (Exception ignored) {
      }
    }
    DeviceManager.getInstance().stop();
    EventBus.getDefault().unregister(this);
  }

  @Override
  public void onBackPressed() {
    if(mInLostCodeFlow) {
      exitLostCodeFlow();
    }
    else if (connect_progress.getVisibility() == View.VISIBLE) {
      mViewPager.setVisibility(VISIBLE);
      connect_progress.setVisibility(View.INVISIBLE);
      DeviceManager.getInstance().stop();
      EventBus.getDefault().unregister(this);
  } else {
      finish();
    }
  }

  public void back(View view) {
    if(mInLostCodeFlow) {
      exitLostCodeFlow();
    }
  }

  public void exitLostCodeFlow() {
    mInLostCodeFlow = false;
    mPagerLayout.setVisibility(View.VISIBLE);
    lostSenderController.hide();
    lostSenderController.stop();
  }

  public void onLostCode(View view) {
    if (BluetoothHelper.getInstance().checkBluetoothAdapterActive(this, BluetoothHelper.SHOW_DIALOG_YES)) {
      mInLostCodeFlow = true;
      lostSenderController.show();
      mPagerLayout.setVisibility(View.GONE);
    }
  }

  public void onScanBarcode(View view) {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
      Intent intent = new Intent(this, BarCodeScannerActivity.class);
      startActivityForResult(intent, 1);
    } else {
      ActivityCompat.requestPermissions(this, new
          String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
     }
  }

  public void goToBonding() {
    String serial = getSerialNumber();
    if (serial.equals(getString(R.string.add_device_code_prefix))) {
      showAlertWithMessage("Please enter sender ID.");
    }
    else if (serial.equals(getString(R.string.demo_device_code))) {
     // do not check this number
      showAlertWithMessage("Please enter valid sender ID.");
    }
    else if (serial.length() != 16) {
      showAlertWithMessage("Please enter valid sender ID");
    }
    else {
      Device device = DeviceRepository.getDeviceForSerialNumber(this, serial);
      if(device!=null) {
        // CBS-31 Android - it is possible to add same device more than once.
        Log.d(TAG, "Device serial = " + serial + " is registered already.");
        showAlreadyConnectedDialog();
        return;
      }
      if (BluetoothHelper.getInstance().checkBluetoothAdapterActive(this, BluetoothHelper.SHOW_DIALOG_YES)) {
        mViewPager.setVisibility(INVISIBLE);
        connect_progress.setVisibility(VISIBLE);
        startBondingProcess();
      }
    }
  }

  private void showAlreadyConnectedDialog() {
    new AlertDialog.Builder(this)
        .setTitle(R.string.information)
        .setMessage(R.string.sender_already_connected)
        .setPositiveButton(R.string.ok, null)
        .show();
  }

  private void showAlertWithMessage(String msg) {
    new AlertDialog.Builder(this)
        .setTitle("Alert")
        .setMessage(msg)
        .setPositiveButton(R.string.ok, null)
        .show();
  }

  public void goToConnecting() {
    mViewPager.setVisibility(INVISIBLE);
    connect_progress.setVisibility(VISIBLE);
    startBondingProcess();
  }

  public void startBondingProcess() {
    if (BluetoothHelper.getInstance().checkBluetoothAdapterActive(this, BluetoothHelper.SHOW_DIALOG_YES)) {
      Log.d(TAG, "Starting bonding process");
      String serial = getSerialNumber();
      if (serial == null)
        return;
      // IF serial is al zeros ,do the demo
      if (serial.equals(getString(R.string.demo_device_code))) {
        Device device = new Device();
        device.setAddress("");
        device.setSerialnumber(serial);
        device.setName("");
        device.setRssi(0);
        saveDeviceAndGoToEdit(device);
      } else {
        EventBus.getDefault().register(this);
        Device dev = deviceList.getDevice(getSerialNumber());
        DeviceManager.getInstance().bondDeviceWithMac(dev);
      }
    }
    else {
      mViewPager.setCurrentItem(3, true);
    }
  }

  private String getSerialNumber() {
    final String serial;
    if (discoveredSerial != null) {
      serial = discoveredSerial;
    } else {
      EditText edit = (EditText) findViewById(R.id.device_code_input);
      TextView tvSerial = (TextView) findViewById(R.id.tv_serial);
      if (edit != null) {
        serial = edit.getText().toString().trim();
      } else if (tvSerial.getText() != null) {
        serial = tvSerial.getText().toString();
      }
      else {
        serial = null;
      }
    }
    if (serial != null) {
      return serial;
    } else {
      return null;
    }
  }

  private void saveDeviceAndGoToEdit(Device device) {
    long id = DeviceRepository.insertOrUpdate(this, device);
    EditDeviceActivity.start(this, true, id);
    finish();
  }

  private LostSenderController.OnSuccessListener onLostSenderSuccess = new LostSenderController.OnSuccessListener() {
    public void onSuccess(String serial) {
      if (serial != null) {
        // CBS-31 Android - it is possible to add same device more than once.
        Device device = DeviceRepository.getDeviceForSerialNumber(NewDeviceActivity.this, serial);
        if(device!=null) {
          Log.d(TAG, "Device serial = " + serial + " is registered already.");
          showAlreadyConnectedDialog();
          return;
        }
//        discoveredSerial = serial.replaceFirst(getString(R.string.add_device_code_prefix), "");
        Log.d(TAG,"Found device = "+serial);
        exitLostCodeFlow();
        goToConnecting();
      }
      else {
        lostSenderController.stop();
        //String message = R.string.not_found_recoverable_device+"";

        String msg = getResources().getString(R.string.not_found_recoverable_device)+"\n"+getResources().getString(R.string.if_paired_unpair)+"\n"+getResources().getString(R.string.properly_connected);


        DialogHelper.showOk(context, R.string.no_device_found_lost_code_title, msg);
      }
    }
  };

  private BluetoothDevicesListAdaptor.onConnectClickListener onConnectClickListener = new BluetoothDevicesListAdaptor.onConnectClickListener() {
    public void connect(int pos) {
      Log.d(TAG,"Connect to "+deviceList.getDevice(pos).getAddress());
      DeviceManager.getInstance().reportConnectTo(deviceList.getDevice(pos).getAddress());
    }
  };

  private DeviceList deviceList;
  private LostSenderController.FoundDevicesListener mDeviceFoundListener = mDeviceList -> {
    deviceList = mDeviceList;
    lostSenderController.initRecyclerView(this,onConnectClickListener,mDeviceList);
  };


  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 1) {
      if(resultCode == RESULT_OK) {
        String strEditText = data.getStringExtra("barCode");
        strEditText = strEditText.substring(5);
        mDeviceCodeInputField.setText(strEditText);
      }
    }
  }
  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    switch (requestCode) {
      case REQUEST_CAMERA_PERMISSION: {
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Intent intent = new Intent(this, BarCodeScannerActivity.class);
          startActivityForResult(intent, 1);
          if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
          }
        } else {
        }
        return;
      }
    }
  }
}
