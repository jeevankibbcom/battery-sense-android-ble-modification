package com.ctek.sba.bluetooth;

/**
 * Created by evgeny.akhundzhanov on 09.04.2018.
 */

public class CTEKData {

  Double[] volt_;
  Integer[] temp_;  // assume raw sensor data

  public CTEKData (int MODE_, byte[] data) {
    if((MODE_!=CTEK.MODE_GET_BATTERY_LEVEL) && (MODE_!=CTEK.MODE_GET_BATTERY_AND_TEMP))
      throw new IllegalArgumentException("CTEKData: Invalid parser mode =  " + MODE_ + ". Should be 2 or 3.");

    if((data.length % MODE_) != 0)
      throw new IllegalArgumentException("CTEKData: Data length MUST be a multiple of " + MODE_ + ".");

    volt_ = new Double[data.length / MODE_];
    temp_ = new Integer[data.length / MODE_];
    for (int i = 0; i < data.length; i += MODE_) {
      // java byte is signed so we need to convert to unsigned, & 0xFF does the trick
      volt_[i / MODE_] = ( (data[i] & 0xFF) + ((data[i+1] & 0xFF) * 256) ) / 2048d;

      if(MODE_==CTEK.MODE_GET_BATTERY_AND_TEMP) {
        int iLiveTemp = (int) data[i + 2];
        temp_[i / MODE_] = iLiveTemp;
      }
      else {
        temp_[i / MODE_] = 0;
      }
    }
  }

  public Double[] getVoltages() { return volt_; }
  public Integer[] getTemperatures() { return temp_; }
  public int getDataLength () { return volt_.length; }


} // EOClass CTEKData
