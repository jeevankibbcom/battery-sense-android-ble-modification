package com.ctek.sba.rest;

/**
 * Created by evgeny.akhundzhanov on 28.08.2017.
 */

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;


public class NameValuePair implements Parcelable {

  public static final Creator<NameValuePair> CREATOR = new Creator<NameValuePair>() {
    @Override
    public NameValuePair createFromParcel(Parcel source) {
      return new NameValuePair(source.readString(), source.readString());
    }

    @Override
    public NameValuePair[] newArray(int size) {
      return new NameValuePair[size];
    }
  };

  private final String name;
  private final String value;

  public NameValuePair(final String name, final String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public String asParam() {
    return name + "=" + value;
  }

  public String asURLEncodedParam() {
    String result = null;
    try {
      result = URLEncoder.encode(name, REST.UTF8) + "=" + URLEncoder.encode(value, REST.UTF8);
    }
    catch (UnsupportedEncodingException xpt) {
      Log.e("asURLEncodedParam", xpt.getMessage());
    }
    return result;
  }


  @Override
  public String toString() {
    return asParam();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(name);
    dest.writeString(value);
  }

} // EOClass NameValuePair
