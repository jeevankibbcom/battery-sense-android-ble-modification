package com.ctek.sba.widget.font;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

@SuppressLint("AppCompatCustomView")
public class CtekEditText extends EditText {

    public CtekEditText(Context context) {
        this(context, null, 0);
    }

    public CtekEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CtekEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Fonts.applyFont(attrs, this);
    }

}
