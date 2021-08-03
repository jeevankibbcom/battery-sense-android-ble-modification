package com.ctek.sba.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.ctek.sba.R;

import com.ctek.sba.bluetooth.BluetoothLeManager;
import com.ctek.sba.services.CTEKUpdateServiceHost;
import com.ctek.sba.util.SettingsHelper;

public class SplashScreenActivity extends Activity {

  private static final long SPLASH_DELAY = 500;

  private Handler handler = new Handler();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash);

    if(SettingsHelper.getBackgroundUpdate(getApplicationContext())) {
      CTEKUpdateServiceHost.initInstance(getApplicationContext());

      //Added
     // BluetoothLeManager.getInstance(this).startBTSearch();
      ///
    }
    return;
  }

  @Override
  protected void onResume() {
    super.onResume();

    // TEST only. // SettingsHelper.setDataCollectionNotSet(this);

    //Added
   // BluetoothLeManager.getInstance(this).startBTSearch();
    ///

    if(SettingsHelper.isDataCollectionSet(this)) {
      handler.postDelayed(runNextScreen, SPLASH_DELAY);
    }
    else {
      handler.postDelayed(runDataCollectionScreen, SPLASH_DELAY);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    handler.removeCallbacks(runDataCollectionScreen);
  }


  private TextView btnYes;
  private TextView btn_No;
  private CheckBox chkAgree;
  private TextView txtText;

  public final static String href_pattern = "<p>_PREF_<a href='_LINK_'>_TEXT_</a>_POST_</p>";

  public static String getHtmlHref (String prefix, String text, String link, String postfix) {
    return href_pattern.replace("_TEXT_", text).replace("_LINK_", link).replace("_PREF_", prefix).replace("_POST_", postfix);
  }

  public static String getHtmlHref (String text, String link) {
    return getHtmlHref("", text, link, "");
  }

  private Runnable runDataCollectionScreen = new Runnable() {
    public void run() {

      setContentView(R.layout.activity_data_collection);

      btnYes = (TextView) findViewById(R.id.button_yes);
      btn_No = (TextView) findViewById(R.id.button_no);

      btnYes.setOnClickListener(mOnClick);
      btn_No.setOnClickListener(mOnClick);

      chkAgree = (CheckBox) findViewById(R.id.chk_agree);
      chkAgree.setOnCheckedChangeListener(mOnCheckedChangeListener);

      btnYes.setBackgroundColor(getResources().getColor(R.color.ctek_text_grey));
      btn_No.setBackgroundColor(getResources().getColor(R.color.ctek_text_grey));

      txtText = (TextView)findViewById(R.id.text);

      String policy_text = getString(R.string.data_collection_policy);
      String terms_text = getString(R.string.data_collection_terms);

      String policy_link = getString(R.string.link_privacy_policy);
      String terms_link = getString(R.string.link_terms);

      String html = getString(R.string.data_collection_text);
      html = html
          .replace("\n", "<BR>")
          .replace("__LINK_POLICY__", getHtmlHref(policy_text, policy_link))
          .replace("__LINK_TERMS__", getHtmlHref(terms_text, terms_link))
      ;
      txtText.setText(Html.fromHtml(html));
      // txtText.setMovementMethod(LinkMovementMethod.getInstance()); // doesn't work as expected
      txtText.setMovementMethod(new SplashMovementMethod(SplashScreenActivity.this));
      return;
    }
  };

  private OnCheckedChangeListener mOnCheckedChangeListener = new OnCheckedChangeListener() {
    public void onCheckedChanged(CompoundButton chk, boolean isChecked) {
      if(chk==chkAgree)	{
        chkAgree.setChecked(isChecked);
        btnYes.setBackgroundColor(getResources().getColor(isChecked ? R.color.ctek_orange : R.color.ctek_text_grey));
        btn_No.setBackgroundColor(getResources().getColor(isChecked ? R.color.ctek_orange : R.color.ctek_text_grey));
      }
    }
  };


  private View.OnClickListener mOnClick = new View.OnClickListener() {
    @Override
    public void onClick(View btn) {
      if(!chkAgree.isChecked()) return;
      if(btn==btnYes) {
        SettingsHelper.setDataCollection(SplashScreenActivity.this, true);
      }
      else if(btn==btn_No) {
        SettingsHelper.setDataCollection(SplashScreenActivity.this, false);
      }

      nextScreen(0);
    }
  };

  private Runnable runNextScreen = new Runnable() {
    @Override
    public void run() {
      DeviceListActivity.start(SplashScreenActivity.this);
      SplashScreenActivity.this.finish();
    }
  };

  private void nextScreen (long msecs) {
    handler.postDelayed(runNextScreen, msecs);
  }

} // EOClass
