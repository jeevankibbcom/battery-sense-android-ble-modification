package com.ctek.sba.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.ctek.sba.R;
import com.ctek.sba.util.SecurityText;

public class LabelingActivity extends BaseActivity {

  public static void start (Context ctx) {
    Intent i_ = new Intent(ctx, LabelingActivity.class);
    ctx.startActivity(i_);
  }


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_labeling);

    TextView fccStatementHeader = (TextView) findViewById(R.id.fcc_statement_header);
    fccStatementHeader.setText(Html.fromHtml(SecurityText.fccStatementHeader));

    TextView fccPart15Warning = (TextView) findViewById(R.id.fcc_part15_warning);
    fccPart15Warning.setText(Html.fromHtml(SecurityText.fccPart15WarningEn));

    TextView fccPart15Note = (TextView) findViewById(R.id.fcc_part15_Note);
    CharSequence part15Note = Html.fromHtml(SecurityText.fccPart15ClassBNoteEn);

    //Remove back button visiablity this page only
    ImageView actionBarBack = findViewById(R.id.img_back);
    actionBarBack.setVisibility(View.INVISIBLE);

    Integer gapWidth = 10;
    SpannableString s1 = new SpannableString(SecurityText.fccPart15ClassBBullet_1 + "\n");
    s1.setSpan(new BulletSpan(gapWidth), 0, SecurityText.fccPart15ClassBBullet_1.length(), 0);
    SpannableString s2 = new SpannableString(SecurityText.fccPart15ClassBBullet_2 + "\n");
    s2.setSpan(new BulletSpan(gapWidth), 0, SecurityText.fccPart15ClassBBullet_2.length(), 0);
    SpannableString s3 = new SpannableString(SecurityText.fccPart15ClassBBullet_3 + "\n");
    s3.setSpan(new BulletSpan(gapWidth), 0, SecurityText.fccPart15ClassBBullet_3.length(), 0);
    SpannableString s4 = new SpannableString(SecurityText.fccPart15ClassBBullet_4);
    s4.setSpan(new BulletSpan(gapWidth), 0, SecurityText.fccPart15ClassBBullet_4.length(), 0);
    part15Note = TextUtils.concat(part15Note, s1);
    part15Note = TextUtils.concat(part15Note, s2);
    part15Note = TextUtils.concat(part15Note, s3);
    part15Note = TextUtils.concat(part15Note, s4);

    fccPart15Note.setText(part15Note);
//    fccPart15Note.setText(Html.fromHtml(SecurityText.fccPart15ClassBNoteEn));

    TextView rss210notice = (TextView) findViewById(R.id.rss210_notice);
    rss210notice.setText(Html.fromHtml(SecurityText.rss210TextEn));

    /*
String[] items = new String[] { "item 1", "item 2", "item 3" };
CharSequence allText = "";
for (int i = 0; i < items.length; i++)
{
    String text = items[i];
    SpannableString s = new SpannableString(text + "\n");
    s.setSpan(new BulletSpan(BulletSpan.STANDARD_GAP_WIDTH), 0, text.length(), 0);
    allText = TextUtils.concat(allText, s);
}     */
  }
  public void back(View view) {
    finish();
  }
}
