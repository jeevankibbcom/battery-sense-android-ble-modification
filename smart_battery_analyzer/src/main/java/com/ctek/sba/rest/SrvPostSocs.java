package com.ctek.sba.rest;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.Pair;

import com.ctek.sba.bluetooth.CTEK;
import com.ctek.sba.bluetooth.DeviceManager;
import com.ctek.sba.bluetooth.DeviceManagerHiQ;
import com.ctek.sba.device.DeviceMap;
import com.ctek.sba.device.DeviceMapIntervals;
import com.ctek.sba.device.DeviceSoC;
import com.ctek.sba.device.DeviceSoCMap;
import com.ctek.sba.device.TimeInterval;
import com.ctek.sba.io.dao.DeviceRepository;
import com.ctek.sba.soc.SoCData;
import com.ctek.sba.soc.VoltageSection;
import com.ctek.sba.ui.DeviceDetailsActivity;
import com.ctek.sba.util.SettingsHelper;

import java.util.List;
import java.util.Locale;

import greendao.Device;
import greendao.Voltage;


/**
 * Created by evgeny.akhundzhanov on 28.08.2017.
 */

public class SrvPostSocs extends RESTIntentService {

  private final static String TAG = "SrvPostSocs";


  private final static String MODE_ = "mode";
  private final static String REASON = "reason";

  public final static int MODE_SEND_LAST_SOC  = 1;  // Obsolete mode. Should be removed later on.
  public final static int MODE_HISTORY_DATA   = 2;

  //Added
  private Context mContext = this.getBaseContext();

  public final static String getModeName (int MODE_) {
    if(MODE_==MODE_SEND_LAST_SOC) return "LAST_SOC";
    if(MODE_==MODE_HISTORY_DATA) return "HISTORY";
    return "UNKNOWN";
  }

  // A maximum of 20´000 readings in total is allowed per request.
  // https://ctek-sensory-ingestion-test.portal.azure-api.net/docs/services/sensory-ingestion-api/operations/apisensorsingest-datapost?
  // public final static int MAX_READINGS_PER_REQUEST = 20000;
  public final static int MAX_READINGS = 1000; // MAX_READINGS_PER_REQUEST / 2;


  public static void start (Context ctx, String reason, int mode) {
      String mess = "";
      if(SettingsHelper.isDataCollectionSet(ctx)) {
        if(Network.isOnline(ctx)) {
          ctx.startService(new Intent(ctx, SrvPostSocs.class).putExtra(MODE_, mode).putExtra(REASON, reason));
          mess = "StartService called.";
        }
        else {
          mess = "Network is n/a.";
        }
      }
      else {
        mess = "Data collection is NOT set. Post data - IGNORED";
      }
      Log.d(TAG, mess + "(" + reason + ").");
      return;
  }

  public SrvPostSocs() {
    super(TAG);
  }

  @Override
  protected void onHandleIntent(Intent i_) {
    super.onHandleIntent(i_);

    final int mode_ = i_.getIntExtra(MODE_, MODE_SEND_LAST_SOC);
    final String reason = i_.getStringExtra(REASON);
    Log.d(TAG, " Service started.(" + reason + "). Mode = " + getModeName(mode_));

    if(mode_==MODE_SEND_LAST_SOC) {
      postLastSocs();
      //Added
//      Log.i("Update complete", "Battery update complete");
//      Intent i1_ = new Intent(DeviceManagerHiQ.START_LIVE_MODE);
//      mContext.sendBroadcast(i1_);
      ///
    }
//    else if(mode_==MODE_HISTORY_DATA) {
//
//    }
    else {
      postHistory();
      //Added
//      Log.i("Update complete", "Battery update complete");
//      Intent i1_ = new Intent(DeviceManagerHiQ.START_LIVE_MODE);
//      mContext.sendBroadcast(i1_);
//      ///
    }

    timer.stop();
    Log.d(TAG, "Service Stopped ======= Total Seconds: " + timer.getSeconds());
    Log.i(TAG, "Battery update complete");
    Context context = getApplicationContext();
      //DeviceManager.getInstance().getTaskRunning().equals(DeviceManager.Task.UPDATE_BATTERY_DATA);
      Intent i1_ = new Intent(DeviceManagerHiQ.START_LIVE_MODE);
      context.sendBroadcast(i1_);
    return;
  }

  private void postLastSocs () {
    List<Device> myDevices = DeviceRepository.getAllDevices(ctx);
    for (Device device : myDevices) {
      String serial = device.getSerialnumber();
      DeviceSoC lastSoC = DeviceSoCMap.getInstance().getDeviceSoC(device);
      if(lastSoC!=null) {
        boolean posted = lastSoC.isPostedToBackend();
        if(posted) {
          Log.d(TAG, "Device = " + serial + " POSTED at " + lastSoC.getPostedTimeFormatted());
        }
        else {
          SoCData socData = lastSoC.getLatestSocValue();
          if(socData!=null) {

            long msecs = lastSoC.getTimestampLast();

            CSensor sensor1 = new CSensor(serial, "Voltage").add(msecs, String.valueOf(socData.d_voltage));
            CSensor sensor2 = new CSensor(serial, "SoC").add(msecs, String.valueOf(socData.d_estSoC));

            // no history for this !
            double capacity = DeviceMap.getInstance().getDeviceCapacity(device.getId());
            CSensor sensor3 = new CSensor(serial, "BatteryCapacity").add(msecs, String.format(Locale.ROOT,"%.2f", capacity));


            CIngestData data = new CIngestData();
            data.addSensor(sensor1);
            data.addSensor(sensor2);
            data.addSensor(sensor3);

            Log.d(TAG, "Device = " + serial + " JSON = " + data.toString());

            Pair<Boolean, String> result = new HConnection().postJson(REST.CTEK_SERVER, token, data.toString());

            if(result.first) {
              lastSoC.setTimestampPostedToBackend();
              DeviceSoCMap.getInstance().setDeviceSoC(device, lastSoC);
              Log.d(TAG, "Device = " + serial + " POST success." + " Time = " + lastSoC.getPostedTimeFormatted());
            }
            else {
              Log.d(TAG, "Device = " + serial + " POST failed: " + result.second);
            }
          }
        }
      }
      else {
        // ignore
        Log.d(TAG, "Device = " + serial + " Last SOC is n/a.");
      }
    }
    return;
  }


