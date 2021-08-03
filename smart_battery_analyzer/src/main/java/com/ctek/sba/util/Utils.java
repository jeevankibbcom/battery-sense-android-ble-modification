package com.ctek.sba.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.support.annotation.Nullable;

import com.ctek.sba.ui.EditDeviceActivity;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Utils {

  @Nullable
  public static <T> T getFirst(List<T> list) {
    if (list != null && list.size() > 0) {
      return list.get(0);
    } else {
      return null;
    }
  }

  public static <T> T getLast(List<T> list) {
    if (list != null && !list.isEmpty()) {
      return list.get(list.size() - 1);
    } else {
      return null;
    }
  }

  public static Bitmap rotateImage(File file) {
    ExifInterface exif = null;
    try {
      exif = new ExifInterface(file.getPath());
    } catch (IOException e) {
      e.printStackTrace();
    }
    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_UNDEFINED);

    Bitmap deviceImg = BitmapFactory.decodeFile(file.getAbsolutePath());
    Bitmap rotatedImage = Utils.rotateBitmap(deviceImg, orientation);

    return rotatedImage;
  }


  private static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

    Matrix matrix = new Matrix();
    switch (orientation) {
      case ExifInterface.ORIENTATION_NORMAL:
        return bitmap;
      case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
        matrix.setScale(-1, 1);
        break;
      case ExifInterface.ORIENTATION_ROTATE_180:
        matrix.setRotate(180);
        break;
      case ExifInterface.ORIENTATION_FLIP_VERTICAL:
        matrix.setRotate(180);
        matrix.postScale(-1, 1);
        break;
      case ExifInterface.ORIENTATION_TRANSPOSE:
        matrix.setRotate(90);
        matrix.postScale(-1, 1);
        break;
      case ExifInterface.ORIENTATION_ROTATE_90:
        matrix.setRotate(90);
        break;
      case ExifInterface.ORIENTATION_TRANSVERSE:
        matrix.setRotate(-90);
        matrix.postScale(-1, 1);
        break;
      case ExifInterface.ORIENTATION_ROTATE_270:
        matrix.setRotate(-90);
        break;
      default:
        return bitmap;
    }
    try {
      Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
      bitmap.recycle();
      return bmRotated;
    }
    catch (OutOfMemoryError e) {
      e.printStackTrace();
      return null;
    }
  }
}
