package com.ctek.sba.rest;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by evgeny.akhundzhanov on 02.04.2018.
 */

public class CIngestData implements Serializable {

  @SerializedName("requestId")          private String requestId;
  @SerializedName("sensors")            private List<CSensor> sensors;
  @SerializedName("requestProperties")  private CRequestProperties requestProperties;

  public CIngestData () {
    requestProperties = new CRequestProperties();
    requestId = UUID.randomUUID().toString();
    sensors = new ArrayList<>();
  }

  public void addSensor(CSensor sensor) {
    sensors.add(sensor);
  }

  public String toString () { return new Gson().toJson(this); }

} // EOClass CIngestData
