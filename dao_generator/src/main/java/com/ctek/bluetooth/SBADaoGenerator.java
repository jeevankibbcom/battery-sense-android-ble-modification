package com.ctek.bluetooth;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;
import de.greenrobot.daogenerator.ToMany;

public class SBADaoGenerator {

  public static void main(String args[]) throws Exception {
    Schema schema = new Schema(5, "greendao");

    Entity device = schema.addEntity("Device");
    device.implementsInterface("java.io.Serializable");
    device.addIdProperty();
    device.addStringProperty("name");
    device.addStringProperty("address");
    device.addStringProperty("imagePath");
    device.addStringProperty("serialnumber");
    device.addBooleanProperty("previouslyBonded");
    device.addLongProperty("updated");
    device.addIntProperty("readCursor");
    device.addIntProperty("rssi");
    device.addIntProperty("indicatorColor");
    device.addDoubleProperty("voltage");

    Entity voltage = schema.addEntity("Voltage");
    voltage.addIdProperty();
    voltage.addLongProperty("timestamp");
    voltage.addDoubleProperty("value");
    voltage.addDoubleProperty("temperature");    // v. 5 - for temperature

    // relations
    Property deviceIdVoltage = voltage.addLongProperty("deviceId").notNull().getProperty();
    device.addToMany(voltage, deviceIdVoltage);

    new DaoGenerator().generateAll(schema, args[0]);
  }
}
