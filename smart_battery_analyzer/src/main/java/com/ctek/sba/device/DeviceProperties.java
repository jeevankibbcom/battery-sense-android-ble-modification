package com.ctek.sba.device;

/**
 * Created by evgeny.akhundzhanov on 04.10.2016.
 */
public class DeviceProperties {

  public static final int  MIN_CAPACITY  =   5;
  public static final int  MAX_CAPACITY  = 200;
  public static final int  CAPACITY_RANGE = 195;

  public static final float  CAPACITY_SMALL  = 15.f;
  public static final float  CAPACITY_NORMAL = 75.f;

  public static final int    I_CAPACITY_SMALL  = 15;
  public static final int    I_CAPACITY_NORMAL = 75;



  public static final float  DFLT_CAPACITY = CAPACITY_NORMAL;
  public static final float  DFLT_CURRTEMP =  21.0f;

  public static final long  DFLT_MSECS_ADDED  = 0;
  public static final long  DFLT_updatedTime  = 1;    // dummy time, 0 is bad for devices which has updated=null.


  double  dCapacity;  // Battery size in ampere hours
  double  dCurrTemp;  // Current temperature in Celsius degrees.

  long    msecsAdded; // time when device was added


  long    updatedTimeWhenNotified_NOT_SYNCED  = 0;
  long    updatedTimeWhenNotified_BAT_STATUS  = 0;

  public DeviceProperties () {
    this(DFLT_CAPACITY, DFLT_CURRTEMP, System.currentTimeMillis(), DFLT_updatedTime, DFLT_updatedTime);
  }

  public DeviceProperties (double dCapacity, double dCurrTemp, long msecsAdded, long updatedTimeWhenNotified_BAT_STATUS, long updatedTimeWhenNotified_NOT_SYNCED) {
    this.dCapacity = dCapacity;
    this.dCurrTemp = dCurrTemp;
    this.msecsAdded = msecsAdded;
    this.updatedTimeWhenNotified_BAT_STATUS = updatedTimeWhenNotified_BAT_STATUS;
    this.updatedTimeWhenNotified_NOT_SYNCED = updatedTimeWhenNotified_NOT_SYNCED;
  }

} // EOClass DeviceProperties
