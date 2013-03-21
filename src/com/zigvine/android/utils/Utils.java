package com.zigvine.android.utils;

import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.util.TypedValue;

public class Utils {
	public static int dp2px(Context context, float dpValue) {
    	float v = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.getResources().getDisplayMetrics());
    	return (int) (v + 0.5f);
    	/*final float scale = context.getResources().getDisplayMetrics().density;
    	return (int) (dpValue * scale + 0.5f);*/
	}
	
	public static long mac2long(String mac) {
		String[] str = mac.split(":");
		long id = 0;
		for (int i = 0; i < str.length; i++) {
			id = id << 8 | Integer.valueOf(str[i], 16);
		}
		return id;
	}
	
	public static String long2mac(long id, int k) {
		String mac = "";
		for (;  k > 0; k--) {
			int b = (int) id & 0xff;
			id >>= 8;
			mac = ":" + Integer.toString(b, 16) + mac;
		}
		return mac.toLowerCase(Locale.US).substring(1);
	}
	
	public static String getDeltaTimeString(Date d1) {
		long time = new Date().getTime() - d1.getTime();
		if (time < 60000) {
			return (time / 1000) + "秒";
		}
		if (time < 60000*60l) {
			return (time / 60000) + "分钟";
		}
		if (time < 60000*60*24l) {
			return (time / 3600000l) + "小时";
		}
		return (time / 3600000*24l) + "天";
	}
}
