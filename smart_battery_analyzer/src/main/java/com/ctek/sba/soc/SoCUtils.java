package com.ctek.sba.soc;

/**
 * Created by evgeny.akhundzhanov on 28.09.2016.
 */
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.ctek.sba.R;
import com.ctek.sba.bluetooth.CTEK;
import com.ctek.sba.bluetooth.SBADevice;
import com.ctek.sba.device.DeviceSoC;
import com.ctek.sba.device.DeviceSoCMap;
import com.ctek.sba.util.SettingsHelper;
import com.ctek.sba.widget.BatteryView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import greendao.Device;
import greendao.Voltage;
  
import static java.lang.Math.round;

/**
 * soc = state of charge
 */
public class SoCUtils {

  public static double getPercentFromSoc(SoCData soc) {
    final double percent;

    if (soc != null) {
      if (soc.d_estSoC < 0) {
        percent = 0;
      } else if (soc.d_estSoC > 100) {
        percent = 100;
      } else {
        percent = soc.d_estSoC;
      }
    } else {
      percent = 100;
    }
    return percent;
  }

  private final static String TAG = "getLatestSocValue";

  @Nullable
  public static SoCData getLatestSocValue(Device device) {
    DeviceSoC lastSoC = DeviceSoCMap.getInstance().getDeviceSoC(device);
    if(lastSoC!=null) {
      DeviceSoC deviceStamp = new DeviceSoC(device, null);
      VoltageSections sections = lastSoC.getLastSocDataIfDeviceStampIsTheSame(deviceStamp);
      if(sections!=null) {
        Log.d(TAG, "Last SoC - from cache.");
        return sections.getLatestSocValue();
      }
    }

    VoltageSections sections =  getSocs(device, VoltageSections.SoCCalcMode.ALL_DATA_FOR_CHART);
    SoCData soc = sections.getLatestSocValue();
    return soc;
  }

  public static VoltageSections getSocs(Device device, VoltageSections.SoCCalcMode mode) {
    DeviceSoC lastSoC = DeviceSoCMap.getInstance().getDeviceSoC(device);
    if(lastSoC!=null) {
      DeviceSoC deviceStamp = new DeviceSoC(device, null);
      VoltageSections sections = lastSoC.getLastSocDataIfDeviceStampIsTheSame(deviceStamp);
      if(sections!=null) {
        Log.d(TAG, "SoC array - from cache.");
        return sections;
      }
    }

    // VoltageSections sections =
    List<Voltage> voltages = device.getVoltageList("getSocs 1");
    if (voltages == null) {
      voltages = new ArrayList<>();
    }
    Log.d(TAG, "SoC algorithm started.");
    VoltageSections sections = new VoltageSections(device, voltages, mode);
    DeviceSoCMap.getInstance().setDeviceSoCSections(device, sections);
    Log.d(TAG, "SoC array calculated and cached.");
    device.getVoltageList("getSocs 2");
    return sections;
  }

  public static void updateBattery(BatteryView battery, SoCData soc) {
    BatteryView.Color color = BatteryView.Color.UNDEFINED;

    if (soc != null) {
      battery.setLevel((float) (soc.d_estSoC / 100d));
      if (soc.getFlag() == SoCData.StateFlag.FLAG_GREEN) {
        color = BatteryView.Color.GREEN;
      }
      if (soc.getFlag() == SoCData.StateFlag.FLAG_YELLOW) {
        color = BatteryView.Color.ORANGE;
      }
      if (soc.getFlag() == SoCData.StateFlag.FLAG_RED) {
        color = BatteryView.Color.RED;
      }
    } else {
      battery.setLevel(0);
      // battery.setCoreColor(BatteryView.Color.GREEN);
    }
    if(color != BatteryView.Color.UNDEFINED) {
      battery.setCoreColor(color);
    }
  }

