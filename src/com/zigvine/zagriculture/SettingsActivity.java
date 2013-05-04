package com.zigvine.zagriculture;

import com.zigvine.android.utils.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public class SettingsActivity extends UIPreferenceActivity<SettingsActivity> implements
		OnPreferenceChangeListener, OnPreferenceClickListener {	
	
	public static String KEY_FREQUENCY = "alarm_frequency";
	public static String KEY_SWITCH = "alarm_switch";
	
	ListPreference frequency;
	CheckBoxPreference checkbox;
	View titleMain;
	TextView title;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UI.setContentView(R.layout.main_settings);
		UI.setParentBackground(R.drawable.main_bg_blue);
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
		
		checkbox = (CheckBoxPreference) findPreference(KEY_SWITCH);
		checkbox.setOnPreferenceClickListener(this);
		
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
	
	public static long getPeriod(Context context) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		String v = sp.getString(KEY_FREQUENCY, "300000");
		long period;
		if (sp.getBoolean("KEY_SWITCH", true)) {
			period = Long.parseLong(v);
		} else {
			period = 0;
		}
		return period;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		UI.startOnlineService(false);
		return true;
	}
	

}