  public void postHistory () {
    final long msecsTemperatureStarted = SettingsHelper.getTemperatureStartedTimestamp(ctx);

    List<Device> myDevices = DeviceRepository.getAllDevices(ctx);
    for (Device device : myDevices) {
      String serial = device.getSerialnumber();
      Long device_id = device.getId();
      List<TimeInterval> intervals = DeviceMapIntervals.getInstance().getIntervals4Device(device_id);

      DeviceMapIntervals.logIntervals(ctx, device_id, serial);

      DeviceSoC lastSoC = DeviceSoCMap.getInstance().getDeviceSoC(device);
      if(lastSoC!=null) {
        int nSects = lastSoC.getSectionsSize();
        Log.d(TAG, "Device = " + serial + " Sections:  " + nSects + ". Intervals: " + intervals.size());

        for(int nSect=0;nSect<nSects;++nSect) {
          VoltageSection sect = lastSoC.getSection(nSect);
          final boolean isDummy = sect.isDummy();
          final int sectSize = sect.size();
          Log.d(TAG, " Section " + (nSect+1) + "/. Size = " + sectSize + " isDummy = " + isDummy);
          if(isDummy) continue;

          long msecs1 = sect.getTimestampStart();
          long msecs2 = sect.getTimestampFinal();
          Log.d(TAG, "Interval: " + new TimeInterval(msecs1, msecs2).toLog());

          List<Voltage> listV = sect.getListVoltage();
          List<SoCData> listS = sect.getListSoCData();

          int nRuns = sectSize / MAX_READINGS;
          boolean bTail = (sectSize % MAX_READINGS) != 0;
          if(bTail) nRuns++;

          for(int nRun=0;nRun<nRuns;++nRun) {
            int indStart = nRun*MAX_READINGS;
            int indFinal = indStart + MAX_READINGS;
            if((nRun==(nRuns-1)) && bTail) indFinal = sectSize;

            Log.d(TAG, "Run: " + (nRun+1) + "/. [" + indStart + " - " + indFinal +").");

            long msecsStart = listV.get(indStart).getTimestamp();
            long msecsFinal = listV.get(indFinal - 1).getTimestamp();
            TimeInterval interval = new TimeInterval(msecsStart, msecsFinal);
            Log.d(TAG, "Interval: " + interval.toLog());

            // check the whole interval
            if(DeviceMapIntervals.containsIntervalInside(intervals, interval)) {
              Log.d(TAG, "The WHOLE interval was posted before. SKIPPED.");
              continue;
            }

            CSensor sensorV = new CSensor(serial, "Voltage");
            CSensor sensorS = new CSensor(serial, "SoC");
            CSensor sensorT = new CSensor(serial, "Temperature");

            int nSkipped = 0;
            for(int ind=indStart;ind<indFinal;++ind) {
              Voltage volt = listV.get(ind);
              long msecs = volt.getTimestamp();

              // check msecs
              if(DeviceMapIntervals.containsTimestamp(intervals, msecs)) {
                nSkipped++;
                // Log.d(TAG, "Timestamp was posted before. SKIPPED. " + CTEK.getMillisFormatted(msecs));
              }
              else {
                sensorV.add(msecs, Double.toString(volt.getValue()));
                sensorS.add(msecs, Double.toString(listS.get(ind).d_estSoC));

                boolean postTemperature = msecs >= msecsTemperatureStarted;
                if(postTemperature) {
                  sensorT.add(msecs, String.format(Locale.ROOT,"%.2f", volt.getTemperature()));
                }
              }
            }
            if(nSkipped!=0) {
              Log.d(TAG, "" + nSkipped + " points were posted before. SKIPPED. ");
            }

            if(sensorV.getSize()==0) {
              Log.d(TAG, "Empty sensor object. POST SKIPPED. ");
              continue;
            }

            Log.d(TAG, "Sensor object size = " + sensorV.getSize());

            CIngestData data = new CIngestData();
            data.addSensor(sensorV);
            data.addSensor(sensorS);
            if(sensorT.getSize()!=0) {
              data.addSensor(sensorT);
            }


            Log.d(TAG, " JSON = " + data.toString());

            Pair<Boolean, String> result = new HConnection().postJson(REST.CTEK_SERVER, token, data.toString());

            if(result.first) {
              // register posted interval for device_id.
              DeviceMapIntervals.getInstance().registerSuccessInterval(device_id, sensorV.getInterval());
            }
            else {
              Log.d(TAG, "POST failed: " + result.second);
            }

          } // nRun
        }
      }
      else {
        // ignore
        Log.d(TAG, "Device = " + serial + " Last SOC is n/a.");
      }
    }
    return;
  }

} // EOClass SrvPostSocs

