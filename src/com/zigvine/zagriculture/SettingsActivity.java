package com.zigvine.zagriculture;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.ListView;

public class SettingsActivity extends UIActivity<SettingsActivity> {
	
	ListView list;
	PreferenceScreen prefScreen;
	PreferenceManager mPrefManager;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UI.setContentView(R.layout.main_settings);
		list = (ListView) findViewById(R.id.settings_list_view);
		
		//Preference p = PreferenceManager.;
		
	}
	

}
