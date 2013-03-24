package com.zigvine.android.widget;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.maxwin.view.XListView;
import me.maxwin.view.XListView.IXListViewListener;

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
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ControlPager extends Pager
		implements ResponseListener, IXListViewListener {

	XListView list;
	TextView refresh, loader;
	Request request;
	int requestId;
	long currentGroup;
	MonitorAdapter adapter;
	Map<Long, Resp> cachedData = new ConcurrentHashMap<Long, Resp>();
	Runnable fadeOutRefresh = new Runnable() {
		@Override
		public void run() {
			if (refresh != null && refresh.getVisibility() == View.VISIBLE) {
				Animation anim = AnimUtils.FadeOut.loadAnimation(mContext, 300);
				anim.setAnimationListener(AnimUtils.loadEndListener(refresh, View.GONE));
				refresh.startAnimation(anim);
			}
		}
	};
	
	final static long Loading_Disappear_Delay_Ms = 500l;
	final static long Refreshed_Disappear_Delay_Ms = 1000l;
	
	public ControlPager(UIActivity<?> context) {
		super(context);
		currentGroup = -1;
	}

	@Override
	public void onCreate() {
		setContentView(R.layout.pager_monitor);
		list = (XListView) findViewById(R.id.monitor_list_view);
		list.setDivider(null);
		list.setDividerHeight(0);
		list.setEmptyView(findViewById(R.id.monitor_empty));
		list.setPullRefreshEnable(true);
		list.setPullLoadEnable(false);
		loader = (TextView) findViewById(R.id.monitor_loading);
		loader.setVisibility(View.GONE);
		adapter = new MonitorAdapter(mContext);
		list.setAdapter(adapter);
		refresh = (TextView) findViewById(R.id.monitor_refresh);
		refresh.setVisibility(View.GONE);
		list.setXListViewListener(this);
	}
	
	public void refreshData(long groupid) {
		refreshData(groupid, false);
	}
	
	@Override
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
		if (resp != null && !force) {
			// use already cached data
			dropOutLoader();
			currentGroup = groupid;
			ignoreLastRequest();
			String deltaTime = Utils.getDeltaTimeString(resp.time);
			refresh.setText(deltaTime + "前更新");
			if (refresh.getVisibility() == View.GONE) {
				AnimUtils.FadeIn.startAnimation(refresh, 300);
			}
			refresh.getHandler().removeCallbacks(fadeOutRefresh);
			refresh.getHandler().postDelayed(fadeOutRefresh, Refreshed_Disappear_Delay_Ms);
			adapter.notifyDataSetChanged();
		} else {
			if (loader.getVisibility() == View.GONE) {
				loader.setVisibility(View.VISIBLE);
				AnimUtils.DropIn.startAnimation(loader, 300);
			}
			ignoreLastRequest();
			request = new Request(Request.GetControl, true);
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
		if (resp.success) {
			dropOutLoader();
			cachedData.put(currentGroup, resp);
			refresh.setText("已更新");
			list.setRefreshTime(Utils.DATETIME.format(resp.time));
			adapter.notifyDataSetChanged();
			mContext.onRefresh(this);
		} else {
			dropOutLoader();
			// fail
		}
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
			anim.setAnimationListener(AnimUtils.loadEndListener(loader, View.GONE));
			anim.setStartOffset(Loading_Disappear_Delay_Ms);
			loader.startAnimation(anim);
		}
		list.stopRefresh();
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
						return resp.json.getJSONArray("data");
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
				String mac = json.getString("deviceId");
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
			alarm.setVisibility(View.GONE);
			if (position == 0) {
				convertView.setBackgroundResource(R.drawable.white_bg_top);
			} else {
				convertView.setBackgroundResource(R.drawable.white_bg);
			}
			try {
				JSONObject json = (JSONObject) getItem(position);
				if (json != null) {
					String s = json.getString("deviceTypeID");
					dname.setText(s);
					s = json.getString("deviceName");
					qname.setText(s);
					JSONArray arr = json.getJSONArray("quota");
					s = arr.getString(json.getInt("num"));
					qvalue.setText(s);
					s = json.getString("date");
					time.setText(s);
					int id = json.getInt("quotaID");
					qid.setImageResource(Quota.ICONS[id]);
					
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return convertView;
		}
		
	}

	@Override
	public void onRefresh() {
		refreshCurrentGroupNow();
	}

	@Override
	public void onLoadMore() {
		
	}
}
