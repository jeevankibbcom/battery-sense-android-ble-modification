package com.ctek.sba.device;

import com.ctek.sba.soc.SoCData;
import com.ctek.sba.soc.VoltageSection;
import com.ctek.sba.soc.VoltageSections;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import greendao.Device;
import greendao.Voltage;

/**
 * Created by evgeny.akhundzhanov on 02.05.2017.
 */

public class DeviceSoC {

  private int   sizeOfVoltages;
  private long  timestampFirst;
  private long  timestampLast;
  private VoltageSections sections;

  private long  timestampPostedToBackend;

  public DeviceSoC () {
    sizeOfVoltages = 0;
    timestampFirst = 0;
    timestampLast  = 0;
    sections = null;
    timestampPostedToBackend = 0;
  }

  private List<Voltage> createVoltages (Device device) {
    List<Voltage> voltages = device.getVoltageList("createVoltages");
    if (voltages == null) {
      voltages = new ArrayList<>();
    }
    return voltages;
  }

  public DeviceSoC (Device device, VoltageSections sections) {
    List<Voltage> voltages = createVoltages(device);

    sizeOfVoltages = voltages.size();
    if(sizeOfVoltages!=0) {
      timestampFirst = voltages.get(0).getTimestamp();
      timestampLast = voltages.get(sizeOfVoltages - 1).getTimestamp();
      this.sections = sections;
      timestampPostedToBackend = 0;
    }
    else {
      timestampFirst = 0;
      timestampLast  = 0;
      this.sections = null;
      timestampPostedToBackend = 0;
    }
  }

  public VoltageSections getLastSocDataIfDeviceStampIsTheSame (DeviceSoC deviceStamp) {
    boolean b1 = sizeOfVoltages == deviceStamp.sizeOfVoltages;
    if(b1) {
      boolean b2 = timestampFirst == deviceStamp.timestampFirst;
      boolean b3 = timestampLast  == deviceStamp.timestampLast;
      if(b2 && b3) {
        return sections;
      }
    }
    return null;
  }

  public void setTimestampPostedToBackend () {timestampPostedToBackend = System.currentTimeMillis(); }
  public long getTimestampPostedToBackend () { return timestampPostedToBackend; }
  public boolean isPostedToBackend () { return timestampPostedToBackend!=0; }

  public SoCData getLatestSocValue () {
    return (sections!=null) ? sections.getLatestSocValue() : null;
  }

  public long getTimestampLast () { return timestampLast; }

  private static String getMillisFormatted (long msecs) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(msecs);
    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US);
    return sdf.format(cal.getTime());
  }

  public String getPostedTimeFormatted () {
    return getMillisFormatted(getTimestampPostedToBackend());
  }

  public int getSectionsSize () { return sections!=null ? sections.size() : 0; }
  public VoltageSection getSection (int index) { return sections.getSection(index); }

} // EOClass DeviceSoC

