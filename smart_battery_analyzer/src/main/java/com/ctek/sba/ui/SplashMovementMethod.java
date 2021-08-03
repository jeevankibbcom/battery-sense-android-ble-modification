package com.ctek.sba.ui;

import android.content.Context;
import android.text.Layout;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.MotionEvent;

import com.ctek.sba.util.SettingsHelper;

/**
 * Created by evgeny.akhundzhanov on 18.04.2018.
 */

public class SplashMovementMethod extends LinkMovementMethod {

  private Context ctx;

  public SplashMovementMethod (Context ctx) {
    this.ctx = ctx;
  }

  public boolean onTouchEvent(android.widget.TextView widget, android.text.Spannable buffer, android.view.MotionEvent event) {
    int action = event.getAction();

    if (action == MotionEvent.ACTION_DOWN)
    {
      int x = (int) event.getX();
      int y = (int) event.getY();

      x -= widget.getTotalPaddingLeft();
      y -= widget.getTotalPaddingTop();

      x += widget.getScrollX();
      y += widget.getScrollY();

      Layout layout = widget.getLayout();
      int line = layout.getLineForVertical(y);
      int off = layout.getOffsetForHorizontal(line, x);

      URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);
      if (link.length != 0)
      {
        String url = link[0].getURL();
        if (url.startsWith("https")) {
          SettingsHelper.openExternalPDF(ctx, url);
        }
        else if (url.startsWith("tel")) {}
        else if (url.startsWith("mailto")) {}
        return true;
      }
    }

    return super.onTouchEvent(widget, buffer, event);
  }

} // EOClass SplashMovementMethod
