package com.ctek.sba.rest;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by evgeny.akhundzhanov on 28.08.2017.
 */

public class REST {

  public final static int	MAX_ATTEMPTS = 3;

  // remote call results
  public final static int RESULT_UNDEFINED		=-1;
  public final static int RESULT_SUCCESS			= 0;
  public final static int RESULT_ERROR			= 1;

  public final static int HTTP_OK = 200;

  public final static String UTF8 = "UTF-8";

  //public final static String CTEK_SERVER = "https://ctek-sensory-ingestion-prod.azure-api.net/v1/api/sensors/ingest-data";

  //Added
//  public final static String CTEK_SERVER = "https://c2pxws4o8k.execute-api.eu-west-1.amazonaws.com/dev/storeSensorData"; //On AWS
  public final static String CTEK_SERVER = "";

  // Roy, 07-May-2018. API keys:
  // Primary: 967fb114fb1d4ebf9df66a97cb2f615e
  // Secondary: 16a98c5af0f14135ac27b2bd81aa60dc
  public final static String CTEK_API_KEY = "967fb114fb1d4ebf9df66a97cb2f615e"; //Nor needed for AWS
  public final static String ANDROID  = "android";  // os



  public static String getNotNullExceptionMessage (Exception xpt) {
    String error = xpt.getLocalizedMessage();
    if(error==null) {
      error = "Unknown exception (with null message).";
    }
    return error;
  }

  public static String md5String (String src) {
    String md5 = src;
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(src.getBytes(), 0, src.length());
      md5 = new BigInteger(1, md.digest()).toString(16);
    }
    catch (NoSuchAlgorithmException xpt) {
      // ignore // Log.e("md5String", getNotNullExceptionMessage(xpt));
    }
    return md5;
  }

} // EOClass REST
