package com.ctek.sba.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import com.ctek.sba.R;
import com.ctek.sba.bluetooth.Notifications;
import com.ctek.sba.soc.Calculate;

import com.ctek.sba.util.SettingsHelper;


public class SettingsActivity extends BaseActivity {

  public static void start (Context ctx) {
    Intent i_ = new Intent(ctx, SettingsActivity.class);
    ctx.startActivity(i_);
  }

  private Switch dataNotif, statusNotif, listViewSwitch, showVoltageSwitch;
  private Switch newChartSwitch; // reserved for test setting
  private Switch dataCollectionSwitch;

  private RadioButton rbCelsius, rbFahrengeit;

  private View legalInfo;
  private View policy;
  private View terms;



  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);

    dataCollectionSwitch = (Switch) findViewById(R.id.data_collection);
    dataNotif = (Switch) findViewById(R.id.notif_data);
    statusNotif = (Switch) findViewById(R.id.notif_status);
    listViewSwitch = (Switch) findViewById(R.id.list_view_switch);

    newChartSwitch = (Switch) findViewById(R.id.new_chart_switch);
    findViewById(R.id.ll_chart_switch).setVisibility(View.GONE);

    showVoltageSwitch = (Switch) findViewById(R.id.show_voltage_switch);
    rbCelsius = (RadioButton) findViewById(R.id.radio_t_celsius);
    rbFahrengeit = (RadioButton) findViewById(R.id.radio_t_fahrengeit);

    Context context = getApplicationContext();

    dataCollectionSwitch.setChecked(SettingsHelper.isDataCollectionEnabled(context));
    dataNotif.setChecked(SettingsHelper.getNotificationData(context));
    statusNotif.setChecked(SettingsHelper.getNotificationStatus(context));
    listViewSwitch.setChecked(SettingsHelper.getDeviceListMode(context) == SettingsHelper.MODE_LIST);
    // newChartSwitch.setChecked(SettingsHelper.getNewChart(context));
    showVoltageSwitch.setChecked(SettingsHelper.getShowVoltage(context));
    boolean bCelsius = SettingsHelper.getCelsius(context);
    rbCelsius.setChecked(bCelsius);
    rbFahrengeit.setChecked(!bCelsius);

    dataCollectionSwitch.setOnCheckedChangeListener(listener);
    dataNotif.setOnCheckedChangeListener(listener);
    statusNotif.setOnCheckedChangeListener(listener);
    listViewSwitch.setOnCheckedChangeListener(listener);
    // newChartSwitch.setOnCheckedChangeListener(listener);
    showVoltageSwitch.setOnCheckedChangeListener(listener);
    rbCelsius.setOnCheckedChangeListener(listener);
    rbFahrengeit.setOnCheckedChangeListener(listener);

    onVoltageSelected();

    try {
    ((TextView) findViewById(R.id.version_label)).setText(String.format("%s %s",
        getString(R.string.app_version),
        getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
    } catch (Exception ignored){}

    try {
      ((TextView) findViewById(R.id.soc_version_label)).setText(String.format("%s %s",
          getString(R.string.soc_version),
          Calculate.SOC_ALRORITHM_VERSION));
    } catch (Exception ignored){}

    // links
    legalInfo = findViewById(R.id.view_legal_info);
    policy = findViewById(R.id.privacy_policy);
    terms = findViewById(R.id.terms_conditions);
    /*
    legalInfo.setOnClickListener(mOnClick);
    policy.setOnClickListener(mOnClick);
    terms.setOnClickListener(mOnClick);
    */

    legalInfo.setOnTouchListener(mOnTouch);
    policy.setOnTouchListener(mOnTouch);
    terms.setOnTouchListener(mOnTouch);
    return;
  }

  @Override
  protected void onResume() {
    super.onResume();
    return;
  }


  private CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {

    public void onCheckedChanged(CompoundButton chk, boolean isChecked) {
      Context context = getApplicationContext(); // EA 27-Oct-2016. Do not touch this! Application context is required in setBackgroundUpdate()!
      switch (chk.getId()) {
      case R.id.data_collection:
        SettingsHelper.setDataCollection(context, isChecked);
        break;

      case R.id.notif_data:
        SettingsHelper.setNotificationData(context, isChecked);
        if (isChecked) {
          Notifications.setNewNotifications(context);
        }
        else {
          Notifications.removePendingNotifications(context);
        }
        break;
      case R.id.notif_status:
        SettingsHelper.setNotificationStatus(context, isChecked);
        if (!isChecked) {
          Notifications.removeLevelNotification(context);
        }
        break;
      case R.id.list_view_switch:
        if (isChecked) {
          SettingsHelper.setDeviceListModeList(context);
        } else {
          SettingsHelper.setDeviceListModePager(context);
        }
        break;
        case R.id.new_chart_switch:
          // SettingsHelper.setNewChart(context, isChecked);
          break;
        case R.id.show_voltage_switch:
          SettingsHelper.setShowVoltage(context, isChecked);
          onVoltageSelected();
          break;
        case R.id.radio_t_celsius:
          if(isChecked) {
            SettingsHelper.setCelsius(context, true);
          }
          break;
        case R.id.radio_t_fahrengeit:
          if(isChecked) {
            SettingsHelper.setCelsius(context, false);
          }
          break;
      }
    }
  };

  private void onVoltageSelected () {
    Context context = getApplicationContext();
    boolean bShowVoltage = SettingsHelper.getShowVoltage(context);
    rbCelsius.setEnabled(bShowVoltage);
    rbFahrengeit.setEnabled(bShowVoltage);
  }

  public void back(View view) {
    finish();
  }

  // Photo picker listener
  final View.OnClickListener mOnClick = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
      Log.d("SettingsActivity", "onClick");
      if  (view==legalInfo) { viewLegal(); }
      else if(view==policy) { openLink(getString(R.string.link_privacy_policy)); }
      else if(view==terms ) { openLink(getString(R.string.link_terms)); }
    }
  };

  View.OnTouchListener mOnTouch = new View.OnTouchListener () {
    @Override
    public boolean onTouch(View view, MotionEvent event) {
      if(event.getAction()==MotionEvent.ACTION_DOWN) {
        Log.d("SettingsActivity", "onTouch");
        if  (view==legalInfo) { viewLegal(); }
        else if(view==policy) { openLink(getString(R.string.link_privacy_policy)); }
        else if(view==terms ) { openLink(getString(R.string.link_terms)); }
        return true;
      }
      return false;
    }
  };

  public void viewLegal() {
    LabelingActivity.start(this);
  }

  public void openLink (String link) {
    SettingsHelper.openExternalPDF(this, link);
  }

} // EOClass SettingsActivity
