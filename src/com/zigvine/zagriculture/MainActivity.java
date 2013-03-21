package com.zigvine.zagriculture;

import org.json.JSONObject;
import org.taptwo.android.widget.SimpleFlowIndicator;
import org.taptwo.android.widget.ViewFlow;
import org.taptwo.android.widget.ViewFlow.SwitchDecideListener;

import com.zigvine.android.http.Request.Resp;
import com.zigvine.android.http.Request.ResponseListener;
import com.zigvine.android.widget.MonitorPager;
import com.zigvine.android.widget.Pager;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MainActivity extends UIActivity<MainActivity>
		implements ResponseListener, SwitchDecideListener {
	
	ViewFlow mViewFlow;
	ViewFlowAdapter adapter;
	SimpleFlowIndicator indicator;
	TextView monitor, control, graph, alarm, title;
	TextView[] views;
	View moreMenu;
	int[] tabsDrawableUnselectedRes;
	int[] tabsDrawableSelectedRes;
	int currentPos;
	
	Pager[] pages = new Pager[4];
	MonitorPager mMonitorPager;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mMonitorPager = new MonitorPager(this);
		pages[0] = mMonitorPager;
		//pages[1] = mMonitorPager;
		
		UI.createStandardSlidingMenu();
		
		title = (TextView) findViewById(R.id.title_text);
		title.setText(getTitle());
		title.setOnClickListener(this);
		
		moreMenu = findViewById(R.id.more_menu);
		moreMenu.setOnClickListener(this);
		
		// content viewflow
        mViewFlow = (ViewFlow) findViewById(R.id.pages);
		adapter = new ViewFlowAdapter(this);
		mViewFlow.setAdapter(adapter, 0);
		currentPos = 0;
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
				//adapter.notifyDataSetChanged();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onResp(int id, Resp resp) {
		log(resp.json.toString());
		switch (id) {
		
		}
			
	}

	@Override
	public void onErr(int id, String err, int httpCode) {
		log(err);
		switch (id) {
		
		}
	}
	
	@Override
	public void onRefresh(Pager pager) {
		if (pager == mMonitorPager) {
			log("Refresh pager of " + pager.toString());
			//adapter.notifyDataSetChanged();
		}
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
				if (position == 0) {
					convertView = activity.pages[position].getContentView();
				} else {
					convertView = new android.widget.TextView(activity);
				}
			}
			return convertView;
		}
		
	}

}
