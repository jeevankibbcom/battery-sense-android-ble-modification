package com.ctek.sba.soc;

import android.util.Log;

import com.ctek.sba.bluetooth.SBADevice;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import greendao.Device;
import greendao.Voltage;

/**
 * Created by evgeny.akhundzhanov on 30.09.2016.
 * Section is some voltage points without gaps inside.
 */
public class VoltageSections {

  final String TAG = this.getClass().getSimpleName();

  public enum SoCCalcMode {
    ALL_DATA_FOR_CHART, // EA 02-May-2017. This entry could be removed a bit later.
    // LATEST_SOC_ONLY, // Now the cal results are cached. No need for this mode.
  };

  public static final Long GAP_MSECS = 30*60*1000L;

  private long  device_id;
  private List<VoltageSection> sections;

  public VoltageSections (Device device, List<Voltage> voltages, SoCCalcMode mode) {
    device_id = device.getId();

    sections = new ArrayList<>();

    if((mode == SoCCalcMode.ALL_DATA_FOR_CHART) && (voltages.size() !=0)) {
      checkFirstVoltageMidnight(voltages.get(0));
    }

    VoltageSection section = new VoltageSection(device_id);
    for (int ii = 0; ii < voltages.size(); ++ii) {
      if (ii > 0) {
        // detect gap
        long currMsecs = voltages.get(ii).getTimestamp();
        long prevMsecs = voltages.get(ii - 1).getTimestamp();
        boolean bGap = (currMsecs - prevMsecs) >= GAP_MSECS;
        if (bGap) {
          if (section.size() > 0) {
            sections.add(section);
          }

          // fill gap with dummy invisible points
          section = new VoltageSection(device_id, prevMsecs, currMsecs, SBADevice.READ_PERIOD_MSECS);
          if(section.size() > 0) {
            sections.add(section);
          }
          section = new VoltageSection(device_id);

        }
      }
      section.add(voltages.get(ii));
    }
    if (section.size() > 0) {
      sections.add(section);
    }

    calculateSoC_All();
  }

  private void checkFirstVoltageMidnight (Voltage voltage) {
    Long point1timestamp = voltage.getTimestamp();

    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(point1timestamp);

    boolean bStartsAtMidnight = (cal.get(Calendar.HOUR_OF_DAY) == 0) && (cal.get(Calendar.MINUTE) == 0); // ignore seconds here
    if(!bStartsAtMidnight) {
      cal.set(Calendar.HOUR_OF_DAY, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      Long midnightMsecs = cal.getTimeInMillis();
      SimpleDateFormat sdf1 = new SimpleDateFormat("dd MMM HH:mm:ss");
      Log.d(TAG, "midnightMsecs = " + midnightMsecs + " - " + sdf1.format(midnightMsecs));

      VoltageSection dummy = new VoltageSection(device_id, midnightMsecs, point1timestamp, SBADevice.READ_PERIOD_MSECS);
      sections.add(dummy);
    }
  }


  public int size () { return sections.size(); }
  public VoltageSection getSection (int index) { return sections.get(index); }

  public void calculateSoC_All () {
    for(int nSection=0; nSection < size(); ++nSection) {
      getSection(nSection).calculateSoC();
    }
  }

  public SoCData getLatestSocValue () {
    if(size() > 0) {
      List<SoCData> soc_list = getSection(size() - 1).getListSoCData();
      return soc_list.size() != 0 ? soc_list.get(soc_list.size() - 1) : null;
    }
    return null;
  }

} // EOClass
