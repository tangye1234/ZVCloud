package com.zigvine.zagriculture;

import httpimage.HttpImageManager;
import httpimage.HttpImageManager.LoadRequest;

import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zigvine.android.anim.AnimUtils;
import com.zigvine.android.http.Request;
import com.zigvine.android.http.Request.Resp;
import com.zigvine.android.http.Request.ResponseListener;
import com.zigvine.android.utils.JSONObjectExt;
import com.zigvine.android.utils.Utils;

import me.maxwin.view.XListView;
import me.maxwin.view.XListView.IXListViewListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController.AnimationParameters;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ForumActivity extends UIActivity<ForumActivity>
		implements IXListViewListener, ResponseListener, OnItemClickListener {
	
	private static final int POST_ID = 0x100;
	private static final int POST_REQUEST = 0x100;
	private static final int SUBPOST_REQUEST = 0x101;
	
	public class DataItem {
		public JSONObjectExt json = null;
		public int left;
		public long id;
		public Date date;
		public boolean isLoading;
		public int animationState;
		public int imageLoadState;
		public int childCount;
		public DataItem(JSONObject j) {
			json = new JSONObjectExt(j);
			left = 0;
			id = json.getLong("Id", -1l);
			childCount = json.getInt("ChildCount", 0);
			date = new Date();
			isLoading = false;
			animationState = 0;
			imageLoadState = 0;
		}
		public DataItem(int leftCount) {
			if (leftCount == 0) throw new IllegalArgumentException("the left count should be bigger than 0");
			left = leftCount;
			id = -1;
			date = null;
			isLoading = false;
			childCount = 0;
			animationState = 0;
			imageLoadState = 0;
		}
	}
	
	public class DataArray extends ArrayList<DataItem> {
		public static final long serialVersionUID = 1L;
		
		public DataArray(Resp resp) {
			append(resp);
		}
		
		public void append(Resp resp) {
			try {
				JSONArray arr = resp.json.getJSONArray("ConsultationList");
				int len = arr.length();
				mReachEnd = len < mCount;
				list.setPullLoadEnable(mReachEnd);
				for (int i = 0; i < len; i++) {
					JSONObject obj = arr.getJSONObject(i);
					DataItem g = new DataItem(obj);
					add(g);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		public void prepend(Resp resp) {
			try {
				JSONArray arr = resp.json.getJSONArray("ConsultationList");
				int i = 0;
				for (; i < arr.length(); i++) {
					JSONObject obj = arr.getJSONObject(i);
					DataItem g = new DataItem(obj);
					g.animationState = 2;
					add(i, g);
				}
				int left = resp.json.getInt("Left");
				if (left > 0) {
					DataItem g = new DataItem(left);
					g.animationState = 2;
					add(i, g);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		public void insert(Resp resp, int index) {
			DataItem mid = get(index);
			int left = mid.left;
			if (left == 0) return;
			mid.isLoading = false;
			try {
				JSONArray arr = resp.json.getJSONArray("ConsultationList");
				int i = 0;
				int len = arr.length();
				if (len > left) {
					left = len - left;
				} else {
					left = 0;
					len = left;
				}
				for (; i < len; i++) {
					JSONObject obj = arr.getJSONObject(i);
					DataItem g = new DataItem(obj);
					g.animationState = 2;
					add(i + index, g);
				}
				if (left > 0) {
					mid.left = left;
				} else {
					remove(i);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	TextView title;
	View titleMain, refreshMenu;
	ImageView titleMenu;
	XListView list;
	DataArray cachedData;
	Request request;
	int requestId, tmpIndex;
	long mPID;
	int mCount;
	ForumListAdapter adapter;
	boolean mReachEnd;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UI.setContentView(R.layout.main_forum);
		UI.requestSignIn();
		UI.setBackNavVisibility(View.VISIBLE);
		//UI.setParentBackground(R.drawable.light_bg);
		UI.setMainBackground(R.drawable.light_bg);
		//findViewById(R.id.main_parent).setBackgroundColor(0xffc5c5c5);
		UI.setupMoreMenu(null);
		
		UI.addCustomMenuIcon(R.drawable.ic_menu_compose, "提问").setId(POST_ID);
		
		title = (TextView) findViewById(R.id.title_text);
		title.setText("所有提问");
		
		titleMain = findViewById(R.id.title_main);
		titleMain.setOnClickListener(this);
		
		titleMenu = (ImageView) findViewById(R.id.title_menu);
		titleMenu.setImageResource(R.drawable.ic_menu_view);
		
		refreshMenu = findViewById(R.id.refresh_menu);
		refreshMenu.setVisibility(View.VISIBLE);
		refreshMenu.setOnClickListener(this);
		
		list = (XListView) findViewById(R.id.forum_list_view);
		list.setDivider(null);
		list.setDividerHeight(0);
		//list.setEmptyView(findViewById(R.id.monitor_empty));
		//list.setLayoutAnimation(layoutAnim);
		list.setPullRefreshEnable(true);
		
		list.setOnItemClickListener(this);

		adapter = new ForumListAdapter();
		list.setAdapter(adapter);
		list.setXListViewListener(this);
		
		initAllData();

	}
	
	private void initAllData() {
		mPID = 0;
		mCount = 5;
		mReachEnd = false;
		list.setPullLoadEnable(false);
		adapter.notifyDataSetInvalidated();
		cachedData = null;
		list.showRefresh();
		onRefresh();
	}
	
	/**
	 * fetch data as weibo dose
	 * @param count max count to fetch
	 * @param parentid consult_id value
	 * @param firstid if want to fetch previous data, use firstid, -1 means ignore this arg
	 * @param lastid if want to fetch following data, use lastid, -1 means ignore this arg
	 * @param index -1 means load more, 0 means fetch the latest data, positive means insert new data
	 */
	public void fetchData(int count, long parentid, long firstid, long lastid, int index) {
		if (!lastRequestInProgress()) {
			requestId++;
			request = new Request(Request.GetConsu, true);
			request.setParam("parent_id", parentid + "");
			request.setParam("count", count + "");
			//request.setDebug(true);
			if (firstid > -1) {
				request.setParam("first_id", firstid + "");
			} else if (lastid > -1) {
				request.setParam("last_id", lastid + "");
			}
			request.asyncRequest(this, requestId, index);
		} else {
			loadingEnd(index);
		}
	}
	
	private boolean lastRequestInProgress() {
		if (request != null && request.isOnFetching()) {
			return true;
		}
		return false;
	}
	
	private void loadMoreData(int count, long lastid) {
		fetchData(count, mPID, -1, lastid, -1);
	}
	
	private void refreshNewDate(int count, long firstid) {
		fetchData(count, mPID, firstid, -1, 0);
	}
	
	@Override
	public void onClick(View v) {
		Intent intent;
		switch(v.getId()) {
			case R.id.title_main:
				finish();
				break;
			case R.id.refresh_menu:
				initAllData();
				break;
			case POST_ID:
				intent = new Intent(this, PostActivity.class);
				startActivityForResult(intent, POST_REQUEST);
				overridePendingTransition(R.anim.slide_in_from_right, R.anim.static_anim);
				break;
		}
	}
	
	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.static_anim, R.anim.slide_out_to_right);
	}

	@Override
	public void onRefresh() {
		long firstid = -1;
		if (cachedData != null) {
			if (cachedData.size() > 0) {
				DataItem g = cachedData.get(0);
				firstid = g.json.getLong("Id", -1);
			}
		}
		refreshNewDate(mCount, firstid);
	}

	@Override
	public void onLoadMore() {
		long lastid = -1;
		if (cachedData != null) {
			if (cachedData.size() > 0) {
				DataItem g = cachedData.get(cachedData.size() - 1);
				lastid = g.json.getLong("Id", -1);
			}
		}
		loadMoreData(mCount, lastid);
	}
	
	public class ForumListAdapter extends BaseAdapter {
		
		private int paramsCount = 0;
		
		@Override
		public int getCount() {
			if (cachedData != null) {
				return cachedData.size();
			}
			return 0;
		}

		@Override
		public Object getItem(int position) {
			if (cachedData != null) {
				return cachedData.get(position);
			}
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final DataItem g = (DataItem) getItem(position);
			if (convertView == null) {
				convertView =  View.inflate(UI.getActivity(), R.layout.forum_item, null);
				convertView.findViewById(R.id.forum_image_btn).setVisibility(View.GONE); // FIXME?
			}
			if (position == 0) {
				convertView.setBackgroundResource(R.drawable.pageritem_bg_top);
			} else {
				convertView.setBackgroundResource(R.drawable.pageritem_bg);
			}
			//convertView.setClickable(true);
			// animation begin ---------------
			ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) convertView.getLayoutParams();
			if (lp != null) {
				AnimationParameters params = lp.layoutAnimationParameters;
				if (params != null) {
					paramsCount = params.count = Math.max(paramsCount, params.index);
				}
			}
			if (position == Math.max(0, getCount() - mCount) && !mReachEnd) {
				onLoadMore();
			}
			if (g.animationState != 1) {
				convertView.clearAnimation();
			}
			if (g.animationState == 0) {
				Animation anim = AnimationUtils.loadAnimation(UI.getActivity(), R.anim.list_anim);
				anim.setAnimationListener(new AnimationListener() {
					@Override
					public void onAnimationEnd(Animation animation) {
						g.animationState = 2;
					}

					@Override
					public void onAnimationRepeat(Animation animation) {}

					@Override
					public void onAnimationStart(Animation animation) {}
					
				});
				convertView.startAnimation(anim);
				g.animationState = 1;
			}
			// animation end --------------
			
			JSONObjectExt json = g.json;
			
			if (g.left != 0) {
				// TODO show loading or not for inserting
			} else {
				int MAX = 40;
				String subject = json.getString("Subject", "");
				String content = json.getString("Content", "");
				if (content.length() > MAX) {
					content = content.substring(0, MAX - 3) + "…";
				}
				String time = json.getString("Timestamp", "");
				String photourl = json.getString("PhotoUrl", null);
				//String child = json.getString("ChildCount", "0");
				TextView tv = (TextView) convertView.findViewById(R.id.forum_subject);
				tv.setText(subject);
				tv = (TextView) convertView.findViewById(R.id.forum_content);
				tv.setText(content);
				tv = (TextView) convertView.findViewById(R.id.forum_time);
				tv.setText(time);
				tv = (TextView) convertView.findViewById(R.id.forum_child_count);
				tv.setText(g.childCount + "条回复");
				
				final ImageView iv = (ImageView) convertView.findViewById(R.id.forum_image);
				if (photourl == null) {
					iv.setTag(null);
					iv.clearAnimation();
					g.imageLoadState = 0;
					convertView.findViewById(R.id.forum_image_frame).setVisibility(View.GONE);
					iv.setImageResource(R.color.transparent);
				} else {
					photourl = Request.HOST + photourl;
					convertView.findViewById(R.id.forum_image_frame).setVisibility(View.VISIBLE);
					HttpImageManager imageManager = MainApp.getHttpImageManager();
					final Uri uri = Uri.parse(photourl);
					if (!uri.equals(iv.getTag())) {
						iv.setImageResource(R.color.transparent);
					}
					Bitmap bitmap = null;
					bitmap = imageManager.loadImage(new HttpImageManager.LoadRequest(uri, iv, new HttpImageManager.OnLoadResponseListener() {

						@Override
						public void onLoadResponse(LoadRequest r,
								final Bitmap data) {}
						
						@Override
						public void onLoadProgress(LoadRequest r,
								long totalContentSize,
								long loadedContentSize) {}

						@Override
						public void onLoadError(LoadRequest r, Throwable e) {
							iv.setImageResource(R.color.transparent);
						}

						@Override
						public boolean onBeforeSetImageBitmap(ImageView v,
								Bitmap data) {
							showBitmapForView(g, v, data);
							return false;
						}
						
					}));
					if (bitmap != null) {
						showBitmapForView(g, iv, bitmap);
					}
				}
			}
			
			return convertView;
		}
		
		private void showBitmapForView(final DataItem g, ImageView v, Bitmap bitmap) {
			v.setImageBitmap(bitmap);
			if (g.imageLoadState == 0) {
				v.clearAnimation();
				g.imageLoadState = 1;
				Animation a = AnimUtils.FadeIn.loadAnimation(UI.getActivity(), 500);
				a.setAnimationListener(AnimUtils.loadEndListener(v, View.VISIBLE, new Runnable() {
					public void run() {
						g.imageLoadState = 2;
					}
				}));
				a.setStartOffset(500);
				v.startAnimation(a);
			} else if (g.imageLoadState == 2) {
				v.clearAnimation();
			}
		}
		
	}

	@Override
	public void onResp(int id, Resp resp, Object... obj) {
		if (id != requestId) return;
		int index = (Integer) obj[0];
		if (resp.success) {
			if (cachedData == null) {
				list.startLayoutAnimation();
				cachedData = new DataArray(resp);
			} else {
				switch(index) {
				case -1:
					cachedData.append(resp);
					break;
				case 0:
					cachedData.prepend(resp);
					break;
				default:
					cachedData.insert(resp, index);
					// TODO insertEnd();
					break;
				}
			}
			list.setRefreshTime(Utils.DATETIME.format(resp.time));
			adapter.notifyDataSetChanged();
		}
		loadingEnd(index);
	}

	@Override
	public void onErr(int id, String err, int httpCode, Object... obj) {
		if (id != requestId) return;
		int index = (Integer) obj[0];
		UI.toast(err);
		loadingEnd(index);
	}
	
	private void loadingEnd(int index) {
		if (index == -1) {
			list.stopLoadMore();
		} else if (index == 0) {
			list.stopRefresh();
		} else {
			// TODO end insert;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == POST_REQUEST) {
			if (resultCode == RESULT_OK) {
				list.showRefresh();
				onRefresh();
				return;
			}
		}
		if (requestCode == SUBPOST_REQUEST) {
			if (resultCode == RESULT_OK) {
				if (data != null) {
					DataItem g = (DataItem) adapter.getItem(tmpIndex);
					int count = data.getIntExtra(SubForumActivity.EXTRA_CNT, g.childCount);
					if (count != g.childCount) {
						g.childCount = count;
						adapter.notifyDataSetChanged();
					}
					return;
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
		pos = (int) id; // very important
		DataItem g = (DataItem) adapter.getItem(pos);
		if (g == null) return;
		if (g.left == 0) {
			Intent intent = new Intent(this, SubForumActivity.class);
			intent.putExtra(SubForumActivity.EXTRA_PID, g.id);
			intent.putExtra(SubForumActivity.EXTRA_OBJ, g.json.toString());
			startActivityForResult(intent, SUBPOST_REQUEST);
			tmpIndex = pos;
			overridePendingTransition(R.anim.slide_in_from_right, R.anim.static_anim);
		} else {
			// TODO refresh inserting
			UI.toast("加载中间的内容，请点击刷新");
		}
		
	}

}
