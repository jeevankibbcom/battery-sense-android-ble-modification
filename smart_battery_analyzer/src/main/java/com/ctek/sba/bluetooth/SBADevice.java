/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctek.sba.bluetooth;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public final class SBADevice {

  public static final int  READ_PERIOD_MINUTES = 5;
  public static final long READ_PERIOD_MSECS = READ_PERIOD_MINUTES * 60 * 1000;   // 5 minutes in msecs
  public static final int  NUMBER_OF_READ_PERIODS_IN_DAY = 24*12; // 288 of 5 minute intervals per day

  // General identifiers
  public static final class Identifier {
    // Device name
    public static final String LOCAL_NAME = "SBA";
    public static final String MANUFACTURER_NAME = "CTEK";
    public static final String MODEL_NUMBER = "SBA_MODEL_A";

    // Device ID
    // public static final UUID DEVICE_ID_UUID = UUID.fromString("DD6D5C96-F0A8-D184-E733-E0F3D78501AE"); // EA 01-Nov-2016. Not used.
  } // EOClass Identifier

  // Services
  public static final class Service {
    // Advertisement service
    // public static final UUID ADVERTISEMENT_SERVICE_UUID    = UUID.fromString("812CA8ED-A177-4D74-AEFA-70098C5416AF");  // EA 01-Nov-2016. Not used.
    // Base & Info services
    public static final UUID BASE_SERVICE_UUID                = UUID.fromString("812CA8ED-A177-4D74-AEFA-70098C5416AF");
    public static final UUID DEVICE_INFORMATION_SERVICE_UUID  = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");

    public static final UUID BASE_SERVICE_UUID_V2= UUID.fromString("000016af-1212-efde-1523-785fef13d123");


  } // EOClass Service

  // Characteristics
  public static final class Characteristic {
    // Seconds since start of peripheral (READ)
    // 00001db7-1212-efde-1523-785fef13d123
    private static final String UPTIME = "337EA83E-744B-4B7C-B23F-45BEAAB11DB7";
    public  static final UUID UPTIME_UUID = UUID.fromString(UPTIME);

//    private static final String UPTIME_V2 = "00001DB7-1212-EFDE-1523-785FEF13D123";
    private static final String UPTIME_V2 = "00001db7-1212-efde-1523-785fef13d123";
    public  static final UUID UPTIME_UUID_V2 = UUID.fromString(UPTIME_V2);

    // Current Voltage (READ)
    private static final String VOLTAGE_CURRENT = "631994F8-FD2E-48BF-AF8F-E86320E17D94";
    public  static final UUID VOLTAGE_CURRENT_UUID = UUID.fromString(VOLTAGE_CURRENT);

    // Current Temperature (READ)
    private static final String TEMPERATURE_CURRENT = "B5634099-97C4-4F37-ADF3-771246E77754";
    public static final UUID TEMPERATURE_CURRENT_UUID = UUID.fromString(TEMPERATURE_CURRENT);

    // Write number of readings wanted of Voltage history (WRITE)
    private static final String VOLTAGE_HISTORY_WRITE = "418D1817-F516-45F1-AB64-433AE596613D";
    public  static final UUID VOLTAGE_HISTORY_WRITE_UUID = UUID.fromString(VOLTAGE_HISTORY_WRITE);

    // Subscribe to get Voltage history (NOTIFY)
    private static final String VOLTAGE_HISTORY = "A337B4EB-2544-4F94-87C9-F7732E5FE250";
    public  static final UUID VOLTAGE_HISTORY_UUID = UUID.fromString(VOLTAGE_HISTORY);

    // Cursor of last written value (READ)
    private static final String VOLTAGE_HISTORY_CURSOR = "FC3119DA-9B40-414E-A1C4-6C1737718CAE";
    public  static final UUID VOLTAGE_HISTORY_CURSOR_UUID = UUID.fromString(VOLTAGE_HISTORY_CURSOR);

    // Write place for pairing key (WRITE)
    private static final String KEY_HOLE = "A1D67C01-FE3F-01BF-7D49-7DBDC43B02A6";
    public  static final UUID KEY_HOLE_UUID = UUID.fromString(KEY_HOLE);

    //16 bit value representing the number of records in the log memory (0 to 29999).
    // When the Log Write Position value wraps around (from 29999 to 0)
    // the log memory is considered full (always 29999).
    private static final String NUMBER_OF_RECORDS = "99063298-B2B7-49BA-B5DA-ACE18DC45292";
    public  static final UUID NUMBER_OF_RECORDS_UUID = UUID.fromString(NUMBER_OF_RECORDS);

    private static final String MEASUREMENT_INTERVAL = "E5A7E9A0-E2D7-4F46-83D1-0E55D2427780";
    public  static final UUID MEASUREMENT_INTERVAL_UUID = UUID.fromString(MEASUREMENT_INTERVAL);

    private static final String SERIAL_NUMBER = "00002A25-0000-1000-8000-00805f9b34fb";
    public  static final UUID SERIAL_NUMBER_UUID = UUID.fromString(SERIAL_NUMBER);

    private static final String MANUFACTURER_NAME = "00002A29-0000-1000-8000-00805f9b34fb";
    public  static final UUID MANUFACTURER_NAME_UUID = UUID.fromString(MANUFACTURER_NAME);

    private static Map<String, String> map_ = null;

    public static String uuid2Name (UUID uuid) {
      if(map_==null) {
        map_ = new TreeMap<>();
        map_.put(UPTIME_UUID                .toString().toLowerCase(),                 "UPTIME");
        map_.put(UPTIME_UUID_V2             .toString().toLowerCase(),              "UPTIME_V2");
        map_.put(VOLTAGE_CURRENT_UUID       .toString().toLowerCase(),        "VOLTAGE_CURRENT");
        map_.put(TEMPERATURE_CURRENT_UUID   .toString().toLowerCase(),    "TEMPERATURE_CURRENT");
        map_.put(VOLTAGE_HISTORY_WRITE_UUID .toString().toLowerCase(),  "VOLTAGE_HISTORY_WRITE");
        map_.put(VOLTAGE_HISTORY_UUID       .toString().toLowerCase(),        "VOLTAGE_HISTORY");
        map_.put(VOLTAGE_HISTORY_CURSOR_UUID.toString().toLowerCase(), "VOLTAGE_HISTORY_CURSOR");
        map_.put(KEY_HOLE_UUID              .toString().toLowerCase(),               "KEY_HOLE");
        map_.put(MEASUREMENT_INTERVAL_UUID  .toString().toLowerCase(),   "MEASUREMENT_INTERVAL");
        map_.put(SERIAL_NUMBER_UUID         .toString().toLowerCase(),          "SERIAL_NUMBER");
        map_.put(NUMBER_OF_RECORDS_UUID     .toString().toLowerCase(),      "NUMBER_OF_RECORDS");
        map_.put(MANUFACTURER_NAME_UUID     .toString().toLowerCase(),      "MANUFACTURER_NAME");
      }
      String name = map_.get(uuid.toString().toLowerCase());
      return (name!=null) ? name : uuid.toString();
    }

  } // EOClass Characteristic

  public static final class Status {
    // Read after key hole
    public static final int WRONG_KEY   = 0x85;
    public static final int CORRECT_KEY = 0x89;
  } // EOClass Status

} // EOClass SBADevice
