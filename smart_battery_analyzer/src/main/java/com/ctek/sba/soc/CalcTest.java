package com.ctek.sba.soc;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.ProgressBar;

import com.ctek.sba.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by evgeny.akhundzhanov on 14.04.2017.
 */

public class CalcTest {

  private final static String  TAG = "CalcTest";

  private final static int BUFFER_CHUNK = 4096 * 4;

  private final static double EPSILON = 0.001;

  private final static String  CR = "\r";
  private final static String  LF = "\n";
  private final static String  CRLF = "\r\n";

  private static class PointD {
    double x, y;

    public PointD (double x, double y) {
      this.x = x;
      this.y = y;
    }
  }


  private static List<Double> makeVoltageList (List<PointD> points) {
    List<Double> voltageList = new ArrayList<>();
    for(PointD point : points) {
      voltageList.add((double) point.x);
    }
    return voltageList;
  }

  private static boolean isClose (Double testValue, Double estSoC) {
    return Math.abs(testValue - estSoC) <= EPSILON;
  }


  public static boolean FileExists (String path) {
    return new File(path).exists();
  }

  public static void createDirectories (String folder) {
    File fold = new File(folder);
    if(!fold.exists()) {
      fold.mkdirs();
    }
  }


  public static String getHomeFolder (Context ctx) {
    File dir = ctx.getExternalFilesDir(null);
    String home_folder = dir.getAbsolutePath();
    return home_folder;
  }

  private static String getSubfolderPath (Context ctx, String subfolder) {
    String path = getHomeFolder(ctx) + "/" + subfolder;
    if(!FileExists(path)) {
      createDirectories(path);
    }
    return path;
  }
  public static String getFolderTEMP (Context ctx) { return getSubfolderPath(ctx, "temp"); }

  public static boolean copyFileRaw (Context ctx, int iRawId, String trg, boolean bOverwriteIfExist) {
    InputStream in;
    // if(Exists(trg)&&!bOverwriteIfExist) return true;
    if(FileExists(trg)) {
      if(!bOverwriteIfExist) {
        // check size
        File f_ = new File(trg);
        long len_file = f_.length();
        try {
          in  = ctx.getResources().openRawResource(iRawId);
          int  len_raw_ = in.available();
          in .close();
          if(len_raw_==(int)len_file) {
            return true;
          }
        }
        catch(IOException e) {
          e.printStackTrace();
        }
      }
    }

    // not exist OR exist && overwrite
    byte data[] = new byte[BUFFER_CHUNK];
    try {
      in  = ctx.getResources().openRawResource(iRawId);
      OutputStream out = new BufferedOutputStream(new FileOutputStream(trg));

      int count;
      while ((count = in.read(data, 0, BUFFER_CHUNK)) != -1) {
        out.write(data, 0, count);
      }
      data = null;
      out.flush();
      out.close();
      in .close();
    } catch (IOException e) {
      e.printStackTrace();
      data = null;
      return false;
    }
    return true;
  }

  public static byte[] readBytes (InputStream i, int max, ProgressBar progressBar) throws IOException {
    byte buf[] = new byte[4096];
    int totalReadBytes = 0;
    while(true) {
      int readBytes = 0;
      readBytes = i.read(buf, totalReadBytes, buf.length-totalReadBytes);
      if (readBytes < 0) {
        // end of stream
        break;
      }
      totalReadBytes += readBytes;
      if (progressBar != null) progressBar.setProgress(totalReadBytes);
      if (max > 0 && totalReadBytes > max) {
        throw new IOException("File is too big");
      }
      if (totalReadBytes == buf.length) {
        // grow buf
        // Log.d("DEBUG", "ReadBytes: growing buffer from " + buf.length + " to " + (buf.length*2));
        byte newbuf[] = new byte[buf.length*2];
        System.arraycopy(buf, 0, newbuf, 0, totalReadBytes);
        buf = newbuf;
      }
    }
    byte result[] = new byte[totalReadBytes];
    System.arraycopy(buf, 0, result, 0, totalReadBytes);
    return result;
  }

  public static byte[] readBytes (InputStream i) throws IOException {
    return readBytes(i, 0, null);
  }

  public static String readString (InputStream i) throws IOException {
    byte[] b = readBytes(i);
    return new String(b, "UTF-8");
  }

  public static String readFileUtf8 (String path) {
    String content = null;
    FileInputStream fs;
    try {
      fs = new FileInputStream(path);
      InputStream is = new BufferedInputStream(fs);
      try {
        content = readString(is);
        is.close();
        is = null;
      } catch (IOException xpt) {
        throw new RuntimeException(xpt);
      }
    } catch (FileNotFoundException xpt) {
      xpt.printStackTrace();
    }
    return content;
  }


  public static void test_91_files (Context ctx) {
    // test_91_file (ctx, R.raw.inp_9_1_105, "inp_9_1_105.txt", R.raw.out_9_1_105, "out_9_1_105.txt", 105);
    test_91_file (ctx, R.raw.inp_9_1_075, "inp_9_1_075.txt", R.raw.out_9_1_075, "out_9_1_075.txt", 75);
    // test_91_file (ctx, R.raw.inp_9_1_032, "inp_9_1_032.txt", R.raw.out_9_1_032, "out_9_1_032.txt", 32);
    return;
  }

  public static String[] parseFile (Context ctx, int iFileInpRawResId, String inpFileName) {
    String pathInp = getFolderTEMP(ctx) + "/" + inpFileName;
    copyFileRaw(ctx, iFileInpRawResId, pathInp, true);
    String input = readFileUtf8(pathInp);
    input = input.replace(CRLF, LF);
    input = input.replace(CR, LF);
    input = input.replace(",", ".");
    String[] inp = input.split(LF);
    return inp;
  }

  public static void test_91_file (Context ctx, int iFileInpRawResId, String inpFileName, int iFileOutRawResId, String outFileName, int Ah) {
    String[] inp = parseFile(ctx, iFileInpRawResId, inpFileName);
    String[] out = parseFile(ctx, iFileOutRawResId, outFileName);

    int ii, iiSize = Math.min(inp.length, out.length);

    List<PointD> points = new ArrayList<>();
    for(ii=0;ii<iiSize;++ii) {
      double volt = Double.parseDouble(inp[ii]);
      double soc_ = Double.parseDouble(out[ii]);
      points.add(new PointD(volt, soc_));
    } // ii

    List<Double> m_voltageList = makeVoltageList(points);

    Calculate calcSoc = new Calculate(points.size(), null);
    calcSoc.setBatterySize(Ah);
    // SoCData.StateFlag flag =
    calcSoc.testVoltageList(m_voltageList);


    Log.d(TAG, "CalcTest START +++++++ Ah = " + Ah + " points: " + points.size() + " EPSILON = " + EPSILON);
    int nErrors = 0;
    SoCData[] soc_data = calcSoc.getSoCData();
    int kk = 0;
    for(PointD point : points) {
      // assertThat(isClose((double)point.y, soc_data[kk++].d_estSoC), is(true));
      boolean bIsClose = isClose(point.y, soc_data[kk].d_estSoC);
      if(!bIsClose) nErrors++;
      Log.d(TAG, "" + (kk+1) + "/. " + point.x + " - " + point.y + "/" + soc_data[kk].d_estSoC + " - " + bIsClose);
      kk++;
    }
    Log.d(TAG, "CalcTest FINAL +++++++ Errors: " + nErrors);

    return;
  }

} // EOClass CalcTest
