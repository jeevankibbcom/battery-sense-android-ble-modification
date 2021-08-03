package com.ctek.sba.soc;

/**
 * Created by evgeny.akhundzhanov on 27.09.2016.
 * SoC = state of charge
 */

import java.util.Locale;

/**
 */
public class SoCData {

  public static final double  yellowlimit = 58.0;
  public static final double  redlimit = 35.0;

  public enum StateFlag {
    FLAG_NONE(0),
    FLAG_GREEN(1),
    FLAG_YELLOW(2),
    FLAG_RED(3);

    private final int value;

    StateFlag (int value) {
      this.value = value;
    }

    public int getValue () { return value; }

  };



  public Double d_voltage;
  public Double d_estSoC;
  public double d_temperature;

  // EA 12-Apr-2017. Added in v. 9.1.
  public int    m_lpnr;

  // Debug data
  public int i_loadFlag;
  public int i_restFlag;
  public int i_chargeFlag;
  public int i_nextloadflag;
  public double d_tRest;
  public double d_prevLastTrV;
  public double d_iLoad;

  public SoCData() {
    d_voltage     = 0.0;
    d_estSoC      = 0.0;
    d_temperature = 0.0;

    m_lpnr          = 0;

    // Debug data
    i_loadFlag      = 0;
    i_restFlag      = 0;
    i_chargeFlag    = 0;
    i_nextloadflag  = 0;
    d_tRest         = 0.0;
    d_prevLastTrV   = 0.0;
    d_iLoad         = 0.0;
  }

  public static StateFlag getFlag4Value (double value) {
    if (value >= yellowlimit) {
      return StateFlag.FLAG_GREEN;
    }
    else if (value >= redlimit) {
      return StateFlag.FLAG_YELLOW;
    }
    return StateFlag.FLAG_RED;
  }

  public static StateFlag integer2Flag (int iValue) {
    if(iValue == StateFlag.FLAG_GREEN .ordinal())  return StateFlag.FLAG_GREEN;
    if(iValue == StateFlag.FLAG_YELLOW.ordinal())  return StateFlag.FLAG_YELLOW;
    if(iValue == StateFlag.FLAG_RED   .ordinal())  return StateFlag.FLAG_RED;
    return StateFlag.FLAG_NONE;
  }



  public StateFlag getFlag () {
    return getFlag4Value(d_estSoC);
  }


  @Override
  public String toString() {
    return String.format(Locale.getDefault(),"%.2f", d_estSoC);
  }
}
