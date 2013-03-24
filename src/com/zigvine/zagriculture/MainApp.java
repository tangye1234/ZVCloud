package com.zigvine.zagriculture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
		mAlarmGroup = new HashMap<Long, Integer>();
	}
	
	public static void quitSession() {
		if (mSignIn) {
			mSignIn = false;
		}
		if (mAlarmGroup != null) {
			mAlarmGroup.clear();
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
	
	/****************** alarm data ********************/
	
	private static Map<Long, Integer> mAlarmGroup;
	private static int mAlarmCount;
	private static ArrayList<AlarmReceiverListener> mListeners = new ArrayList<AlarmReceiverListener>();
	
	public static void setAlarmSummary(JSONArray list) throws JSONException {
		if (mAlarmGroup != null) {
			mAlarmGroup.clear();
			mAlarmCount = list.length();
			for (int i = 0; i < mAlarmCount; i++) {
				JSONObject json = list.getJSONObject(i);
				long gid = json.getLong("GroupId");
				Integer count = mAlarmGroup.get(gid);
				if (count == null) {
					count = 0;
				}
				mAlarmGroup.put(gid, count.intValue() + 1);
			}
			synchronized(mListeners) {
				for (AlarmReceiverListener l : mListeners) {
					if (l != null) {
						l.onAlarm(mAlarmCount, mAlarmGroup);
					}
				}
			}
		}
	}
	
	public static Map<Long, Integer> getAlarmGroup() {
		return mAlarmGroup;
	}
	
	public static interface AlarmReceiverListener {
		public void onAlarm(int alarmCount, Map<Long, Integer> alarmGroup);
	}
	
	public static void registerAlarmReceiver(AlarmReceiverListener listener, boolean fetchOnRegister) {
		if (listener != null) {
			if (fetchOnRegister) {
				listener.onAlarm(mAlarmCount, mAlarmGroup);
			}
			mListeners.add(listener);
		}
	}
	
	public static void unRegisterAlarmReceiver(AlarmReceiverListener listener) {
		if (listener != null) {
			mListeners.remove(listener);
		}
	}
}
