package com.ctek.sba.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.ctek.sba.R;

public class BatteryView extends View {

  public enum Color {
    UNDEFINED,
    RED,
    ORANGE,
    GREEN
  }

  private final Drawable battery_tra;
  private final Drawable battery_red;
  private final Drawable battery_yel;
  private final Drawable battery_gre;
  //Added
  private final Drawable battery_whi;
  private Color color;

  public BatteryView(Context context) {
    this(context, null);
  }

  public BatteryView(Context context, AttributeSet attrs) {

    super(context, attrs);

    this.color = Color.UNDEFINED;

    battery_tra = getResources().getDrawable(R.drawable.battery_trans);
    battery_red = getResources().getDrawable(R.drawable.battery_red);
    battery_yel = getResources().getDrawable(R.drawable.battery_yellow);
    battery_gre = getResources().getDrawable(R.drawable.battery_green);
    battery_whi = getResources().getDrawable(R.drawable.battery_white);

  }

  /**
   * Determines core height
   * From 0 to 1
   */
  public void setLevel(float level) {
      invalidate();
  }

  public void setCoreColor(Color color) {
    this.color = color;
    invalidate();
  }

  /**
   * This is used to animate core
   * 0 means core have height 0, 1 means core have height according to current level
   * From 0 to 1
   */
  private void setCoreScale(float coreScale) {
    invalidate();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int width = resolveSize(battery_gre.getIntrinsicWidth(), widthMeasureSpec);
    final int height = resolveSize(battery_gre.getIntrinsicHeight(), heightMeasureSpec);

    setMeasuredDimension(width, height);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    int width = right - left;
    int height = bottom - top;

    battery_whi.setBounds(0, 0, width, height);  //Original battery_tra
    battery_red.setBounds(0, 0, width, height);
    battery_yel.setBounds(0, 0, width, height);
    battery_gre.setBounds(0, 0, width, height);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    switch(color) {
      case GREEN:   battery_gre.draw(canvas); break;
      case ORANGE:  battery_yel.draw(canvas); break;
      case RED:     battery_red.draw(canvas); break;
      default:      battery_whi.draw(canvas); break;  //Original battery_tra
    }
    return;
  }

} // EOClass BatteryView
