package com.zigvine.android.widget;

import com.zigvine.zagriculture.UIActivity;

import android.util.Log;
import android.view.View;

public abstract class Pager {

	protected UIActivity<?> mContext;
	private View mView;
	private final String TAG;

	public Pager(UIActivity<?> context) {
		mContext = context;
		TAG = getClass().getSimpleName();
		mContext.runOnUiThread(new Runnable() {
			public void run() {
				onCreate();
			}
		});
	}

	public abstract void onCreate();
	
	public abstract void refreshCurrentGroupNow();

	public View getContentView() {
		return mView;
	}

	public View setContentView(int resid) {
		mView = View.inflate(mContext, resid, null);
		return mView;
	}

	public View findViewById(int resid) {
		return mView.findViewById(resid);
	}

	public UIActivity<?> getContext() {
		return mContext;
	}
	
	protected void log(String str) {
		Log.d(TAG, str);
	}
}
