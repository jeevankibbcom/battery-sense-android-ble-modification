package com.ctek.sba.rest;

import com.ctek.sba.device.TimeInterval;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by evgeny.akhundzhanov on 02.04.2018.
 *
 {
 "deviceId": "01234567890123456789012345678901",
 "deviceType": "BatterySense",
 "sensorName": "SoC",
 "valueType": "Double",
 "readings": {
 "2017-07-01T00:00:00Z": "50.082",
 "2017-07-02T00:00:00Z": "65"
 }
 },
 *
 */

public class CSensor implements Serializable {

  @SerializedName("deviceId")   private String deviceId;
  @SerializedName("deviceType") private String deviceType;
  @SerializedName("sensorName") private String sensorName;
  @SerializedName("valueType")  private String valueType;
  @SerializedName("os")  private String os;
  @SerializedName("readings")   private JsonObject readings;

  private transient String serial;
  private transient Long msecsStart;
  private transient Long msecsFinal;

  public CSensor (String serial, String name) {
    this.serial = serial;
    deviceId = REST.md5String(serial);
    deviceType = "BatterySense";
    sensorName = name;
    valueType = "Double";
    os = "android";

    readings = new JsonObject();

    msecsStart = null;
    msecsFinal = null;
  }


  public CSensor add (long msecs, String value) {
    if(msecsStart==null) msecsStart = msecs;  // first
    msecsFinal = msecs;                       // last

    readings.addProperty(CSensor.getMillisFormatted(msecs), value);
    return this;
  }

  public int getSize () {
    return readings.entrySet().size();
  }
  public TimeInterval getInterval() { return new TimeInterval(msecsStart, msecsFinal) ; }

  public String toString () { return new Gson().toJson(this).replace("nameValuePairs", "readings"); }


  public static String getMillisFormatted (long msecs) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(msecs);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
    return sdf.format(cal.getTime());
  }

} // EOClass CSensor
