package com.ctek.sba.bluetooth;

import android.content.Context;
import android.util.Log;

import com.ctek.sba.R;
import com.ctek.sba.soc.Calculate;
import com.ctek.sba.soc.SoCData;

import org.junit.Test;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by evgeny.akhundzhanov on 27.09.2016.
 * Note: Original buffer size in test is 5*12*2500 = 15000, while number of points is 30000.
 @RunWith(PowerMockRunner.class)
 @PrepareForTest({Log.class})
 */

public class CalcSoC_Test1 {

  private final static double EPSILON = 0.001;


  private class PointD {
    double x, y;

    public PointD (double x, double y) {
      this.x = x;
      this.y = y;
    }
  }


  private List<Double> makeVoltageList (List<PointD> points) {
    List<Double> voltageList = new ArrayList<>();
    for(PointD point : points) {
      voltageList.add((double) point.x);
    }
    return voltageList;
  }

  private static boolean isClose (Double testValue, Double estSoC) {
    return Math.abs(testValue - estSoC) <= EPSILON;
  }

  @Test
  public void test_90_flag_green () {

    List<PointD> points = new ArrayList<>();
    points.add(new PointD(12.673, 87.594));
    points.add(new PointD(12.671, 87.438));
    points.add(new PointD(12.669, 87.282));
    points.add(new PointD(12.664, 86.892));

    List<Double> m_voltageList = makeVoltageList(points);

    Calculate calcSoc = new Calculate(points.size(), null);
    SoCData.StateFlag flag = calcSoc.testVoltageList(m_voltageList);
    assertThat(flag == SoCData.StateFlag.FLAG_GREEN, is(true));

    SoCData[] soc_data = calcSoc.getSoCData();
    int ii = 0;
    for(PointD point : points) {
      assertThat(isClose(point.y, soc_data[ii++].d_estSoC), is(true));
    }
  }

  @Test
  public void test_90_flag_yellow () {
    List<PointD> points = new ArrayList<>();
    points.add(new PointD(12.057, 60.255));
    points.add(new PointD(11.0, 0));
    points.add(new PointD(10.0, 0));
    points.add(new PointD(9.5, 0));

    Calculate calcSoc = new Calculate(points.size(), null);
    SoCData.StateFlag flag = calcSoc.testVoltageList(makeVoltageList(points));
    SoCData[] soc_data = calcSoc.getSoCData();
    assertThat(flag == SoCData.StateFlag.FLAG_YELLOW, is(true));
  }

  @Test
  public void test_90_flag_red () {
    List<PointD> points = new ArrayList<>();
    points.add(new PointD(10.0, 0));
    points.add(new PointD(8.0, 0));
    points.add(new PointD(6.0, 0));
    points.add(new PointD(4.0, 0));

    Calculate calcSoc = new Calculate(points.size(), null);
    SoCData.StateFlag flag = calcSoc.testVoltageList(makeVoltageList(points));
    SoCData[] soc_data = calcSoc.getSoCData();
    assertThat(flag == SoCData.StateFlag.FLAG_RED, is(true));
  }


  @Test
  public void test_90_curve_1 () {
    List<PointD> points = new ArrayList<>();
    points.add(new PointD(12.00, 0));
    points.add(new PointD(12.02, 0));
    points.add(new PointD(12.04, 0));
    points.add(new PointD(12.06, 0));
    points.add(new PointD(12.08, 0));
    points.add(new PointD(12.10, 0));
    points.add(new PointD(12.08, 0));
    points.add(new PointD(12.06, 0));
    points.add(new PointD(12.04, 0));
    points.add(new PointD(12.02, 0));
    points.add(new PointD(12.00, 0));

    Calculate calcSoc = new Calculate(points.size(), null);
    SoCData.StateFlag flag = calcSoc.testVoltageList(makeVoltageList(points));
    SoCData[] soc_data = calcSoc.getSoCData();
    assertThat(flag == SoCData.StateFlag.FLAG_GREEN, is(true));

  }

