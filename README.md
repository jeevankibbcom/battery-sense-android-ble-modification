# CTEK Battery Sense

CTEK Battery Sense is an Android and iOS application that monitors and reads
voltage history from a Bluetooth Low Energy device connected to a car battery.


## Prerequisites
What is needed to run the project

### Development
A Mac OS X, Windows or Linux computer with:

- Java
- Android Studio 2.1.2+
- Android SDK 23+

### Running
A device running at least Android 4.4 with BLE HW support. To be able to test
end-to-end, a battery analyser connected to a power source of at least 10V.

### Signing
A keystore.jks file is available in the repository and used when generating a signed APK.
The password for the keystore and the key "ctek-beta" is "odessa".

## Project
The project is built using Android Studio and Gradle and divided into three
modules. Import the root project in Android Studio for the full picture.

### Root
The root gradle. Contains gradle configuration, editor configuration and git
ignore.

### Dao Generator
A Java-project using GreenDao which generates SQLLite database entities for the Android app.
_SBADaoGenerator_ is the main file with the configuration. Generate data
objects defined in _SBADaoGenerator_ using the gradle task
**:dao_generator - run**

### Smart Battery Analyser
The main Android project. Contains


## Dependencies

- MPAndroidChart
    - v2.1.0
    - For drawing the percentage chart
- Appcompat v7
    - v22.1.1
    - For supporting material design in 4.x devices
- Picasso
    - v2.3.3
    - For taking a device photo
- Image-chooser-library
    - v1.4.02
    - For choosing an existing photo
- Greendao
    - v1.3.7
    - For storing device information in a SQLLite database.
- Material-dialogs
    - v0.7.8.1
    - For showing dialogs, material style
- Butterknife
    - v6.1.0
    - For injecting views.
- EventBus
    - v2.4.0
    - For event handling
- Junit
    - v4.12
    - For running java JUnit tests.

## Testing

### Devices
Samsung S5 Mini, Android v4.4.2
Motorola Moto-G, Android v5.1.1
Nexus 5, Android, v6.0.1
Samsung Galaxy S7, Android v6.0.1, Required new firmware in the sender. Fix from Samsung is coming.


## Developer notes

# Bluetooth LE architecture

There is three major requests that can be applied to a device
- Bind (add device)
- Get device serial (sender ID)
- Update device (read data stored in device)

DeviceManager is a class that syncronize these high-level requests

DeviceScanner does the scan for BLE devices

UpdateManager handlers the actual data received when updating a device.

BluetoothLeManager performs particular simple requests to the device


# Reading data stored in device (updating device)

This is sequence of actions performed by BluetoothLeManager:
- Connect
- Discover services
- Write secret to device keyhole
- Trigger bonding by reading uptime characteristic (if bonding, reading uptime again to get data)

Once uptime is read, the following sequence is used to read data:

- Read update interval
- Read number of voltage records available on device
- Read cursor position (the position where device will write next item)
- Handle the following cases:
    - We are already up-to-date based on time of last update
    - No data available on the sender (use live data if uptime > 60 second stabilizing time)
    - Read available data by "enabling notification" on voltage history characteristic
    - Detect if there is a gap in data, used to get timestamps correct, and if the circular buffer
      in the sender has wrapped around. If wrap around has occurred, the "notification"-read is
      divided in two steps, oldest data from the end of the buffer and newest data from the
      beginning of the buffer.

- Once we got expected number of records (in several batches of 10 records) broadcast result
and consider work done.


# Reading device serial number

Reading device serial is possible within 5 minutes of device reboot. After 5 minutes you still can
try to read it but will get empty string.

- Connect
- Discover services
- Read serial number from public DEVICE_INFORMATION service
