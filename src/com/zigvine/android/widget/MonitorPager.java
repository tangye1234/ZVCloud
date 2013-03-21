package com.zigvine.android.widget;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zigvine.android.anim.AnimUtils;
import com.zigvine.android.http.Request;
import com.zigvine.android.http.Request.Resp;
import com.zigvine.android.http.Request.ResponseListener;
import com.zigvine.android.utils.Quota;
import com.zigvine.android.utils.Utils;
import com.zigvine.zagriculture.R;
import com.zigvine.zagriculture.UIActivity;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MonitorPager extends Pager
		implements ResponseListener, OnClickListener {

	View loader;
	ListView list;
	TextView refresh;
	Request request;
	int requestId;
	long currentGroup;
	MonitorAdapter adapter;
	Map<Long, Resp> cachedData = new ConcurrentHashMap<Long, Resp>();
	
	public MonitorPager(UIActivity<?> context) {
		super(context);
		currentGroup = -1;
	}

	@Override
	public void onCreate() {
		setContentView(R.layout.pager_monitor);
		list = (ListView) findViewById(R.id.monitor_list_view);
		list.setDivider(null);
		list.setDividerHeight(0);
		list.setEmptyView(findViewById(R.id.monitor_empty));
		loader = findViewById(R.id.monitor_loading);
		loader.setVisibility(View.GONE);
		adapter = new MonitorAdapter(mContext);
		list.setAdapter(adapter);
		refresh = (TextView) findViewById(R.id.monitor_refresh);
		refresh.setOnClickListener(this);
		refresh.setVisibility(View.GONE);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.monitor_refresh:
			refreshCurrentGroupNow();
			break;
		}
	}
	
	public void refreshData(long groupid) {
		refreshData(groupid, false);
	}
	
	public void refreshCurrentGroupNow() {
		if (currentGroup >= 0) {
			refreshData(currentGroup, true);
		}
	}

	private void refreshData(long groupid, boolean force) {
		if (currentGroup != groupid) {
			currentGroup = groupid;
			adapter.notifyDataSetInvalidated();
		}
		Resp resp = cachedData.get(groupid);
		refresh.setVisibility(View.VISIBLE);
		if (resp != null && !force) {
			// use already cached data
			dropOutLoader();
			currentGroup = groupid;
			ignoreLastRequest();
			String deltaTime = Utils.getDeltaTimeString(resp.time);
			refresh.setText(deltaTime + "前更新");
			adapter.notifyDataSetChanged();
		} else {
			if (loader.getVisibility() == View.GONE) {
				loader.setVisibility(View.VISIBLE);
				AnimUtils.DropIn.startAnimation(loader, 300);
			}
			ignoreLastRequest();
			request = new Request(Request.SnapShotData, true);
			request.setParam("groupID", String.valueOf(groupid));
			request.asyncRequest(this, requestId);
		}
	}
	
	private void ignoreLastRequest() {
		requestId++;
		if (request != null) {
			request.shutdown();
		}
	}
	
	@Override
	public void onResp(int id, Resp resp) {
		if (id != requestId) return;
		dropOutLoader();
		cachedData.put(currentGroup, resp);
		refresh.setText("已更新");
		adapter.notifyDataSetChanged();
		mContext.onRefresh(this);
	}

	@Override
	public void onErr(int id, String err, int httpCode) {
		if (id != requestId) return;
		dropOutLoader();
		mContext.UI.toast(err);
	}
	
	private void dropOutLoader() {
		if (loader.getVisibility() == View.VISIBLE) {
			Animation anim = AnimUtils.DropOut.loadAnimation(mContext, 300);
			anim.setAnimationListener(AnimUtils.DropOut.loadListener(loader));
			anim.setStartOffset(500);
			loader.startAnimation(anim);
		}
	}
	
	public class MonitorAdapter extends BaseAdapter {
		
		private UIActivity<?> activity;

		public MonitorAdapter(UIActivity<?> context) {
			activity = context;
		}
		
		private JSONArray getData() {
			if (cachedData != null) {
				Resp resp = cachedData.get(currentGroup);
				if (resp != null) {
					try {
						return resp.json.getJSONArray("DataList");
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
			return null;
		}
		
		@Override
		public int getCount() {
			JSONArray data = getData();
			if (data != null) {
				return data.length();
			}
			return 0;
		}

		@Override
		public Object getItem(int position) {
			try {
				JSONArray data = getData();
				if (data != null) {
					return data.getJSONObject(position);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public long getItemId(int position) {
			try {
				JSONObject json = (JSONObject) getItem(position);
				String mac = json.getString("DeviceId");
				return Utils.mac2long(mac);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView =  View.inflate(activity, R.layout.monitor_item, null);
			}
			TextView dname = (TextView) convertView.findViewById(R.id.device_name);
			TextView qname = (TextView) convertView.findViewById(R.id.quota_name);
			TextView qvalue = (TextView) convertView.findViewById(R.id.quota_value);
			TextView time = (TextView) convertView.findViewById(R.id.monitor_time);
			ImageView qid = (ImageView) convertView.findViewById(R.id.quotaId);
			View alarm = convertView.findViewById(R.id.alarm_mark);
			if (position == 0) {
				convertView.setBackgroundResource(R.drawable.white_bg_top);
			} else {
				convertView.setBackgroundResource(R.drawable.white_bg);
			}
			try {
				JSONObject json = (JSONObject) getItem(position);
				if (json != null) {
					String s = json.getString("DeviceName");
					dname.setText(s);
					s = json.getString("QuotaName");
					qname.setText(s);
					s = json.getString("Value");
					qvalue.setText(s);
					s = json.getString("Timestamp");
					time.setText(s);
					int id = json.getInt("QuotaId");
					qid.setImageResource(Quota.ICONS[id]);
					int status = json.getInt("AlarmStatus"); // 0:normal, 1:lower, 2:higher
					if (status > 0) {
						qvalue.setTextColor(0xffff0000);
						qvalue.setShadowLayer(20, 0, 0, 0xffff0000);
						alarm.setVisibility(View.VISIBLE);
					} else {
						qvalue.setTextColor(0xff7fe101);//0xfffdb222
						qvalue.setShadowLayer(20, 0, 0, 0xff7fe101);
						alarm.setVisibility(View.GONE);
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return convertView;
		}
		
	}
}
