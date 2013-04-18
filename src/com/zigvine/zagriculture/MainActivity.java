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

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MainActivity extends UIActivity<MainActivity>
		implements SwitchDecideListener, AlarmReceiverListener, UIActivity.MenuListener {
	
	ViewFlow mViewFlow;
	ViewFlowAdapter adapter;
	SimpleFlowIndicator indicator;
	TextView monitor, control, /*graph, */alarm, title, alarm_count;
	TextView[] views;
	View refreshMenu, titleMain;
	int currentPos;
	long currentGroup;
	
	Pager[] pages = new Pager[3];
	MonitorPager mMonitorPager;
	ControlPager mControlPager;
	AlarmPager mAlarmPager;
	boolean refreshOnStart;
	String currentGroupName, currentGroupDesc;
	
	public static final String POSITION_EXTRA = "com.zigvine.zagriculture.jump_position";
	public static final String NOTICE_EXTRA = "com.zigvine.zagriculture.notice"; 
	
	final static int[] tabsDrawableUnselectedRes;
	final static int[] tabsDrawableSelectedRes;
	static {
		tabsDrawableUnselectedRes = new int[] {
				R.drawable.monitor,
				R.drawable.control,
				//R.drawable.graph,
				R.drawable.alarm
			};
		tabsDrawableSelectedRes = new int[] {
				R.drawable.monitor_select,
				R.drawable.control_select,
				//R.drawable.graph_select,
				R.drawable.alarm_select
			};
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UI.setContentView(R.layout.main_viewflow);
		UI.setBackNavVisibility(View.VISIBLE);
		UI.setupFooterView();
		UI.setMainBackground(R.drawable.main_bg_blur);
		
		mMonitorPager = new MonitorPager(this);
		mControlPager = new ControlPager(this);
		mAlarmPager = new AlarmPager(this);
		pages[0] = mMonitorPager;
		pages[1] = mControlPager;
		pages[2] = mAlarmPager;
		
		UI.setupMoreMenu(null);
		UI.createStandardSlidingMenu();
		UI.setStandardSlidingMenuListener(this);
		
		title = (TextView) findViewById(R.id.title_text);
		title.setText(getTitle());
		
		titleMain = findViewById(R.id.title_main);
		titleMain.setOnClickListener(this);
		
		refreshMenu = findViewById(R.id.refresh_menu);
		refreshMenu.setVisibility(View.VISIBLE);
		refreshMenu.setOnClickListener(this);
		
		//currentPos = 0;
		currentGroup = -1;
		
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
		//graph = (TextView) findViewById(R.id.graph);
		alarm = (TextView) findViewById(R.id.alarm);
		alarm_count = (TextView) findViewById(R.id.tab_alarm_count);
		views = new TextView[] {monitor, control, /*graph, */alarm};
		
		for (int i = 0; i < views.length; i++) {
			views[i].setTag(i);
			views[i].setOnClickListener(this);
		}
		
		//get position from intent
		Intent intent = getIntent();
		int pos = intent.getIntExtra(POSITION_EXTRA, 0);
		mViewFlow.setAdapter(adapter, pos); // must init after all
		refreshOnStart = true;
		
		showNoticeIfNeeded(intent);
		
	}
	
	private void showNoticeIfNeeded(Intent intent) {
		String[] arr = intent.getStringArrayExtra(NOTICE_EXTRA);
		if (arr != null && arr.length == 2) {
			String title = arr[0];
			String content = arr[1];
			if (content != null && content.length() > 0) {
				new AlertDialog.Builder(this)
				.setTitle(title)
				.setMessage(content)
				.setIcon(R.drawable.ic_dialog_info)
				.setPositiveButton(android.R.string.ok, null)
				.show();
			}
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		int pos = intent.getIntExtra(POSITION_EXTRA, 0);
		mViewFlow.setSelection(pos);
		if (pages[currentPos] != null) {
			pages[currentPos].refreshCurrentGroupNow();
		}
		refreshOnStart = true;
		// FIXME if use just enter it without new intent, than no tabs will be refreshed
		log("new Intent arrives");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		MainApp.registerAlarmReceiver(this, true);
		if (!refreshOnStart) {
			// FIXME if the interval is too short, we no longer need refresh
			if (pages[currentPos] != null) {
				pages[currentPos].notifyLastRefreshTime();
			}
		} else {
			refreshOnStart = false;
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		MainApp.unRegisterAlarmReceiver(this);
	}
	
	@Override
	public void finish() {
		super.finish();
		UI.startOnlineService(true);
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
		//case R.id.graph:
		case R.id.alarm:
			mViewFlow.setSelection((Integer) view.getTag());
			break;
		case R.id.title_main:
			UI.toggleStandardMenu();
			break;
		case R.id.refresh_menu:
			if (pages[currentPos] != null) {
				pages[currentPos].refreshCurrentGroupNow();
			}
			break;
		}
	}
	
	@Override
	protected void onStandardMenuSelected(Object obj, int pos, long id) {
		super.onStandardMenuSelected(obj, pos, id);
		try {
			JSONObject json = (JSONObject) obj;
			if (json != null) {
				currentGroupName = json.getString("GroupName");
				currentGroupDesc = json.getString("GroupDesc");
				title.setText(currentGroupName);
				// TODO too much data connection here
				//MainApp.selectStore(id, name, desc);
				currentGroup = id;
				for (int i = 0; i < pages.length; i++) {
					Pager page = pages[i];
					if (page != null) {
						if (i == currentPos) {
							page.refreshData(currentGroup);
						} else {
							page.refreshDataWithoutFetch(currentGroup);
						}
					}
				}
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
		String tab = "";
		if (pager == mMonitorPager) {
			tab = getString(R.string.monitor);
		} else if (pager == mControlPager) {
			tab = getString(R.string.control);
		} else if (pager == mAlarmPager) {
			tab = getString(R.string.alarm);
		}
		pager.setEmptyViewText("【" + currentGroupName + "】中没有" + tab + "项目");
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
		selectTabs.setTextColor(0xffff9000);
		unSelectTabs.setTextColor(0xffffffff);
		currentPos = position;
		if (currentGroup >= 0 && pages[currentPos] != null) {
			pages[currentPos].refreshData(currentGroup);
		}
	}
	
	public static class ViewFlowAdapter extends BaseAdapter {
		
		private MainActivity activity;

		public ViewFlowAdapter(MainActivity context) {
			activity = context;
		}
		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return activity.pages.length;
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
				case 2:
					convertView = activity.pages[position].getContentView();
					break;
				default:
					convertView = new android.widget.TextView(activity);
					((TextView) convertView).setText("datagram");
				}
			}
			return convertView;
		}
		
	}

	@Override
	public void onOpened() {
		UI.setBackNavVisibility(View.INVISIBLE);
	}

	@Override
	public void onClosed() {
		UI.setBackNavVisibility(View.VISIBLE);
	}

}
