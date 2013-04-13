package com.zigvine.zagriculture;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import com.zigvine.android.http.HttpManager;
import com.zigvine.android.http.Request;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class UpdateModel {

	private static final String TAG = "UpdateModel";
	private static final String DEFAULT_URL = Request.URL + "/version/ws2_version.txt";
	private Context context;
	
	public static final String UPDATE = "update_infomation";

	public UpdateModel(Context ctx) {
		context = ctx.getApplicationContext();
	}

	public int update(String pkgname) {
		return update(pkgname, DEFAULT_URL);
	}

	private int update(String pkgname, String urlget) {
		Log.v(TAG, "prepare to update detection");
		// FIXME when wanna save data stream, consider to add 'force' argument to 
		// determine wether the timestamp is in need
		HttpGet request = new HttpGet(urlget + "?timestamp=" + new Date().getTime());
		HttpResponse httpResponse = null;
		int result = MainApp.UNCHECKED;
		try {
			httpResponse = HttpManager.getMessageManager().nativeExecute(request);
			int state = httpResponse.getStatusLine().getStatusCode();
			HttpEntity entity = httpResponse.getEntity();
			if (state == HttpStatus.SC_OK) {
				result = handleResp(entity, pkgname);
			} else {
				entity.consumeContent();
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private int handleResp(HttpEntity resp, String pkgname)
			throws UnsupportedEncodingException, IllegalStateException,
			IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				resp.getContent(), "UTF-8"));
		try {
			String line;
			final int datanum = 5;
			final int pkg = 0;
			final int ver = 1;
			final int sze = 2;
			final int src = 3;
			final int ext = 4;
			String[] arr = new String[datanum];
			arr[ext] = "";
			int i = 0;
			//Log.v(TAG, "pkgname = " + pkgname);
			while ((line = reader.readLine()) != null) {
				try {
					//Log.i(TAG, line);
					if (i != 0 || line.equalsIgnoreCase(pkgname)) {
						if (i < datanum - 1) {
							arr[i++] = line;
						} else if (line.length() > 0) {
							arr[i] += (line + "\n");
						} else {
							break;
						}
					}
				} catch (Exception e) {
					continue;
				}
			}
			Log.v(TAG, "pkg= " + arr[pkg]);
			Log.v(TAG, "ver= " + arr[ver]);
			Log.v(TAG, "sze= " + arr[sze]);
			Log.v(TAG, "src= " + arr[src]);
			Log.v(TAG, "ext= " + arr[ext]);
			if (pkgname.equalsIgnoreCase(arr[pkg])) {
				if (isNewVersion(arr[ver])) {
					Log.v(TAG, "need update");
					notify(arr[ver], arr[src], arr[sze], arr[ext]);
					return MainApp.CHECKED_NEED_UPGRADE;
				}
			}
		} finally {
			reader.close();
			Log.v(TAG, "DONE Handle Http Resp");
		}
		return MainApp.CHECKED_NO_NEED;
	}

	private boolean isNewVersion(String v) {
		try {
			String current = getVersionName();
			Log.v(TAG, "current version = " + current);
			if (current.equals(v)) {
				return false;
			} else {
				VersionRule a = new VersionRule(v);
				VersionRule b = new VersionRule(current);
				if (b.compareTo(a) > 0) {
					return false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	private void notify(String version, String src, String size, String ext) {
		if (version != null && src != null && ext != null) {
			Editor edit = context.getSharedPreferences(UPDATE, Context.MODE_PRIVATE).edit();
			edit.putString("ver", version);
			edit.putString("src", src);
			edit.putString("sze", size);
			edit.putString("ext", ext);
			Log.v(TAG, version + "," + src + "," + size + "," + ext);
			edit.commit();

			Intent intent = new Intent(context, UpdateActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
		} else {
			Log.e(TAG, "Server Configuration Error");
		}
	}

	private String getVersionName() throws Exception {
		PackageManager packageManager = context.getPackageManager(); // getPackageName()是你当前类的包名，0代表是获取版本信息
		PackageInfo packInfo = packageManager.getPackageInfo(
				context.getPackageName(), 0);
		return packInfo.versionName;
	}
	
	
	private static class VersionRule implements Comparable<VersionRule> {
    	
    	private String a;
    	private int b;
    	private int c;
    	private int d;
    	private int len;
    	String[] split;
    	
    	public VersionRule(String version) {
    		split = version.split("\\.");
    		a = split[0];
    		len = split.length;
    		b = parseAt(1);
    		c = parseAt(2);
    		d = parseAt(3);
    	}
    	
    	@Override
    	public int compareTo(VersionRule v) {
    		if (a.equalsIgnoreCase(v.a)) {
    			float fv = Float.valueOf(v.b + "." + v.c);
				float fme = Float.valueOf(b + "." + c);
				if (fme > fv) {
					return 1;
				} else if (fme == fv) {
					if (d > v.d) return 1;
					else if (d < v.d) return -1;
					else return 0;
				} else {
					return -1;
				}
    		}
    		return -2; // not the same a
    	}
    	
    	private int parseAt(int i) {
    		int number = 0;
    		if (len > i) {
    			number = Integer.parseInt(split[i]);
    		}
    		return number;
    	}
    }
	
}