  /*
  @Test
  public void test_91_dataset_1 () {

    List<PointD> points = new ArrayList<>();
    // First 20 values from C#_Input_9_1_105Ah.txt
    points.add(new PointD(12.86096, 100.));
    points.add(new PointD(12.31763, 100.));
    points.add(new PointD(12.32815, 100.));
    points.add(new PointD(12.33108, 100.));
    points.add(new PointD(12.32815, 100.));
    points.add(new PointD(12.32172, 99.82123525));
    points.add(new PointD(12.31763, 99.44492035));
    points.add(new PointD(12.30769, 98.91837759));
    points.add(new PointD(12.29074, 98.09123855));
    points.add(new PointD(12.27905, 97.02501004));
    points.add(new PointD(12.26385, 95.8751007));
    points.add(new PointD(12.24865, 94.74013458));
    points.add(new PointD(12.23637, 93.59200958));
    points.add(new PointD(12.21591, 92.34203705));
    points.add(new PointD(12.20129, 91.04199641));
    points.add(new PointD(12.182, 89.62230507));
    points.add(new PointD(12.16797, 88.32140254));
    points.add(new PointD(12.14634, 86.89691196));
    points.add(new PointD(12.1288, 85.45634112));
    points.add(new PointD(12.10776, 83.88428965));

    List<Double> m_voltageList = makeVoltageList(points);

    Calculate calcSoc = new Calculate(points.size(), null);
    calcSoc.setBatterySize(105.);
    SoCData.StateFlag flag = calcSoc.testVoltageList(m_voltageList);
    assertThat(flag == SoCData.StateFlag.FLAG_GREEN, is(true));

    SoCData[] soc_data = calcSoc.getSoCData();
    int ii = 0;
    for(PointD point : points) {
      assertThat(isClose((double)point.y, soc_data[ii++].d_estSoC), is(true));
    }
  }
  */

  @Test
  public void test_92_dataset_1 () {

    // First 20 values from C#_Input_9_2_105Ah.txt
    List<PointD> points = new ArrayList<>();
    points.add(new PointD(12.86096, 100.));
    points.add(new PointD(12.31763, 100.));
    points.add(new PointD(12.32815, 100.));
    points.add(new PointD(12.33108, 100.));
    points.add(new PointD(12.32815, 100.));
    points.add(new PointD(12.32172, 99.82123525));
    points.add(new PointD(12.31763, 99.44492035));
    points.add(new PointD(12.30769, 98.91837759));
    points.add(new PointD(12.29074, 98.09123855));
    points.add(new PointD(12.27905, 97.02501004));
    points.add(new PointD(12.26385, 95.8751007));
    points.add(new PointD(12.24865, 94.74013458));
    points.add(new PointD(12.23637, 93.59200958));
    points.add(new PointD(12.21591, 92.34203705));
    points.add(new PointD(12.20129, 91.04199641));
    points.add(new PointD(12.182,   89.62230507));
    points.add(new PointD(12.16797, 88.32140254));
    points.add(new PointD(12.14634, 86.89691196));
    points.add(new PointD(12.1288,  85.45634112));
    points.add(new PointD(12.10776, 83.88428965));

    List<Double> m_voltageList = makeVoltageList(points);

    Calculate calcSoc = new Calculate(points.size(), null);
    calcSoc.setBatterySize(105.);
    SoCData.StateFlag flag = calcSoc.testVoltageList(m_voltageList);
    assertThat(flag == SoCData.StateFlag.FLAG_GREEN, is(true));

    SoCData[] soc_data = calcSoc.getSoCData();
    int ii = 0;
    for(PointD point : points) {
      assertThat(isClose((double)point.y, soc_data[ii++].d_estSoC), is(true));
    }
  }


} // EOClass CalcSoC_Test1
