package com.zigvine.android.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.zigvine.zagriculture.MainApp;

import android.content.Context;
import android.graphics.Bitmap;
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
	
	public static class MathFloat {
		public static float max(float[] d) {
			if (d == null || d.length == 0) throw new IllegalArgumentException("param should not be empty");
			int len = d.length;
			float max = d[0];
			for (int i = 1; i < len; i++) {
				float v = d[i];
				if (v >  max) max = v;
			}
			return max;
		}
		public static float min(float[] d) {
			if (d == null || d.length == 0) throw new IllegalArgumentException("param should not be empty");
			int len = d.length;
			float min = d[0];
			for (int i = 1; i < len; i++) {
				float v = d[i];
				if (v < min) min = v;
			}
			return min;
		}
	}
	
	public static class MathInt {
		public static int max(int[] d) {
			if (d == null || d.length == 0) throw new IllegalArgumentException("param should not be empty");
			int len = d.length;
			int max = d[0];
			for (int i = 1; i < len; i++) {
				int v = d[i];
				if (v >  max) max = v;
			}
			return max;
		}
		public static int min(int[] d) {
			if (d == null || d.length == 0) throw new IllegalArgumentException("param should not be empty");
			int len = d.length;
			int min = d[0];
			for (int i = 1; i < len; i++) {
				int v = d[i];
				if (v < min) min = v;
			}
			return min;
		}
	}
	
	public static File saveTmpBitmap(Bitmap bitmap) throws IOException {
		File f = new File(MainApp.getOutCacheDir(), "_tmp_save.jpg");
		f.createNewFile();
		FileOutputStream fOut = null;
		try {
			fOut = new FileOutputStream(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fOut);
		try {
			fOut.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			fOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return f;
	}
	
	public static final SimpleDateFormat DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	public static final SimpleDateFormat DATE = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

}
