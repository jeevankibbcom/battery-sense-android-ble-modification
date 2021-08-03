package com.ctek.sba.bluetooth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.ctek.sba.util.HexBin;

/**
 * Created for android on 07/05/15.
 * by Martin Kurtsson, martin.kurtsson@screeninteraction.com
 * © Screen Interaction 2015
 */
public class BondingHelper {
  private final static String TAG = BondingHelper.class.getSimpleName();

  public static byte[] MD5EncryptPasscode(String serialNumber) {
    try {
      int[] dec = mergeSaltAndSerialNumber(getSalt(), convertSerialNumber(serialNumber));
      byte[] bytes = HexBin.lossyIntArrayToByteArray(dec);
      return MessageDigest.getInstance("MD5").digest(bytes);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return new byte[0];
    }
  }

  public static int[] convertSerialNumber(String serialNumber) {
    byte[] bytes = serialNumber.getBytes(StandardCharsets.US_ASCII);
    int[] ascii = new int[serialNumber.length()];
    for (int i = 0; i < serialNumber.length(); i++) {
      ascii[i] = bytes[i];
    }
    return ascii;
  }

  public static int[] mergeSaltAndSerialNumber(int[] salt, int[] serial) {
    int[] saltAndSerial = new int[salt.length + serial.length];
    int j = 0;
    for (int i = 0; i < salt.length; i++) {
      saltAndSerial[j++] = salt[i];
    }
    for (int i = 0; i < serial.length; i++) {
      saltAndSerial[j++] = serial[i];
    }
    return saltAndSerial;
  }

  public static int[] getSalt() {
    int[] salt = {0xE1, 0xDA, 0x71, 0x95, 0xEC, 0xED, 0x40, 0xE4, 0xBC, 0x16, 0x55, 0x9E, 0x9D, 0xE7, 0x3B, 0xA4 };
    return salt;
  }
}
