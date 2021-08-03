package com.ctek.sba.soc;

import android.util.Log;

import com.ctek.sba.device.DeviceMap;

import java.util.ArrayList;
import java.util.List;

import greendao.Voltage;

/**
 * Created by evgeny.akhundzhanov on 30.09.2016.
 *
 * List of Voltages without gaps.
 * Dummy lists (that do not appear on chart) are supported as well.
 */
public class VoltageSection {

  private long  device_id;
  private List<Voltage> list;
  private List<SoCData> socs;
  private boolean isDummy;

  public VoltageSection (long device_id) {
    this.device_id = device_id;

    list = new ArrayList<>();
    socs = new ArrayList<>();
    setIsDummy(false);
  }

  /*
  public VoltageSection (long device_id, Long msecs1, Long msecs2) {
    this.device_id = device_id;

    list = new ArrayList<>();
    list.add(new Voltage(null, msecs1, 0.d, 0));
    list.add(new Voltage(null, msecs2, 0.d, 0));

    socs = new ArrayList<>();
    socs.add(new SoCData());
    socs.add(new SoCData());

    setIsDummy(true);
  }
  */

  public VoltageSection (long device_id, long prevMsecs, long currMsecs, long step) {
    this.device_id = device_id;

    list = new ArrayList<>();
    socs = new ArrayList<>();
    long msecs;
    for(msecs=prevMsecs; msecs<currMsecs; msecs+= step) {
      list.add(new Voltage(null, msecs, 0.d, 0, 0.d));
      socs.add(new SoCData());
    }
    list.add(new Voltage(null, currMsecs, 0.d, 0, 0.d));
    socs.add(new SoCData());
    setIsDummy(true);
  }

  public int size () { return list.size(); }
  public List<Voltage> getListVoltage () { return list; }
  public List<SoCData> getListSoCData () { return socs; }

  public void add (Voltage data) {
    list.add(data);
  }

  private void setIsDummy (boolean value) {
    isDummy = value;
  }
  public boolean isDummy () { return isDummy; }

  private class LogCallback implements Calculate.ILogCallback {

    @Override
    public void LogD(String tag, String mess) {
      Log.d(tag, mess);
    }
  };

  private LogCallback mLogCallback = new LogCallback();
  // private LogCallback mLogCallback = null;

  public void calculateSoC () {
    if(size() == 0) {
      socs = new ArrayList<>();
    }

    if(isDummy()) {
      socs = new ArrayList<>();
      for(int ii=0; ii < size(); ++ii) {
        socs.add(new SoCData());
      }
    }
    else {
      Calculate calc = new Calculate(size(), null); // mLogCallback
      calc.addSamples(list);
      calc.setBatterySize(DeviceMap.getInstance().getDeviceCapacity(device_id));
      calc.calcSoC();
      socs = calc.getSoCDataList();
    }
    return;
  }

  public long getTimestampStart () { return list.get(0).getTimestamp(); }
  public long getTimestampFinal () { return list.get(list.size()-1).getTimestamp(); }

} // EOClass VoltageSection
