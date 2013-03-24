package com.zigvine.zagriculture;

import java.util.Map;

import org.json.JSONObject;
import org.taptwo.android.widget.SimpleFlowIndicator;
import org.taptwo.android.widget.ViewFlow;
import org.taptwo.android.widget.ViewFlow.SwitchDecideListener;

import com.zigvine.android.widget.AlarmPager;
import com.zigvine.android.widget.ControlPager;
import com.zigvine.android.widget.MonitorPager;
import com.zigvine.android.widget.Pager;
import com.zigvine.zagriculture.MainApp.AlarmReceiverListener;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MainActivity extends UIActivity<MainActivity>
		implements SwitchDecideListener, AlarmReceiverListener {
	
	ViewFlow mViewFlow;
	ViewFlowAdapter adapter;
	SimpleFlowIndicator indicator;
	TextView monitor, control, graph, alarm, title, alarm_count;
	TextView[] views;
	View moreMenu, refreshMenu;
	int[] tabsDrawableUnselectedRes;
	int[] tabsDrawableSelectedRes;
	int currentPos;
	
	Pager[] pages = new Pager[4];
	MonitorPager mMonitorPager;
	ControlPager mControlPager;
	AlarmPager mAlarmPager;
	
	public static final String POSITION_EXTRA = "com.zigvine.zagriculture.jump_position";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mMonitorPager = new MonitorPager(this);
		mControlPager = new ControlPager(this);
		mAlarmPager = new AlarmPager(this);
		pages[0] = mMonitorPager;
		pages[1] = mControlPager;
		pages[3] = mAlarmPager;
		
		UI.createStandardSlidingMenu();
		
		title = (TextView) findViewById(R.id.title_text);
		title.setText(getTitle());
		title.setOnClickListener(this);
		
		refreshMenu = findViewById(R.id.refresh_menu);
		refreshMenu.setVisibility(View.VISIBLE);
		refreshMenu.setOnClickListener(this);
		
		moreMenu = findViewById(R.id.more_menu);
		moreMenu.setOnClickListener(this);
		
		//get position from intent
		Intent intent = getIntent();
		int pos = intent.getIntExtra(POSITION_EXTRA, 0);
		//currentPos = 0;
		
		// content viewflow
        mViewFlow = (ViewFlow) findViewById(R.id.pages);
		adapter = new ViewFlowAdapter(this);
		
		indicator = (SimpleFlowIndicator) findViewById(R.id.indic);
		mViewFlow.setFlowIndicator(indicator);
		//mViewFlow.setOnViewSwitchListener(this);
		mViewFlow.setOnSwitchDecideListener(this);
		mViewFlow.setInterceptXMarginInDp(48, 0);
		// end content vieflow
		
		monitor = (TextView) findViewById(R.id.monitor);
		control = (TextView) findViewById(R.id.control);
		graph = (TextView) findViewById(R.id.graph);
		alarm = (TextView) findViewById(R.id.alarm);
		alarm_count = (TextView) findViewById(R.id.tab_alarm_count);
		views = new TextView[] {monitor, control, graph, alarm};
		tabsDrawableUnselectedRes = new int[] {
				R.drawable.monitor,
				R.drawable.control,
				R.drawable.graph,
				R.drawable.alarm
			};
		tabsDrawableSelectedRes = new int[] {
				R.drawable.monitor_select,
				R.drawable.control_select,
				R.drawable.graph_select,
				R.drawable.alarm_select
			};
		for (int i = 0; i < views.length; i++) {
			views[i].setTag(i);
			views[i].setOnClickListener(this);
		}
		/*
		Drawable selectDrawable = getResources().getDrawable(tabsDrawableSelectedRes[currentPos]);
		views[currentPos].setCompoundDrawablesWithIntrinsicBounds(null, selectDrawable, null, null);
		*/
		mViewFlow.setAdapter(adapter, pos); // must init after all
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		int pos = intent.getIntExtra(POSITION_EXTRA, 0);
		mViewFlow.setSelection(pos);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		startOnlineService(false);
		MainApp.registerAlarmReceiver(this, true);
		log("quit background, register alarm receiver");
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		startOnlineService(true);
		MainApp.unRegisterAlarmReceiver(this);
		log("start background on depend, un-register alarm receiver");
	}
	
	
	private void startOnlineService(boolean foreground) {
		Intent intent = new Intent(this, OnlineService.class);
		intent.putExtra(OnlineService.FOREGROUND_EXTRA, foreground);
		startService(intent);
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()) {
		case R.id.menu_signoff:
			MainApp.quitSession();
			finish();
			break;
		case R.id.monitor:
		case R.id.control:
		case R.id.graph:
		case R.id.alarm:
			log("pos" + view.getTag().toString());
			mViewFlow.setSelection((Integer) view.getTag());
			break;
		case R.id.title_text:
			UI.toggleStandardMenu();
			break;
		case R.id.refresh_menu:
			if (pages[currentPos] != null) {
				pages[currentPos].refreshCurrentGroupNow();
			}
			break;
		case R.id.more_menu:
			//TODO
			break;
		}
	}
	
	@Override
	protected void onStandardMenuSelected(Object obj, int pos, long id) {
		super.onStandardMenuSelected(obj, pos, id);
		try {
			JSONObject json = (JSONObject) obj;
			if (json != null) {
				String name = json.getString("GroupName");
				//String desc = json.getString("GroupDesc");
				title.setText(name);
				//MainApp.selectStore(id, name, desc);
				mMonitorPager.refreshData(id);
				mControlPager.refreshData(id);
				mAlarmPager.refreshData(id);
				//adapter.notifyDataSetChanged();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onAlarm(int alarmCount, Map<Long, Integer> alarmGroup) {
		//log("alarm count=" + alarmCount);
		UI.updateStandardSlidingMenu();
		if (alarmCount > 0) {
			if (alarmCount > 99) {
				alarm_count.setText("99+");
			} else {
				alarm_count.setText("" + alarmCount);
			}
			alarm_count.setVisibility(View.VISIBLE);
		} else {
			alarm_count.setText("0");
			alarm_count.setVisibility(View.GONE);
		}
	}
	
	@Override
	public void onRefresh(Pager pager) {
		/*if (pager == mMonitorPager) {
			log("Refresh pager of " + pager.toString());
			//adapter.notifyDataSetChanged();
		}*/
		// TODO
	}
	
	@Override
	public void onSwitchDecide(int position) {
		// pager switched
		//log("onSwitch: cur=" + currentPos + ",pos=" + position);
		if (position == 0) {
			mViewFlow.setInterceptXMarginInDp(10000, 0);
		} else {
			mViewFlow.setInterceptXMarginInDp(48, 0);
		}
		if (position == currentPos) return;
		TextView selectTabs = views[position];
		TextView unSelectTabs = views[currentPos];
		Drawable selectDrawable = getResources().getDrawable(tabsDrawableSelectedRes[position]);
		Drawable unSelectDrawable = getResources().getDrawable(tabsDrawableUnselectedRes[currentPos]);
		selectTabs.setCompoundDrawablesWithIntrinsicBounds(null, selectDrawable, null, null);
		unSelectTabs.setCompoundDrawablesWithIntrinsicBounds(null, unSelectDrawable, null, null);
		selectTabs.setTextColor(0xffffffff);
		unSelectTabs.setTextColor(0x99ffffff);
		currentPos = position;
	}
	
	public static class ViewFlowAdapter extends BaseAdapter {
		
		private MainActivity activity;

		public ViewFlowAdapter(MainActivity context) {
			activity = context;
		}
		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return 4;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				switch(position) {
				case 0:
				case 1:
				case 3:
					convertView = activity.pages[position].getContentView();
					break;
				default:
					convertView = new android.widget.TextView(activity);
				}
			}
			return convertView;
		}
		
	}

}
