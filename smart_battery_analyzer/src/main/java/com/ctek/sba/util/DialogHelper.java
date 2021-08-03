package com.ctek.sba.util;

import android.app.AlertDialog;
import android.content.Context;

import com.ctek.sba.R;

/**
 * Created by elab on 06/02/15.
 */
public class DialogHelper {
/*
  public static void showDialog(Context context, int titleId, int messageId,
                                MaterialDialog.ButtonCallback buttonCallback) {
    new MaterialDialog.Builder(context)
        .title(titleId)
        .content(messageId)
        .positiveText(R.string.ok)
        .negativeText(R.string.cancel)
        .cancelable(false)
        .callback(buttonCallback)
        .show();
  }
*/
/*
  public static void showOk(Context context, int titleId, int messageId, MaterialDialog.ButtonCallback cb) {
    new MaterialDialog.Builder(context)
        .title(titleId)
        .content(messageId)
        .neutralText(R.string.ok)
        .callback(cb)
        .cancelable(false)
        .show();
  }
  */

 // See CTEKBATTERYSENSESLA-50 Alert dialogs are displayed incorrectly on Android 8.0 device.
  public static void showOk(Context ctx, int titleId, int messageId) {
    new AlertDialog.Builder(ctx)
        .setTitle(titleId)
        .setMessage(messageId)
        .setPositiveButton(R.string.ok, null)
        .show();
  }

  public static void showOk(Context ctx, int titleId, String  messageId) {
    new AlertDialog.Builder(ctx)
        .setTitle(titleId)
        .setMessage(messageId)
        .setPositiveButton(R.string.ok, null)
        .show();
  }

}
