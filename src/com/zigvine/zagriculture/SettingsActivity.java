package com.zigvine.zagriculture;

import com.zigvine.android.utils.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.provider.MediaStore;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public class SettingsActivity extends UIPreferenceActivity<SettingsActivity> implements
		OnPreferenceChangeListener, OnPreferenceClickListener {	
	
	public static final String KEY_FREQUENCY = "alarm_frequency";
	public static final String KEY_SWITCH = "alarm_switch";
	public static final String KEY_RINGTONE = "alarm_sound";
	public static final String KEY_MONITORBG = "monitor_bg";
	
	public static final String DEFAULT = "默认铃声";
	
	ListPreference frequency, monitorbg;
	CheckBoxPreference checkbox;
	RingtonePreference ringtone;
	View titleMain;
	TextView title;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UI.setContentView(R.layout.main_settings);
		UI.setParentBackground(getMonitorBackgroundResourceId(this));
		UI.setMainBackground(R.drawable.image_frame);
		
		addPreferencesFromResource(R.xml.settings_preference);
		
		int dp = Utils.dp2px(this, 5);
		FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) findViewById(android.R.id.list).getLayoutParams();
		lp.topMargin = dp;
		lp.rightMargin = dp;
		lp.leftMargin = dp;
		lp.bottomMargin = dp;
		findViewById(android.R.id.list).setLayoutParams(lp);
		
		lp = (FrameLayout.LayoutParams) findViewById(R.id.main_content).getLayoutParams();
		lp.topMargin += dp;
		lp.rightMargin += dp;
		lp.leftMargin += dp;
		lp.bottomMargin += dp;
		findViewById(R.id.main_content).setLayoutParams(lp);
		
		//Preference p = PreferenceManager.;
		frequency = (ListPreference) findPreference(KEY_FREQUENCY);
		frequency.setSummary(frequency.getEntry());
		frequency.setOnPreferenceChangeListener(this);
		
		monitorbg = (ListPreference) findPreference(KEY_MONITORBG);
		monitorbg.setSummary(monitorbg.getEntry());
		monitorbg.setOnPreferenceChangeListener(this);
		
		checkbox = (CheckBoxPreference) findPreference(KEY_SWITCH);
		checkbox.setOnPreferenceClickListener(this);
		
		ringtone = (RingtonePreference) findPreference(KEY_RINGTONE);
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		String r = sp.getString(KEY_RINGTONE, "");
		ringtone.setSummary(getRingtoneName(r));
		ringtone.setOnPreferenceChangeListener(this);
		
		UI.setupMoreMenu(null);
		UI.setBackNavVisibility(View.VISIBLE);
		titleMain = findViewById(R.id.title_main);
		titleMain.setOnClickListener(this);
		title = (TextView) findViewById(R.id.title_text);
		title.setText("设置");
		
	}
	
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.title_main:
			finish();
			return;
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals(KEY_FREQUENCY)) {
			final CharSequence[] values = frequency.getEntryValues();
			for (int i = 0; i < values.length; i++) {
				if(values[i].equals(newValue)) {
					frequency.setSummary(frequency.getEntries()[i]);
					break;
				}
			}
			UI.startOnlineService(false);
			return true;
		}
		if (preference.getKey().equals(KEY_MONITORBG)) {
			final CharSequence[] values = monitorbg.getEntryValues();
			for (int i = 0; i < values.length; i++) {
				if(values[i].equals(newValue)) {
					monitorbg.setSummary(monitorbg.getEntries()[i]);
					UI.setParentBackground(getResIdFromValue(Integer.valueOf((String) values[i])));
					break;
				}
			}
			return true;
		}
		if (preference.getKey().equals(KEY_RINGTONE)) {
			//UI.toast(newValue.toString());
			ringtone.setSummary(getRingtoneName(newValue.toString()));
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            sp.edit().putString(KEY_RINGTONE, newValue.toString()).commit();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onPreferenceClick(Preference preference) {
		UI.startOnlineService(false);
		return true;
	}
	
	private String getRingtoneName(String uri) {
		if(uri == null || uri.equals("")) {
			return "无";
		} else {
			try {
				Uri mUri = Uri.parse(uri);
				Cursor c = getContentResolver().query(mUri, new String[] { MediaStore.Audio.Media.TITLE }, null, null, null);
				try {
					if (c.moveToFirst()) {
						return c.getString(0);
					} else {
						return DEFAULT;
					}
				} finally {
					if (c != null) {
						c.close();
					}
				}
			} catch(Exception e) {
				return DEFAULT;
			}
		}
	}
	
	public static long getPeriod(Context context) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		String v = sp.getString(KEY_FREQUENCY, "300000");
		long period;
		if (sp.getBoolean(KEY_SWITCH, true)) {
			period = Long.parseLong(v);
		} else {
			period = 0;
		}
		return period;
	}
	
	public static int getMonitorBackgroundResourceId(Context context) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		int i = Integer.valueOf(sp.getString(KEY_MONITORBG, "0"));
		return getResIdFromValue(i);
	}
	
	private static int getResIdFromValue(final int i) {
		switch (i) {
			case 0:
				return R.drawable.main_bg_blue;
			case 1:
				return R.drawable.main_bg;
			case 2:
				return R.drawable.main_bg_blur;
			default:
				return R.drawable.main_bg_blue;
		}
	}
	
	public static String getRingtone(Context context) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		String r = sp.getString(KEY_RINGTONE, "");
		return r;
	}

}
