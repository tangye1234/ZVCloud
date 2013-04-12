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
	
}
