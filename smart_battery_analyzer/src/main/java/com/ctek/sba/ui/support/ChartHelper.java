package com.ctek.sba.ui.support;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;

import com.ctek.sba.BuildConfig;
import com.ctek.sba.R;
import com.ctek.sba.bluetooth.SBADevice;
import com.ctek.sba.soc.SoCData;
import com.ctek.sba.soc.SoCUtils;
import com.ctek.sba.soc.VoltageSection;
import com.ctek.sba.soc.VoltageSections;
import com.ctek.sba.ui.DeviceDetailsActivity;
import com.ctek.sba.util.SettingsHelper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.PointD;
import com.github.mikephil.charting.utils.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import greendao.Device;
import greendao.Voltage;

/**
 * Created by evgeny.akhundzhanov on 07.10.2016.
 */
public class ChartHelper {

  private Context ctx;
  private String  TAG;
  private int     nPointsSum;

  // CBS-49 Android - New chart algorithm to be set as default.
  private final boolean bNewChartAlgorithm = true; // SettingsHelper.getNewChart(ctx);
  private LineChart mChart;
  private float     scaleXInitial;

  private int moveIndex;


  public ChartHelper (Context ctx, String TAG) {
    this.ctx = ctx;
    this.TAG = TAG;
    nPointsSum = 0;
  }


  // create a dataset and give it a type
  private LineDataSet createLineDataSet (boolean bNewChartAlgorithm, List<Entry> yVals) {
    LineDataSet set1 = new LineDataSet(yVals, "");

    // CBS-2 Change color of sender history graph. Color of line in history graph shall be white (#FFFFFF).

    float width;
    float circleSize;
    int iColor;
    boolean bCircles;
    if(bNewChartAlgorithm) {
      iColor = ctx.getResources().getColor(R.color.ctek_white); // light_green
      width = 2;
      bCircles = true;
      circleSize = 2;
    }
    else {
      iColor = ctx.getResources().getColor(R.color.ctek_white);
      width  = 2;
      bCircles = false;
      circleSize = 2;
    }
    // if(BuildConfig.DEBUG) {
    //  iColor = ctx.getResources().getColor(R.color.light_green);
    //}

    set1.setColor(iColor);
    set1.setCircleColor(iColor);

    set1.setLineWidth(width);
    set1.setCircleSize(circleSize);
    set1.setFillAlpha(65);
    // ### ??? set1.setFillColor(ColorTemplate.getHoloBlue());
    // ### ??? set1.setHighLightColor(Color.rgb(244, 117, 117)); // carrot
    set1.setDrawCircles(bCircles);
    set1.setDrawCubic(false);
    set1.setDrawValues(false);
    return set1;
  }


  private LineDataSet createDummyDataSet (List<Entry> yVals) {
    LineDataSet set0 = new LineDataSet(yVals, "");
    int iColor = ctx.getResources().getColor(android.R.color.transparent);
    if(BuildConfig.DEBUG) {
      iColor = ctx.getResources().getColor(R.color.ctek_yellow);
    }
    set0.setColor(iColor);
    set0.setCircleColor(iColor);
    set0.setCircleSize(BuildConfig.DEBUG ? 3.0f : 0.0f);
    set0.setDrawCircles(BuildConfig.DEBUG);
    set0.setDrawCubic(false);
    set0.setDrawValues(false);
    return set0;
  }

  private boolean needDisplayDate (Calendar prev, Calendar next) {
    if(prev!=null && next!=null) {
      boolean bSameDate =
          (prev.get(Calendar.YEAR) == next.get(Calendar.YEAR)) &&
              (prev.get(Calendar.MONTH) == next.get(Calendar.MONTH)) &&
              (prev.get(Calendar.DAY_OF_MONTH) == next.get(Calendar.DAY_OF_MONTH));
      return !bSameDate;
    }
    return true;
  }

  private SimpleDateFormat sdf1 = new SimpleDateFormat("dd MMM HH:mm");
  private SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm");
  private SimpleDateFormat sdf3 = new SimpleDateFormat("HH:mm:ss");

