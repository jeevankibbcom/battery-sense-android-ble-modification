package com.ctek.sba.widget;

/**
 * Created by evgeny.akhundzhanov on 19.10.2016.
 * Saved old BatteryView.
 * TO DO: delete this file a bit later if customer requirements do not assume real level "animation".
 */
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.ctek.sba.R;

import static java.lang.Math.max;
import static com.ctek.sba.ui.support.Ui.toPixels;

/**
 * See /docs/battery.png
 * for battery
 */
public class BatteryViewOld extends View {

  public enum Color {
    UNDEFINED,
    RED,
    ORANGE,
    GREEN,
  }

  private static final int RED = 0xFFD0021B;
  private static final int ORANGE = 0xFFFF00; // 0xFFF39125; // CBS-3 Amber -> Yellow.
  private static final int GREEN = 0xFF7ED321;

  private final Drawable battery;

  // This is battery default dimensions
  // All real dimensions of battery are calculated based on this ones
  private final float batteryDefaultWidth;
  private final float batteryDefaultHeight;

  private final float coreDefaultMarginTop;
  private final float coreDefaultMarginBot;
  private final float coreDefaultMarginLeft;
  private final float coreDefaultMarginRight;

  private final float coreDefaultCornerRadius;

  // Core rect when level is 100% (1f)
  private final RectF core = new RectF();
  // Current core rect, when level is applied
  private final RectF currentCore = new RectF();

  private float cornerRadiusX;
  private float cornerRadiusY;

  private float level = 1f;
  private float coreScale = 1f;

  private final Paint corePaint = new Paint();

  public BatteryViewOld(Context context) {
    this(context, null);
  }

  public BatteryViewOld(Context context, AttributeSet attrs) {
    super(context, attrs);

    battery = getResources().getDrawable(R.drawable.battery);
    batteryDefaultWidth = battery.getIntrinsicWidth();
    batteryDefaultHeight = battery.getIntrinsicHeight();

    coreDefaultMarginTop = toPixels(12);
    coreDefaultMarginBot = toPixels(5);
    coreDefaultMarginLeft = toPixels(4.5f);
    coreDefaultMarginRight = toPixels(4.5f);

    coreDefaultCornerRadius = toPixels(0);

    corePaint.setStyle(Paint.Style.FILL);
    corePaint.setColor(GREEN);
  }

  /**
   * Determines core height
   * From 0 to 1
   */
  public void setLevel(float level) {
    if (level < 0) {
      this.level = 0;
    } else if (level > 1) {
      this.level = 1;
    } else {
      this.level = level;
    }
    invalidate();
  }

  public void setCoreColor(Color color) {
    switch (color) {
      case GREEN:
        corePaint.setColor(GREEN);
        break;
      case ORANGE:
        corePaint.setColor(ORANGE);
        break;
      case RED:
        corePaint.setColor(RED);
        break;
    }
    invalidate();
  }

  /**
   * This is used to animate core
   * 0 means core have height 0, 1 means core have height according to current level
   * From 0 to 1
   */
  private void setCoreScale(float coreScale) {
    this.coreScale = coreScale;
    invalidate();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int width = resolveSize(battery.getIntrinsicWidth(), widthMeasureSpec);
    final int height = resolveSize(battery.getIntrinsicHeight(), heightMeasureSpec);

    setMeasuredDimension(width, height);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    int width = right - left;
    int height = bottom - top;

    battery.setBounds(0, 0, width, height);

    float scaleX = width / batteryDefaultWidth;
    float scaleY = height / batteryDefaultHeight;

    float coreMarginTop = scaleY * coreDefaultMarginTop;
    float coreMarginBot = scaleY * coreDefaultMarginBot;
    float coreMarginLeft = scaleX * coreDefaultMarginLeft;
    float coreMarginRight = scaleX * coreDefaultMarginRight;

    cornerRadiusX = coreDefaultCornerRadius * scaleX;
    cornerRadiusY = coreDefaultCornerRadius * scaleY;

    core.set(
        coreMarginLeft,
        coreMarginTop,
        width - coreMarginRight,
        height - coreMarginBot);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    battery.draw(canvas);

    // Adjust core height to match current level
    // Be sure that it is not too small
    // If coreScale not 1f this means that we are
    // in animation, so don't limit it's min height
    float coreHeightScale;
    if (coreScale == 1f) {
      //coreHeightScale = max(0.1f, level * coreScale);
      coreHeightScale = level * coreScale;
    } else {
      coreHeightScale = level * coreScale;
    }

    float coreCurrentHeight = core.height() * coreHeightScale;
    currentCore.set(core);
    currentCore.top = core.bottom - coreCurrentHeight;

    // Never draw too thin line
    if (coreCurrentHeight > 0) {
      canvas.drawRoundRect(currentCore, cornerRadiusX, cornerRadiusY, corePaint);
    }
  }

}
