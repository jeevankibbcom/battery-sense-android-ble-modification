package com.ctek.sba.widget.font;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.HashMap;

import com.ctek.sba.R;

public class Fonts {

    private static HashMap<String, Typeface> mFonts = new HashMap<String, Typeface>();


    public static void applyFont(AttributeSet attrs, TextView textView) {
        if (textView.isInEditMode()) {
            return;
        }

        if (attrs != null) {
            TypedArray a = textView.getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CtekTextView, 0, 0);
            try {
                if (a != null) {
                    String font = a.getString(R.styleable.CtekTextView_customfont);
                    if (font != null) {
                        Fonts.applyFont(font, textView);
                    }
                }
            } finally {
              if (a != null) {
                a.recycle();
              }
            }
        }
    }

    public static void applyFont(String font, TextView textView) {
        textView.setTypeface(getTypeFace(font, textView.getContext()));
    }

    private static Typeface getTypeFace(String font, Context context) {
        if (!mFonts.containsKey(font)) {
            mFonts.put(font, Typeface.createFromAsset(context.getAssets(), "fonts/" + font + ".otf"));
        }
        return mFonts.get(font);
    }
}
