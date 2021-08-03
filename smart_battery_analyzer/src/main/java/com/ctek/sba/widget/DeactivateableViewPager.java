package com.ctek.sba.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created for CTEK Smart Battery Analyser on 07/05/15.
 * by Martin Kurtsson, martin.kurtsson@screeninteraction.com
 * © Screen Interaction 2015
 */
public class DeactivateableViewPager extends ViewPager {
  public DeactivateableViewPager(Context context) {
    super(context);
  }

  public DeactivateableViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return !isEnabled() || super.onTouchEvent(event);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    return isEnabled() && super.onInterceptTouchEvent(event);
  }
}
