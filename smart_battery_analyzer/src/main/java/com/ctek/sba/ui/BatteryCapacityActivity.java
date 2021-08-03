package com.ctek.sba.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ctek.sba.R;
import com.ctek.sba.device.DeviceMap;
import com.ctek.sba.device.DeviceProperties;

/**
 * Created by evgeny.akhundzhanov on 04.10.2016.
 */
public class BatteryCapacityActivity extends BaseActivity {

  // public final String TAG = this.getCass().getSimpleName();

  public static void start (Context ctx, long device_id) {
    Intent i_ = new Intent(ctx, BatteryCapacityActivity.class);
    i_.setAction(Intent.ACTION_VIEW);
    i_.putExtra(EditDeviceActivity.EXTRA_DEVICE_ID, device_id);
    ctx.startActivity(i_);
    return;
  }

  private long    device_id;
  private double  capacity;
  private double  oldCapacity;

  private RadioButton rbSmall, rbNormal;
  private RadioGroup radioGroup;
  private SeekBar seekBar;
  private TextView txtCapacity;

  @Override
  protected void onCreate(Bundle sis) {
    super.onCreate(sis);
    setContentView(R.layout.activity_battery_capacity);

    if(sis==null) {
      sis = getIntent().getExtras();
    }
    device_id = sis.getLong(EditDeviceActivity.EXTRA_DEVICE_ID);
    capacity = DeviceMap.getInstance().getDeviceCapacity(device_id);
    oldCapacity = capacity;

    radioGroup = (RadioGroup) findViewById(R.id.radio_group);

    rbSmall = (RadioButton) findViewById(R.id.radio_small);
    rbNormal = (RadioButton) findViewById(R.id.radio_normal);

    rbSmall .setOnCheckedChangeListener(listener);
    rbNormal.setOnCheckedChangeListener(listener);

    seekBar = (SeekBar) findViewById(R.id.seekBar);
    seekBar.setMax(DeviceProperties.CAPACITY_RANGE);
    // seekBar.setProgressDrawable(new ColorDrawable(getResources().getColor(R.color.light_green)));
    seekBar.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener(){
          @Override public void onStartTrackingTouch(SeekBar seekBar) {}
          @Override public void onStopTrackingTouch(SeekBar seekBar) {}

          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            // Log.d(TAG, "onProgressChanged: " + DeviceProperties.MIN_CAPACITY + progress + " fromUser = " + fromUser);
            if(fromUser) {
              setCapacity(DeviceProperties.MIN_CAPACITY + progress, false, true);
            }
          }
        });

    txtCapacity = (TextView) findViewById(R.id.txtCapacity);

    setCapacity(capacity, true, true);
    return;
  }

  public void back(View view) {
    setCapacity(oldCapacity,true,true);
    finish();
  }

  @Override
  protected void onSaveInstanceState (Bundle sis) {
    super.onSaveInstanceState(sis);
    sis.putLong(EditDeviceActivity.EXTRA_DEVICE_ID, device_id);
    return;
  }

  private CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
    public void onCheckedChanged(CompoundButton chk, boolean isChecked) {
      if      (chk==rbSmall ) {
        // Log.d(TAG, "onCheckedChanged rbSmall isChecked = " + isChecked);
        if(isChecked) setCapacity(DeviceProperties.CAPACITY_SMALL, true, false);
      }
      else if (chk==rbNormal) {
        // Log.d(TAG, "onCheckedChanged rbNormal isChecked = " + isChecked);
        if(isChecked) setCapacity(DeviceProperties.CAPACITY_NORMAL, true, false); }
    }
  };

  private void setCapacity (double newCapacity, boolean bSetSeekBar, boolean bSetRadio) {
    // Log.d(TAG, "updateCapacityUI: bSetSeekBar = " + bSetSeekBar + " bSetRadio = " + bSetRadio);
    capacity = newCapacity;
    DeviceMap.getInstance().setDeviceCapacity(device_id, capacity);

    int iCapacity = (int) Math.round(capacity);

    txtCapacity.setText(String.valueOf(iCapacity) + "Ah");
    if(bSetSeekBar) {
      seekBar.setProgress(iCapacity - DeviceProperties.MIN_CAPACITY);
    }

    boolean isSmall = iCapacity == DeviceProperties.I_CAPACITY_SMALL;
    boolean isNormal =iCapacity == DeviceProperties.I_CAPACITY_NORMAL;
    if(bSetRadio) {
      if(isSmall) rbSmall.setChecked(isSmall);
      else if(isNormal) rbNormal.setChecked(isNormal);
      else radioGroup.clearCheck();
    }

    // Log.d(TAG, "updateCapacityUI: iCapacity = " + iCapacity + " isSmall = " + isSmall + " isNormal = " + isNormal);
    return;
  }

  public void saveBatteryCapacity (View view) {
    setCapacity(capacity, true, true);
    finish();
  }

  @Override
  public void onBackPressed() {
    setCapacity(oldCapacity,true,true);
    finish();
  }
} // EOClass BatteryCapacityActivity
