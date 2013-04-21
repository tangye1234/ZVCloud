package com.zigvine.android.utils;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONObjectExt {
	
	public JSONObject json;
	
	public JSONObjectExt(JSONObject json) {
		this.json = json;
	}
	
	public String getString(String key, String defValue) {
		try {
			return json.getString(key);
		} catch (JSONException e) {}
		return defValue;
	}
	
	public int getInt(String key, int defValue) {
		try {
			return json.getInt(key);
		} catch (JSONException e) {}
		return defValue;
	}
	
	public long getLong(String key, long defValue) {
		try {
			return json.getLong(key);
		} catch (JSONException e) {}
		return defValue;
	}
	
	@Override
	public String toString() {
		return json.toString();
	}
	
}
