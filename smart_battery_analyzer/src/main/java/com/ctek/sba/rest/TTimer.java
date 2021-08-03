package com.ctek.sba.rest;

/**
 * Created by evgeny.akhundzhanov on 28.08.2017.
 */

import java.util.Calendar;

public class TTimer {

  private	long	msecsStart;
  private	long	msecsFinal;
  private	long	msecsTotal;

  public TTimer () {
    start();
  }

  public void start () {
    msecsStart = msecsFinal = Calendar.getInstance().getTimeInMillis();
    msecsTotal = 0;
  }

  public void stop () {
    msecsFinal = Calendar.getInstance().getTimeInMillis();
    msecsTotal = msecsFinal - msecsStart;
  }


  public long	getStartMSecs () { return msecsStart; }
  public long	getFinalMSecs () { return msecsFinal; }
  public long	getMillis     () { return msecsTotal; }
  public int	getSeconds    () { return (int)(msecsTotal/1000); }

} // EOClass TTimer
