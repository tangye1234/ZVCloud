package com.zigvine.zagriculture;

import org.json.JSONArray;

import com.zigvine.android.http.HttpManager;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

public class MainApp extends Application {
	
	private static String version;
	private static int apilevel;
	
	@Override
	public void onCreate() {
		super.onCreate();
		HttpManager.createMessageManager(this);
		apilevel = android.os.Build.VERSION.SDK_INT;
		version = "unkown";
		try {
			PackageInfo packInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = packInfo.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			// TODO pend another thread to get version and agent
		}
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		HttpManager.destroyMessageManager();
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		HttpManager.destroyMessageManager();
	}
	
	public static String getVersion() {
		return version;
	}
	
	public static int getAPILevel() {
		return apilevel;
	}
	
	
	/****************** sign in session ********************/
	
	private static boolean mSignIn;
	private static String mUser;
	private static JSONArray mGroup;
	
	public static void initSession(String user, JSONArray group) {
		mSignIn = true;
		mUser = user;
		mGroup = group;
	}
	
	public static void quitSession() {
		if (mSignIn) {
			mSignIn = false;
		}
	}
	
	public static boolean isSignIn() {
		return mSignIn;
	}
	
	public static String getUser() {
		return mUser;
	}
	
	public static JSONArray getGroup() {
		return mGroup;
	}
}
