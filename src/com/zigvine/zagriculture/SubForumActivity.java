package com.zigvine.zagriculture;

import httpimage.HttpImageManager;
import httpimage.HttpImageManager.LoadRequest;

import java.io.File;
import java.io.IOException;
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

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SubForumActivity extends UIActivity<SubForumActivity>
		implements ResponseListener {
	
	private static final int POST_ID = 0x100;
	private static final int POST_REQUEST = 0x100;
	
	public static final String EXTRA_PID = SubForumActivity.class.getName() + "_extra_pid";
	public static final String EXTRA_OBJ = SubForumActivity.class.getName() + "_extra_obj";
	public static final String EXTRA_CNT = SubForumActivity.class.getName() + "_extra_cnt";
	
	public class DataItem {
		public JSONObjectExt json = null;
		public long id;
		public String date;
		public DataItem(JSONObject j) {
			json = new JSONObjectExt(j);
			id = json.getLong("Id", -1l);
			date = json.getString("Timestamp", Utils.DATETIME.format(new Date()));
		}
	}
	
	public class DataArray extends ArrayList<DataItem> {
		public static final long serialVersionUID = 1L;
		
		public DataArray(Resp resp) {
			if (resp != null) {
				append(resp);
			}
		}
		
		public void append(Resp resp) {
			try {
				JSONArray arr = resp.json.getJSONArray("ConsultationList");
				int len = arr.length();
				mReachEnd = len < mCount;
				for (int i = 0; i < len; i++) {
					JSONObject obj = arr.getJSONObject(i);
					DataItem g = new DataItem(obj);
					add(g);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	TextView title;
	EditText postContent;
	View titleMain, refreshMenu, mLoadingView;
	ImageView titleMenu;
	ListView list;
	DataArray cachedData;
	Request request;
	int requestId;
	long mPID;
	int mCount, mChildCount;
	SubForumListAdapter adapter;
	boolean mReachEnd;
	JSONObjectExt parentObj;
	boolean isUpdated;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UI.setContentView(R.layout.main_subforum);
		UI.requestSignIn();
		UI.setBackNavVisibility(View.VISIBLE);
		//UI.setParentBackground(R.drawable.light_bg);
		UI.setMainBackground(R.drawable.light_bg);
		//findViewById(R.id.main_parent).setBackgroundColor(0xffc5c5c5);
		UI.setupMoreMenu(null);
		
		title = (TextView) findViewById(R.id.title_text);
		title.setText("");
		
		titleMain = findViewById(R.id.title_main);
		titleMain.setOnClickListener(this);
		
		titleMenu = (ImageView) findViewById(R.id.title_menu);
		titleMenu.setImageResource(android.R.drawable.ic_menu_view);
		
		refreshMenu = findViewById(R.id.refresh_menu);
		refreshMenu.setVisibility(View.VISIBLE);
		refreshMenu.setOnClickListener(this);
		
		list = (ListView) findViewById(R.id.subforum_list_view);
		list.setDivider(null);
		list.setDividerHeight(0);
		//list.setSelector(R.color.transparent);

		adapter = new SubForumListAdapter();
		list.setAdapter(adapter);
		isUpdated = false;
		
		postContent = (EditText) findViewById(R.id.subforum_text);
		findViewById(R.id.subforum_submit).setOnClickListener(this);
		
		mLoadingView = findViewById(R.id.subforum_loading);
		mLoadingView.setVisibility(View.GONE);
		
		initAllData(getIntent());

	}
	
	private void initAllData(Intent intent) {
		
		String obj = intent.getStringExtra(EXTRA_OBJ);
		try {
			JSONObject json = new JSONObject(obj);
			parentObj = new JSONObjectExt(json);
			String subject = parentObj.getString("Subject", "");
			mChildCount = parentObj.getInt("ChildCount", 0);
			title.setText(subject);
		} catch (JSONException e) {
			finish();
			UI.toast("无法加载内容");
			e.printStackTrace();
			return;
		}
		
		mPID = intent.getLongExtra(EXTRA_PID, 0l);
		mCount = 5;
		mReachEnd = false;
		cachedData = new DataArray(null);
		adapter.notifyDataSetChanged();
		
		onLoadMore(false);
	}
	
	/**
	 * fetch data as weibo dose
	 * @param count max count to fetch
	 * @param parentid consult_id value
	 * @param firstid if want to fetch previous data, use firstid, -1 means ignore this arg
	 * @param lastid if want to fetch following data, use lastid, -1 means ignore this arg
	 * @param index -1 means load more, 0 means fetch the latest data, positive means insert new data
	 */
	public void fetchData(int count, long parentid, long lastid, boolean scrollEnd) {
		if (!lastRequestInProgress()) {
			requestId++;
			request = new Request(Request.GetConsu, true);
			request.setParam("parent_id", parentid + "");
			request.setParam("count", count + "");
			request.setParam("order", "1");
			if (lastid > -1) {
				request.setParam("last_id", lastid + "");
			}
			request.asyncRequest(this, requestId, scrollEnd);
		} else {
			loadingEnd();
		}
	}
	
	private boolean lastRequestInProgress() {
		if (request != null && request.isOnFetching()) {
			return true;
		}
		return false;
	}
	
	private void loadMoreData(int count, long lastid, boolean scrollEnd) {
		fetchData(count, mPID, lastid, scrollEnd);
	}
	
	@Override
	public void onClick(View v) {
		Intent intent;
		switch(v.getId()) {
			case R.id.title_main:
				finish();
				break;
			case R.id.refresh_menu:
				// TODO loading animation
				onLoadMore(false);
				if (mLoadingView.getVisibility() != View.VISIBLE) {
					mLoadingView.clearAnimation();
					AnimUtils.DropIn.startAnimation(mLoadingView, 300);
				}
				//list.removeFooterView(v);
				break;
			case R.id.subforum_submit:
				sendPrepare();
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
		if (isUpdated) {
			Intent intent = new Intent();
			intent.putExtra(EXTRA_CNT, mChildCount);
			setResult(RESULT_OK, intent);
		}
		super.finish();
		overridePendingTransition(R.anim.static_anim, R.anim.slide_out_to_right);
	}

	public void onLoadMore(boolean scrollEnd) {
		long lastid = -1;
		if (cachedData != null) {
			if (cachedData.size() > 0) {
				DataItem g = cachedData.get(cachedData.size() - 1);
				lastid = g.json.getLong("Id", -1);
			}
		}
		loadMoreData(mCount, lastid, scrollEnd);
	}
	
	public class SubForumListAdapter extends BaseAdapter {
		
		private View topicView = null;
		private HttpImageManager imageManager;
		
		public SubForumListAdapter() {
			imageManager = MainApp.getHttpImageManager();
		}
				
		@Override
		public int getCount() {
			if (cachedData != null) {
				int count = cachedData.size();
				if (count > mChildCount) {
					mChildCount = count;
				}
				return count + 1;
			}
			return 1;
		}

		@Override
		public Object getItem(int position) {
			if (position > 0) {
				if (cachedData != null) {
					return cachedData.get(position - 1);
				}
			}
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (position == 0) {
				if (topicView == null) {
					topicView =  View.inflate(UI.getActivity(), R.layout.forum_item, null);
					topicView.setBackgroundColor(0xddffffff);
					topicView.setPadding(0, 0, 0, 0);
					convertView = topicView;
					JSONObjectExt json = parentObj;
					
					String subject = json.getString("Subject", "");
					String content = json.getString("Content", "");
					String time = json.getString("Timestamp", "");
					String photourl = json.getString("PhotoUrl", null);
					TextView tv = (TextView) convertView.findViewById(R.id.forum_subject);
					tv.setText(subject);
					tv = (TextView) convertView.findViewById(R.id.forum_content);
					tv.setText(content);
					tv = (TextView) convertView.findViewById(R.id.forum_time);
					tv.setText(time);
					
					final ImageView iv = (ImageView) convertView.findViewById(R.id.forum_image);
					
					if (photourl == null) {
						topicView.findViewById(R.id.forum_image_frame).setVisibility(View.GONE);
					} else {
						photourl = Request.HOST + photourl;
						final Uri uri = Uri.parse(photourl);
						topicView.findViewById(R.id.forum_image_frame).setVisibility(View.VISIBLE);
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
								showBitmapForView(v, data);
								return false;
							}
							
						}));
						if (bitmap != null) {
							showBitmapForView(iv, bitmap);
						}
					}
				}
				convertView = topicView;
				TextView tv = (TextView) convertView.findViewById(R.id.forum_child_count);
				tv.setText(mChildCount + "条回复");
				return convertView;
			}

			final DataItem g = (DataItem) getItem(position);
			if (convertView == null || convertView == topicView) {
				convertView = View.inflate(UI.getActivity(), R.layout.forum_feedback_item, null);
			}
			// animation begin ---------------
			if (position == Math.max(0, getCount() - mCount) && !mReachEnd) {
				onLoadMore(false);
			}
			// animation end --------------
			
			JSONObjectExt json = g.json;
			
			String content = json.getString("Content", "");
			
			TextView tv = (TextView) convertView.findViewById(R.id.subitem_content);
			tv.setText(content);
			tv = (TextView) convertView.findViewById(R.id.subitem_time);
			tv.setText(g.date);
			
			// View divider = convertView.findViewById(R.id.subitem_divider);
			
			/*if (position != 1) {
				divider.setVisibility(View.VISIBLE);
			} else {
				divider.setVisibility(View.GONE);
			}*/
			
			return convertView;
		}
		
		private void showBitmapForView(ImageView v, final Bitmap bitmap) {
			v.setImageBitmap(bitmap);
			Animation a = AnimUtils.FadeIn.loadAnimation(UI.getActivity(), 500);
			a.setStartOffset(500);
			v.startAnimation(a);
			topicView.findViewById(R.id.forum_image_btn).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					File f;
					try {
						f = Utils.saveTmpBitmap(bitmap);
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setDataAndType(Uri.fromFile(f), "image/*");
						startActivity(intent);
					} catch (IOException e) {
						e.printStackTrace();
						UI.toast("图片保存出错");
					} catch (ActivityNotFoundException e) {
						UI.toast("无法查看图片，无图库程序");
					}
					
				}
			});
		}
	}

	@Override
	public void onResp(int id, Resp resp, Object... obj) {
		if (id != requestId) return;
		boolean scrollEnd = (Boolean) obj[0];
		if (resp.success) {
			if (cachedData == null) {
				cachedData = new DataArray(resp);
			} else {
				cachedData.append(resp);
			}
			adapter.notifyDataSetChanged();
			if (scrollEnd) {
				list.setSelection(adapter.getCount() - 1);
			}
		}
		loadingEnd();
	}

	@Override
	public void onErr(int id, String err, int httpCode, Object... obj) {
		if (id != requestId) return;
		UI.toast(err);
		loadingEnd();
	}
	
	private void loadingEnd() {
		if (mLoadingView.getVisibility() == View.VISIBLE) {
			AnimUtils.DropOut.startAnimation(mLoadingView, 300);
		}
	}
	
	private void sendPrepare() {
		String postT = parentObj.getString("Subject", "");
		String postC = postContent.getText().toString();
		if (postC.length() == 0) {
			postContent.requestFocus();
			UI.toast("内容不能为空");
			return;
		}
		File upload = null;
		/*if (frame.getVisibility() == View.VISIBLE) {
			Bitmap bitmap = null;
			try {
				if (contentUri != null) {
					bitmap = resizeBitmap(getContentResolver(), contentUri, 400, 300);
				} else if (outputFileUri != null) {
					bitmap = resizeBitmap(outputFileUri, 400, 300);
				}
			} catch (FileNotFoundException e) {}
			if (bitmap == null) {
				UI.toast("图片已经不存在，请重新选择");
				return;
			}
			upload = new File(MainApp.getOutCacheDir(), TEMP_IMAGE);
			try { 
				FileOutputStream out = new FileOutputStream(upload);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
			} catch (Exception e) { e.printStackTrace(); }
		}*/
		sendNow(postT, postC, upload);
	}
	
	private void sendNow(final String postT, final String postC, final File upload) {
		UI.hideInputMethod();
		findViewById(R.id.subforum_submit).setEnabled(false);
		final Request request = new Request(Request.SUBMITCONSU);
		request.setParam("subject", postT);
		request.setParam("content", postC);
		request.setParam("parent_id", mPID + ""); // TODO to indicate this is a new subject a just a sub response to a parent subject
		if (upload != null) {
			request.setSoTimeout(30000);
			request.setFile("plant_photo", upload);
		}
		
		final ProgressDialog pd = new ProgressDialog(this);
    	pd.setMessage("正在发表提问");
    	pd.setCanceledOnTouchOutside(false);
    	pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				request.shutdown();
				findViewById(R.id.subforum_submit).setEnabled(true);
			}
		});
    	pd.show();
		
		request.asyncRequest(new Request.ResponseListener() {
			@Override
			public void onResp(int id, Resp resp, Object... obj) {
				if (resp != null) {
					if (resp.success) {
						UI.toast("评论成功");
						isUpdated = true;
						// finish();
						mChildCount++;
						adapter.notifyDataSetChanged();
						onLoadMore(true);
						postContent.setText("");
					} else {
						UI.toast("回帖失败");
					}
					pd.dismiss();
				}
				findViewById(R.id.subforum_submit).setEnabled(true);
			}
			
			@Override
			public void onErr(int id, String err, int httpCode, Object... obj) {
				pd.dismiss();
				UI.toast(err);
				findViewById(R.id.subforum_submit).setEnabled(true);
			}
		}, 0);
	}
	

}