  public void generateGraph(LineChart theChart, Device device, final int graphType) {
    if (device == null) return;

    //1. set x and y axis
    //2. get graph plot points
    //3. draw graphs

    mChart = theChart;
    List<LineDataSet> dataSets = new ArrayList<>();
    List<String> xVals = new ArrayList<>();
    int xIndex = 0;


    double maxTemp = -0.99;
    double minTemp = -0.99;

    if (graphType == DeviceDetailsActivity.SHOW_TEMPERATURE_GRAPH) {

      VoltageSections sections = SoCUtils.getSocs(device, VoltageSections.SoCCalcMode.ALL_DATA_FOR_CHART);
      if (sections.size() == 0) return;

      // Calendar calPrev = null;
      Log.d(TAG, "Sections " + sections.size() + " +++++++");
      for(int ii=0; ii < sections.size(); ++ ii) {
        VoltageSection section = sections.getSection(ii);
        List<Voltage> list = section.getListVoltage();
        //List<SoCData> socs = section.getListSoCData();
        String line = "Section " + (ii+1) + "/. Points = " + list.size();
        if(section.isDummy()) {
          line += " DUMMY.";
        }
        Log.d(TAG, line);
        List<Entry> yVals = new ArrayList<>();
        for(int kk=0; kk< list.size(); ++kk) {
          Voltage voltage = list.get(kk);

          Long msecs = voltage.getTimestamp();
          // Calendar calNext = Calendar.getInstance();
          // calNext.setTimeInMillis(msecs);
          // boolean bDisplayDate = needDisplayDate(calPrev, calNext);
          // Log.d(TAG, "bDisplayDate " + bDisplayDate);
          // xVals.add(bDisplayDate ? sdf1.format(msecs) : sdf2.format(msecs));
          xVals.add(sdf1.format(msecs));

          double temperatureC = voltage.getTemperature().doubleValue();

          boolean bCelsius = SettingsHelper.getCelsius(ctx);
          double temperature = bCelsius ? temperatureC : SettingsHelper.convertCelcius2Fahrenheit((float)temperatureC);

          if (maxTemp == -0.99 && minTemp == -0.99) {
            maxTemp = temperature;
            minTemp = temperature;
          }



          if (temperature > maxTemp) {
            maxTemp = temperature;
          }
          else  if (temperature < minTemp) {
           minTemp = temperature;
          }



          yVals.add(new Entry((float)temperature , xIndex++));
          // calPrev = calNext;
        }

        LineDataSet dataset = (section.isDummy()) ? createDummyDataSet(yVals) : createLineDataSet(bNewChartAlgorithm, yVals);
        dataSets.add(dataset);
      }
      Log.d(TAG, "Sections " + sections.size() + " =======");



    }
    else if (graphType == DeviceDetailsActivity.SHOW_SOC_GRAPH) {
      VoltageSections sections = SoCUtils.getSocs(device, VoltageSections.SoCCalcMode.ALL_DATA_FOR_CHART);
      if (sections.size() == 0) return;

      // Calendar calPrev = null;
      Log.d(TAG, "Sections " + sections.size() + " +++++++");
      for(int ii=0; ii < sections.size(); ++ ii) {
        VoltageSection section = sections.getSection(ii);
        List<Voltage> list = section.getListVoltage();
        List<SoCData> socs = section.getListSoCData();
        String line = "Section " + (ii+1) + "/. Points = " + list.size();
        if(section.isDummy()) {
          line += " DUMMY.";
        }
        Log.d(TAG, line);
        List<Entry> yVals = new ArrayList<>();
        for(int kk=0; kk< list.size(); ++kk) {
          Voltage voltage = list.get(kk);

          Long msecs = voltage.getTimestamp();
          // Calendar calNext = Calendar.getInstance();
          // calNext.setTimeInMillis(msecs);
          // boolean bDisplayDate = needDisplayDate(calPrev, calNext);
          // Log.d(TAG, "bDisplayDate " + bDisplayDate);
          // xVals.add(bDisplayDate ? sdf1.format(msecs) : sdf2.format(msecs));
          xVals.add(sdf1.format(msecs));
          yVals.add(new Entry((float) socs.get(kk).d_estSoC.doubleValue(), xIndex++));
          // calPrev = calNext;
        }

        LineDataSet dataset = (section.isDummy()) ? createDummyDataSet(yVals) : createLineDataSet(bNewChartAlgorithm, yVals);
        dataSets.add(dataset);
      }
      Log.d(TAG, "Sections " + sections.size() + " =======");

    }
    else if (graphType == DeviceDetailsActivity.SHOW_VOLTAGE_GRAPH) {
      VoltageSections sections = SoCUtils.getSocs(device, VoltageSections.SoCCalcMode.ALL_DATA_FOR_CHART);
      if (sections.size() == 0) return;

      // Calendar calPrev = null;
      Log.d(TAG, "Sections " + sections.size() + " +++++++");
      for(int ii=0; ii < sections.size(); ++ ii) {
        VoltageSection section = sections.getSection(ii);
        List<Voltage> list = section.getListVoltage();
        String line = "Section " + (ii+1) + "/. Points = " + list.size();
        if(section.isDummy()) {
          line += " DUMMY.";
        }
        Log.d(TAG, line);
        List<Entry> yVals = new ArrayList<>();
        for(int kk=0; kk< list.size(); ++kk) {
          Voltage voltage = list.get(kk);

          Long msecs = voltage.getTimestamp();
          // Calendar calNext = Calendar.getInstance();
          // calNext.setTimeInMillis(msecs);
          // boolean bDisplayDate = needDisplayDate(calPrev, calNext);
          // Log.d(TAG, "bDisplayDate " + bDisplayDate);
          // xVals.add(bDisplayDate ? sdf1.format(msecs) : sdf2.format(msecs));
          xVals.add(sdf1.format(msecs));
          yVals.add(new Entry((float) voltage.getValue().doubleValue(), xIndex++));
          // calPrev = calNext;
        }

        LineDataSet dataset = (section.isDummy()) ? createDummyDataSet(yVals) : createLineDataSet(bNewChartAlgorithm, yVals);
        dataSets.add(dataset);
      }
      Log.d(TAG, "Sections " + sections.size() + " =======");
    }




    mChart.setDescription("");
    mChart.getXAxis().setDrawLabels(true);
    mChart.setBackgroundColor(ctx.getResources().getColor(R.color.background));

//    mChart.setValueTextColor(Color.WHITE);

//    mChart.setUnit(getString(R.string.unit_percent));
//    mChart.setDrawUnitsInChart(false);

    // if enabled, the chart will always start at zero on the y-axis
//    mChart.setStartAtZero(true);

    // disable the drawing of values into the chart
//    mChart.setDrawYValues(true);
//    mChart.setValueTextColor(Color.WHITE);

    mChart.setBorderColor(Color.GRAY);
//    mChart.setBorderPositions(new BarLineChartBase.BorderPosition[]{BarLineChartBase.BorderPosition.BOTTOM});

    // // enable / disable grid lines
    // mChart.setDrawVerticalGrid(false);
    // mChart.setDrawHorizontalGrid(false);
    //
    // // enable / disable grid background
    mChart.setDrawGridBackground(false);
//    mChart.linesetGridColor(Color.GRAY);
    //
    mChart.getLegend().setEnabled(false);

    // enable value highlighting
    mChart.setHighlightEnabled(false);

    mChart.setTouchEnabled(true);
    mChart.setDragEnabled(true);
    mChart.setPinchZoom(false); // if set to true, both x and y axis can be scaled with 2 fingers, if false, x and y axis can be scaled separately.
    mChart.setScaleXEnabled(true);
    mChart.setScaleYEnabled(false);

    Typeface tf = Typeface.createFromAsset(ctx.getAssets(), "fonts/Forza-Book.otf");
    mChart.getXAxis().setTypeface(tf);
    mChart.getXAxis().setTextColor(Color.WHITE);
    mChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

    mChart.getAxisLeft().setTypeface(tf);
    mChart.getAxisLeft().setTextColor(Color.WHITE);
    mChart.getAxisLeft().setValueFormatter(new ValueFormatter() {
      public String getFormattedValue(float value) {

        if (graphType == DeviceDetailsActivity.SHOW_TEMPERATURE_GRAPH) {

          boolean bCelsius = SettingsHelper.getCelsius(ctx);

          if (bCelsius) {
            return String.format(Locale.getDefault(),"%.0f℃", value);
          }
          else {
            return String.format(Locale.getDefault(),"%.0f℉", value);
          }


        }
        else if (graphType == DeviceDetailsActivity.SHOW_SOC_GRAPH) {
          return String.format(Locale.getDefault(),"%.0f%%", value);
        }
        else if (graphType == DeviceDetailsActivity.SHOW_VOLTAGE_GRAPH) {
          return String.format(Locale.getDefault(),"%.0fV", value);
        }

        return String.format(Locale.getDefault(),"%.0f%%", value);
      }
    });

    // Unfortunately this does not result in 0% 25% 50% 75% 100%. A bug in chart package.
    mChart.getAxisLeft().setLabelCount(5);

    if (graphType == DeviceDetailsActivity.SHOW_VOLTAGE_GRAPH) {
      mChart.getAxisLeft().setStartAtZero(true);
      mChart.getAxisLeft().setAxisMaxValue(25.0f);
      mChart.getAxisLeft().setAxisMinValue(0.0f);

    }
    else if (graphType == DeviceDetailsActivity.SHOW_TEMPERATURE_GRAPH) {
      mChart.getAxisLeft().setStartAtZero(false);
      mChart.getAxisLeft().setAxisMaxValue((float)(maxTemp+10));
      mChart.getAxisLeft().setAxisMinValue((float)minTemp);

    }
    else {
      mChart.getAxisLeft().setStartAtZero(true);
      mChart.getAxisLeft().setAxisMaxValue(100.0f);
    }

    mChart.getAxisRight().setEnabled(false);

    LineData data = new LineData(xVals, dataSets);
    mChart.setData(data);

    mChart.setDrawMarkerViews(false); // didn't help
    mChart.setHardwareAccelerationEnabled(false);

    mChart.setVisibleYRange(100.f, YAxis.AxisDependency.LEFT);
    if(bNewChartAlgorithm) {
      // CBS-68 Change graph default in portrait to 5 days.
      // boolean bLandscape = (ctx.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
      // final int nVisibleXRange = bLandscape ? 5 * SBADevice.NUMBER_OF_READ_PERIODS_IN_DAY : SBADevice.NUMBER_OF_READ_PERIODS_IN_DAY;
      final int nVisibleXRange = 5 * SBADevice.NUMBER_OF_READ_PERIODS_IN_DAY;
      mChart.setVisibleXRange(nVisibleXRange); // If this is e.g. set to 10, no more than 10 values on the x-axis can be viewed at once without scrolling.
      Log.d(TAG, "nVisibleXRange = " + nVisibleXRange);

      int nPointsTotal = xVals.size();
      if((nPointsTotal != 0) && (nVisibleXRange < nPointsTotal)) {
        float fScaleMinX = ((float)nVisibleXRange) / nPointsTotal;
        Log.d(TAG, "fScaleMinX = " + fScaleMinX);
        mChart.setScaleMinima(fScaleMinX / 2.f, 1.0f);
      }

      nPointsSum = xVals.size();
      moveIndex = (nPointsSum > nVisibleXRange) ? nPointsSum - nVisibleXRange - 1 : 0;
      if(moveIndex > 0) {
        mChart.enableScroll();
        mChart.moveViewToX(moveIndex);
      }

      scaleXInitial = mChart.getScaleX(); // for 5 days
      if((nPointsTotal != 0) && (nPointsTotal < nVisibleXRange)) {
        float nDaysReal = ((float)nPointsTotal) / nVisibleXRange;
        scaleXInitial = scaleXInitial * nDaysReal;
      }
      Log.d(TAG, "scaleXInitial = " + scaleXInitial);
    }
    else {
      moveIndex = 0;
    }

    setNewGestureListener(bNewChartAlgorithm);  // override GestureListener
    return;
  }

