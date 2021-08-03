package com.ctek.sba.rest;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by evgeny.akhundzhanov on 02.04.2018.
 */

public class CRequestProperties implements Serializable {

  @SerializedName("os") public String os;

  public CRequestProperties() {
    os = REST.ANDROID;
  }

  public String toString () { return new Gson().toJson(this); }

} // EOClass CRequestProperties
