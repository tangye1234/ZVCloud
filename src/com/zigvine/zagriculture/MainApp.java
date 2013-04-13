package com.zigvine.zagriculture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zigvine.android.http.HttpManager;
import com.zigvine.android.http.Request;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class MainApp extends Application {
	
	private static final String TAG = "ZVMainApp";
	private static String version;
	private static int apilevel;
	private static MainApp instance = null;
	
	public static final int UNCHECKED = -1;
    public static final int CHECKED_NO_NEED = 0;
    public static final int CHECKED_NEED_UPGRADE = 1;
    
    private static int checkstate = UNCHECKED; 
    private Thread updatethread = null;
	
	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
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
		startCheck(null, false); // 自动检测版本更新
	}
	
	public static interface UpdateCheckListener {
		public void onCheckedOver(int result);
	}
	
	/**
     * 检测版本信息
     */
    public void startCheck(final UpdateCheckListener l, boolean force) {
        if(force || updatethread == null && checkstate == UNCHECKED) {
            updatethread = new Thread() {
                public void run() {
                    UpdateModel um = new UpdateModel(MainApp.this);
                    checkstate = um.update(getPackageName());
                    //for test
                    //checkstate = um.update("com.cnepay.android.pos");
                    Log.v(TAG, "checking result: " + String.valueOf(checkstate));
                    updatethread = null;
                    if (l != null) {
                    	l.onCheckedOver(checkstate);
                    }
                }
            };
            updatethread.start();
        } else {
            Log.v(TAG, "No need to start thread, checkstate: " + String.valueOf(checkstate));
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
		instance = null;
	}
	
	public static String getVersion() {
		return version;
	}
	
	public static int getAPILevel() {
		return apilevel;
	}
	
	public static MainApp getInstance() {
		return instance;
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
			Request request = new Request(Request.LogOff, true);
			request.asyncRequest(null, 0);
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