  public void moveChartToEnd () {
    if(moveIndex > 0) {
      long downTime = SystemClock.uptimeMillis();
      float xx = 0.0f;
      float yy = 0.0f;
      // List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
      int metaState = 0;
      MotionEvent motionEvent = MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_DOWN, xx, yy, metaState);
      // Dispatch touch event to view
      mChart.dispatchTouchEvent(motionEvent);
    }
    return;
  }

  private void onDoubleTap (MotionEvent me, boolean isNewChartAlgorithm) {
    Log.d(TAG, "onChartDoubleTapped bFirstDoubleTap = " + bFirstDoubleTap);
    float chartWidth = mChart.getXChartMax() - mChart.getXChartMin();
    Log.d(TAG, "Chart width = " + chartWidth);

    final int nPointsOneDay = SBADevice.NUMBER_OF_READ_PERIODS_IN_DAY;

    float xx = me.getX();
    float yy = me.getY();
    PointD point = mChart.getValuesByTouchPoint(xx, yy, YAxis.AxisDependency.LEFT);
    Log.d(TAG, "onChartDoubleTapped: getX = " + xx);
    int actualMoveIndex = 0;
    if(chartWidth > 0.f) {
      double part = point.x / chartWidth; // [0 - 1]
      Log.d(TAG, "getValuesByTouchPoint  X = " + point.x + " --> " + part * 100.f + " %");

      float nDays = ((float)nPointsSum) / ((float)nPointsOneDay);
      float fDay = ((float)part * nDays);
      int nDay = (int) fDay;
      actualMoveIndex = nDay * nPointsOneDay;
      Log.d(TAG, "getValuesByTouchPoint  day = " + nDay + " / " + fDay + " actualMoveIndex = " + actualMoveIndex);
    }

    if(isNewChartAlgorithm) {
      float scaleXRequired = bFirstDoubleTap ? 5*scaleXInitial : scaleXInitial;
      bFirstDoubleTap = !bFirstDoubleTap;

      Log.d(TAG, "scaleXRequired = " + scaleXRequired);
      float scaleX = scaleXRequired / mChart.getScaleX();
      Log.d(TAG, "new ZOOM scaleX = " + scaleX);
      PointF center = mChart.getViewPortHandler().getContentCenter();
      mChart.zoom(scaleX, 1.f, center.x, center.y);
      Log.d(TAG, "new scaleX = " + mChart.getScaleX());

      final int actualMoveIndex2 = (actualMoveIndex!=0) ? (actualMoveIndex + 1) : 0;
      new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          mChart.moveViewToX(actualMoveIndex2);
        }
      }, 100);

      new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          mChart.invalidate();
        }
      }, 200);
    }
    else {
      iScaleX++;
      float scaleX = (iScaleX%2 == 0) ? 2.f : 1.f / mChart.getScaleX(); // always back to 1
      PointF center = mChart.getViewPortHandler().getContentCenter();
      mChart.zoom(scaleX, 1, center.x, center.y);
      mChart.moveViewToX(actualMoveIndex);
      mChart.invalidate();
    }
    return;
  }

  private boolean bFirstDoubleTap = true;
  private int iScaleX = 1;

  private void setNewGestureListener (final boolean isNewChartAlgorithm) {
    final OnChartGestureListener oldGestureListener = mChart.getOnChartGestureListener();
    OnChartGestureListener newGestureListener = new OnChartGestureListener() {


      @Override
      public void onChartLongPressed(MotionEvent me) {
        // Log.d(TAG, "onChartLongPressed");
        // bFirstDoubleTap = true;
        if(oldGestureListener!=null) oldGestureListener.onChartLongPressed(me);
      }

      @Override
      public void onChartDoubleTapped(final MotionEvent me) {
        // if(oldGestureListener!=null) oldGestureListener.onChartDoubleTapped(me);
        /*
        new Handler().postDelayed(new Runnable() {
          @Override
          public void run() {
            onDoubleTap(me, isNewChartAlgorithm);
          }
        }, 0);
        */
        onDoubleTap(me, isNewChartAlgorithm);
      }

      @Override
      public void onChartSingleTapped(MotionEvent me) {
        if(oldGestureListener!=null) oldGestureListener.onChartSingleTapped(me);
      }

      @Override
      public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
        // Log.d(TAG, "onChartFling");
        // bFirstDoubleTap = true;
        if(oldGestureListener!=null) oldGestureListener.onChartFling(me1, me2, velocityX, velocityY);
      }

      @Override
      public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
        Log.d(TAG, "onChartScale scaleX = " + scaleX);
        // bFirstDoubleTap = true;
        if(oldGestureListener!=null) oldGestureListener.onChartScale(me, scaleX, scaleY);
      }

      @Override
      public void onChartTranslate(MotionEvent me, float dX, float dY) {
        // Log.d(TAG, "onChartTranslate");
        // bFirstDoubleTap = true;
        if(oldGestureListener!=null) oldGestureListener.onChartTranslate(me, dX, dY);
      }
    };

    mChart.setOnChartGestureListener(newGestureListener);
    return;
  }




  private LineDataSet createLiveDataSet (List<Entry> yVals) {
    LineDataSet set = new LineDataSet(yVals, "");
    int color = ctx.getResources().getColor(R.color.ctek_yellow); // R.color.ctek_yellow android.R.color.transparent
    set.setColor(color);
    set.setCircleColor(color);
    set.setCircleSize(3.f);
    // set.setDrawCircles(false);
    set.setDrawCubic(false);
    set.setDrawValues(false);
    return set;
  }

  private LineChart mChartLive;
  private int xIndexLive = 0;

  public void generateLive (String TAG, final LineChart mChart) {
    mChartLive = mChart;

    List<LineDataSet> dataSets = new ArrayList<>();
    List<String> xValsLive = new ArrayList<>();

    List<Entry> yVals = new ArrayList<>();
    /*
    long msecs = System.currentTimeMillis() - 11000;

    for(int ii=0;ii<10;++ii) {
      msecs += 1000;
      xValsLive.add(sdf1.format(msecs));
      yVals.add(new Entry(getRandomVoltage().floatValue(), xIndex++));
    }
    */
    xValsLive.add(sdf1.format(System.currentTimeMillis() - 1000));
    yVals.add(new Entry(getRandomVoltage().floatValue(), xIndexLive++));
    LineDataSet dataset = createLiveDataSet(yVals);
    dataSets.add(dataset);

    mChart.setDescription("");
    mChart.getXAxis().setDrawLabels(true);
    mChart.setBackgroundColor(ctx.getResources().getColor(R.color.background));

    mChart.setBorderColor(Color.GRAY);
    mChart.setDrawGridBackground(false);

    mChart.getLegend().setEnabled(false);

    // enable value highlighting
    mChart.setHighlightEnabled(false);

    mChart.setTouchEnabled(true);
    mChart.setDragEnabled(true);
    mChart.setPinchZoom(false); // if set to true, both x and y axis can be scaled with 2 fingers, if false, x and y axis can be scaled separately.
    mChart.setScaleXEnabled(true);
    mChart.setScaleYEnabled(false);

    Typeface tf = Typeface.createFromAsset(ctx.getAssets(), "fonts/Forza-Book.otf");
    mChart.getXAxis().setTypeface(tf);
    mChart.getXAxis().setTextColor(Color.WHITE);
    mChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

    mChart.getAxisLeft().setTypeface(tf);
    mChart.getAxisLeft().setTextColor(Color.WHITE);
    mChart.getAxisLeft().setValueFormatter(new ValueFormatter() {
      public String getFormattedValue(float value) {
        return String.format(Locale.getDefault(),"%.0f V", value);
      }
    });

    // Unfortunately this does not result in 0% 25% 50% 75% 100%. A bug in chart package.
    mChart.getAxisLeft().setLabelCount(5);
    mChart.getAxisLeft().setStartAtZero(true);
    // ### mChart.getAxisLeft().setAxisMaxValue(100.0f);
    // ### mChart.setVisibleYRange(100.f, YAxis.AxisDependency.LEFT);

    mChart.getAxisRight().setEnabled(false);

    LineData data = new LineData(xValsLive, dataSets);
    mChart.setData(data);

    setNewGestureListener(false);
    return;
  }

  public void addLiveEntry (String TAG, Double value) {
    if(mChartLive==null) return; // invalid call
    LineData data = mChartLive.getData();
    if(data== null) return;     // invalid call

    float voltage = (float) value.doubleValue();
    long msecs = System.currentTimeMillis();
    data.addXValue(sdf1.format(msecs));
    data.addEntry(new Entry(voltage, xIndexLive), 0);
    Log.d(TAG, "addLiveEntry: " + xIndexLive + "/. " + sdf3.format(msecs) + " V = " + voltage);
    xIndexLive++;

    data.notifyDataChanged();
    mChartLive.notifyDataSetChanged();  // let the chart know it's data has changed
    mChartLive.setVisibleXRange(10);   // limit the number of visible entries
    // mChart.setVisibleYRange(30, AxisDependency.LEFT);
    mChartLive.moveViewToX(xIndexLive);
    mChartLive.invalidate();
    return;
  }

  public static Double getRandomVoltage () {
    return 12. + Math.random();
  }

} // EOClass ChartHelper
