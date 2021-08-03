package com.ctek.sba.device;

import android.util.Log;

import com.ctek.sba.bluetooth.CTEK;
import com.ctek.sba.bluetooth.SBADevice;

/**
 * Created by evgeny.akhundzhanov on 02.04.2018.
 *
 * [msecsStart, msecsFinal)
 */

public class TimeInterval {

  private String DELIM = "\n";

  private long msecsStart;
  private long msecsFinal;

  public TimeInterval (long msecsStart, long msecsFinal) {
    this.msecsStart = msecsStart;
    this.msecsFinal = msecsFinal;
  }

  public TimeInterval (String packed) {
    String part[] = packed.split(DELIM);
    try {
      msecsStart = Long.parseLong(part[0]);
      msecsFinal = Long.parseLong(part[1]);
    }
    catch(Exception xpt) {
      Log.e("TimeInterval", "Invalid packed string <" + packed + ">.");
      msecsStart = 0;
      msecsFinal = 0;
    }
  }

  public long getStart () { return msecsStart; }
  public long getFinal () { return msecsFinal; }

  public boolean isValid() {
    return (msecsStart!=0) && (msecsFinal!=0);
  }

  public boolean isEqual(TimeInterval other) {
    return (msecsStart==other.msecsStart) && (msecsFinal==other.msecsFinal);
  }

  public boolean containsInside(TimeInterval other) {
    return (this.msecsStart<=other.msecsStart) && (other.msecsFinal<=this.msecsFinal);
  }

  public boolean hasSameStart(TimeInterval other) {
    return (msecsStart==other.msecsStart);
  }

  public boolean isPrevious(TimeInterval other) {
    return msecsFinal + SBADevice.READ_PERIOD_MSECS == other.msecsStart;
  }

  public boolean isInside (long msecs) {
    return (msecsStart<=msecs) && (msecs<=msecsFinal);
  }

  public String toString () {
    return String.valueOf(msecsStart) + DELIM + String.valueOf(msecsFinal);
  }

  public String toLog () {
    return String.valueOf(msecsStart) + ";" + String.valueOf(msecsFinal) + "  [" + CTEK.getMillisFormatted(msecsStart) + " - " + CTEK.getMillisFormatted(msecsFinal) + "]";
  }

} // EOClass TimeInterval