  public static void updateBattery(BatteryView battery, TextView batterStatus, ImageView imgBatterStatus, SoCData soc) {
    BatteryView.Color color = BatteryView.Color.UNDEFINED;
    batterStatus.setText("Battery ok");
    batterStatus.setVisibility(View.INVISIBLE);
    imgBatterStatus.setImageResource(R.drawable.battery_legend_green);
    imgBatterStatus.setVisibility(View.INVISIBLE);
    if (soc != null) {
      battery.setLevel((float) (soc.d_estSoC / 100d));
      if (soc.getFlag() == SoCData.StateFlag.FLAG_GREEN) {
        color = BatteryView.Color.GREEN;
        batterStatus.setVisibility(View.VISIBLE);
        batterStatus.setText("Battery ok");
        imgBatterStatus.setVisibility(View.VISIBLE);
        imgBatterStatus.setImageResource(R.drawable.battery_legend_green);
      }
      if (soc.getFlag() == SoCData.StateFlag.FLAG_YELLOW) {
        color = BatteryView.Color.ORANGE;
        batterStatus.setVisibility(View.VISIBLE);
        batterStatus.setText("Charge soon");
        imgBatterStatus.setVisibility(View.VISIBLE);
        imgBatterStatus.setImageResource(R.drawable.battery_legend_yellow);
      }
      if (soc.getFlag() == SoCData.StateFlag.FLAG_RED) {
        color = BatteryView.Color.RED;
        batterStatus.setVisibility(View.VISIBLE);
        batterStatus.setText("Charge now");
        imgBatterStatus.setVisibility(View.VISIBLE);
        imgBatterStatus.setImageResource(R.drawable.battery_legend_red);
      }
    } else {
      battery.setLevel(0);
      color = BatteryView.Color.UNDEFINED;
    }
    if(color != BatteryView.Color.UNDEFINED) {
      battery.setCoreColor(color);
    }
  }

  public static void updatePercentLabel(TextView label, SoCData soc) {
    if (soc != null) {
      final double percent = getPercentFromSoc(soc);
      String labelText = String.format("%d%%", round(percent));
      label.setText(labelText);
    }
    else {
      label.setText("");
    }
  }

  public static void setUIVoltage (TextView txtVolt, double value) {
    String volt = String.format(Locale.getDefault(), "%.2f V", value);
    txtVolt.setText(volt);
  }

  public static void setUITemperature (Context ctx, TextView txtTemp, double temperatureC, boolean bLive) {
    boolean bCelsius = SettingsHelper.getCelsius(ctx);
    double temperature = bCelsius ? temperatureC : SettingsHelper.convertCelcius2Fahrenheit((float)temperatureC);
    // String format = bLive ? "%.2f" : "%.0f";
    String format = "%.0f"; // Roy: 2 decimals for Voltage, 0 decimals for temperature.
    String temp = String.format(Locale.getDefault(), format, temperature);
    temp += bCelsius ? " \u2103" : " \u2109";
    txtTemp.setText(temp);
  }

  public static void updateSoC_Voltage_Temperature (Context ctx, TextView txtPercents, TextView txtVolt, TextView txtTemp, SoCData soc, double temperatureC, boolean bLive) {
    if (soc != null) {
      long percent = round(getPercentFromSoc(soc));
      txtPercents.setText(String.format("%d%%", percent));
      boolean bShowVoltage = SettingsHelper.getShowVoltage(ctx);
      if(bShowVoltage) {
        if(bLive) {
          setUIVoltage(txtVolt, soc.d_voltage);
          setUITemperature(ctx, txtTemp, temperatureC, bLive);
        }
        else {
          txtVolt.setText("-- V");
          txtTemp.setText("--" + (SettingsHelper.getCelsius(ctx) ? " \u2103" : " \u2109"));
        }
      }
    }
    else {
      txtVolt.setText("");
      txtTemp.setText("");
    }
  }

  public static List<Voltage> primitivesToVoltages(double[] voltageDoubles, long deviceId, long firstItemTimestamp) {
    List<Voltage> voltages = new ArrayList<>();
    for (int i = 0; i < voltageDoubles.length; ++i) {
      double pack = voltageDoubles[i]; // assume packed
      double voltageReal = pack % CTEK.MULT;
      pack -= voltageReal;
      double temperature = (int) Math.round(pack / CTEK.MULT) / 2;
      long dataPointTime = firstItemTimestamp + SBADevice.READ_PERIOD_MSECS * i;
      Voltage voltage = new Voltage(
          null,
          dataPointTime,
          voltageReal,
          deviceId,
          temperature);
      voltages.add(voltage);
    }
    return voltages;
  }
} // EOClass SoCUtils
