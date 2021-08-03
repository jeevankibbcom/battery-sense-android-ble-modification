package com.ctek.sba.rest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.json.JSONException;

import android.util.Base64;
import android.util.Log;
import android.util.Pair;


/**
 * Created by evgeny.akhundzhanov on 28.08.2017.
 */

public class HConnection {

  public HConnection () {
  }

  public HttpURLConnection createURLConnection (URL url, String token) throws IOException {

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

    conn.setDoInput(true);
    conn.setReadTimeout(15000);

    // Basic Auth
    if(token!=null) {
      String auth = "app:" + token;
      String code = Base64.encodeToString(auth.getBytes(), Base64.NO_PADDING);
      conn.setRequestProperty("Authorization", "Basic " + code);
    }
    return conn;
  }

  public void write (HttpURLConnection conn, String postString) {
     Log.i("writePost", "URL = " + conn.getURL().toString());
     //Log.i("writePost", "postString = " + postString);
    OutputStream outputStream = null;
    try {
      outputStream = conn.getOutputStream();
      BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));

      bufferedWriter.write(postString);

      bufferedWriter.flush();
      bufferedWriter.close();
      outputStream.close();
    }
    catch (IOException xpt) {
      Log.e("writePost", xpt.getMessage());
    }
    return;
  }

  public String getStringFromInpuStream (InputStream is) throws IOException, JSONException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      sb.append(line + "\n");
    }
    is.close();
    return sb.toString();
  }

  public Pair<Boolean, String> postJson (String url_, String token, String json) {
    boolean success = false;
    String response = "";
    HttpURLConnection conn = null;
    try {
      conn = createURLConnection(new URL(url_), token);
      conn.setRequestMethod("POST");

      //Added
      conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
     // conn.setRequestProperty("Connection","keep-alive");
     // conn.setRequestProperty("Content-Type", "application/json-patch+json; charset=utf-8");  //Original
    //  conn.setRequestProperty("Ocp-Apim-Subscription-Key", REST.CTEK_API_KEY);  //Originally present change to AWS
      write(conn, json);


      int http_code = conn.getResponseCode();
      success = (http_code == REST.HTTP_OK);

      InputStream is = null;
      try {
        is = conn.getInputStream();
      }
      catch(IOException xpt) {
        is = conn.getErrorStream();
      }
      if(is!=null) {
        response = getStringFromInpuStream(is);
        is.close();
      }

    }
    catch (JSONException | IOException xpt) {
      response = xpt.getMessage();
    }
    finally {
      if(conn!=null) {
        conn.disconnect();
      }
    }

    return new Pair<Boolean, String>(success,response);
  }

} // EOClass HConnection
