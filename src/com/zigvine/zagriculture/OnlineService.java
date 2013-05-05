package com.zigvine.zagriculture;

import org.json.JSONArray;
import org.json.JSONException;

import com.zigvine.android.http.Request;
import com.zigvine.android.http.Request.Resp;
import com.zigvine.android.http.Request.ResponseListener;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class OnlineService extends Service implements Runnable, ResponseListener {
	
	public static final String FOREGROUND_EXTRA = "com.zigvine.zagriculture.service_foreground";
	public static long GET_ALARM_PERIOD = 5 * 60 * 1000;  // 5 mins
	
	int requestId;
	Handler handler;
	boolean isForeground;
	boolean isRequestStarted;
	int alarmCount;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
    public void onCreate() {
    	super.onCreate();
    	handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	super.onStartCommand(intent, flags, startId);
    	if (!MainApp.isSignIn()) {
    		stopSelf();
    		isRequestStarted = false;
    		if (isForeground) {
    			isForeground = false;
    			stopForeground(true);
    			NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    			int icon = R.drawable.alarm;
    			Intent newIntent = new Intent(this, LoginActivity.class);
    			newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    			PendingIntent pi = PendingIntent.getActivity(this, 0, newIntent, 0);
    			Notification noti = new NotificationCompat.Builder(this)
    	        .setContentTitle(getString(R.string.app_name))
    	        .setContentText("用户已经注销")
    	        .setSmallIcon(icon)
    	        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
    	        .setContentIntent(pi)
    	        .setTicker("用户已经注销")
    	        .setAutoCancel(true)
    	        .build();
    			nm.notify(icon, noti);
    		}
    		return START_STICKY;
    	}
    	if (intent.getBooleanExtra(FOREGROUND_EXTRA, false)) {
    		isForeground = true;
    		alertNotification(alarmCount, false);
    	} else {
    		isForeground = false;
    		stopForeground(true);
    		GET_ALARM_PERIOD = SettingsActivity.getPeriod(this);
    		//android.util.Log.d("OnlineService", "period=" + GET_ALARM_PERIOD);
    		if (GET_ALARM_PERIOD <= 0) {
    			isRequestStarted = false;
    			++requestId;
    			handler.removeCallbacks(this);
    		} else if (!isRequestStarted) {
    			isRequestStarted = true;
    			run();
    		}
    	}
        return START_STICKY;
    }
    
    @Override
    public void run() {
    	if (MainApp.isSignIn()) {
	    	Request request = new Request(Request.GetAlarm, true);
	    	request.setDebug(true);
	    	request.asyncRequest(this, ++requestId);
	    	handler.removeCallbacks(this);
	    	if (GET_ALARM_PERIOD > 0) {
	    		handler.postDelayed(this, GET_ALARM_PERIOD);
	    	} else {
	    		isRequestStarted = false;
	    	}
    	} else {
    		isRequestStarted = false;
    	}
    }

	@Override
	public void onResp(int id, Resp resp, Object...obj) {
		if (resp != null) {
			try {
				JSONArray list = resp.json.getJSONArray("AlarmList");
				MainApp.setAlarmSummary(list);
				boolean ticker = alarmCount != list.length();
				alarmCount = list.length();
				if (isForeground) {
					alertNotification(alarmCount, ticker);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onErr(int id, String err, int httpCode, Object...obj) {
		if (id != requestId) return;
		// TODO something that user want to be notified
	}
	
	private void alertNotification(int count, boolean ticker) {
		int icon = R.drawable.alarm;
		Intent newIntent = new Intent(this, LoginActivity.class);
		newIntent.setAction(Intent.ACTION_MAIN);
		newIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		if (count > 0) {
			newIntent.putExtra(MainActivity.POSITION_EXTRA, 2);
		} else {
			newIntent.putExtra(MainActivity.POSITION_EXTRA, 0);
		}
		PendingIntent pi = PendingIntent.getActivity(this, 0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		Notification noti = new NotificationCompat.Builder(this)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(MainApp.getUser() + " 已经登录" + (count <= 0 ? "":"，已有" + count + "个报警"))
        .setSmallIcon(icon)
        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
        .setContentIntent(pi)
        .build();
		if (count > 0 && ticker) {
			String r = SettingsActivity.getRingtone(this);
			if (r != null && r.length() > 0) {
				Uri uri = Uri.parse(r);
				Ringtone ring = RingtoneManager.getRingtone(this, uri);
				ring.play();
			}
			noti.tickerText = "监控有" + count + "个报警";
		}
		startForeground(icon, noti);
	}

}
