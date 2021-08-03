package com.ctek.sba.ui.support;

import android.content.Context;

import static java.lang.Math.round;

public class Ui {

	private static Context context;

	public static void init(Context context) {
		Ui.context = context;
	}

	public static int toPixels(float dipsValue) {
		if (context == null) return 0;
		return round(dipsValue * context.getResources().getDisplayMetrics().density);
	}

}
