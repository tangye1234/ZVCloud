package com.zigvine.zagriculture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zigvine.android.anim.AnimUtils;
import com.zigvine.android.anim.QueuedTextView;
import com.zigvine.android.http.Request;
import com.zigvine.android.http.Request.Resp;
import com.zigvine.android.http.Request.ResponseListener;
import com.zigvine.android.utils.Utils;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class LoginActivity extends UIActivity<LoginActivity>
		implements ResponseListener {

	View logo, loginPan, loginBtn, loginInput, loginProgress;
	ProgressBar progressBar;
	EditText email, password;
	QueuedTextView load;
	Handler handler;
	Request request;
	
	String mUser;
	JSONArray mGroup;
	
	private static final int RECOVERY = -1;
	private static final int VERIFY_ID = 0;
	private static final int GET_GROUP_LIST_ID = 1;
	
	private static final boolean isPreAccountSet = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (MainApp.isSignIn()) {
			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		
		setContentView(R.layout.activity_login);
		
		email = (EditText) findViewById(R.id.login_user);
		password = (EditText) findViewById(R.id.login_password);
		
		if (isPreAccountSet) {
			email.setText("zigvine");
			password.setText("zigvine123");
		}
		
		logo = findViewById(R.id.logo);
		loginPan = findViewById(R.id.login_pan);
		loginBtn = findViewById(R.id.btn_signin);
		loginInput = findViewById(R.id.login_input);
		loginProgress = findViewById(R.id.login_progress);
		load = (QueuedTextView) findViewById(R.id.loading_text);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		
		Runnable showLogin = new Runnable() {
			public void run() {
				AnimUtils.FadeOut.startAnimation(logo, 300);
				AnimUtils.FadeIn.startAnimation(loginPan, 300);
				loginBtn.setOnClickListener(UI.getActivity());
				
				final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) (loginBtn.getLayoutParams());
				AnimUtils.CustomAnimation.Callback Call_back = new AnimUtils.CustomAnimation.Callback() {
					@Override
					public void process(int value) {
						//log(value + "");
						lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, value);
						loginBtn.requestLayout();
					}
					@Override
					public void onAnimationEnd() {}
				};
				new AnimUtils.CustomAnimation(500, lp.bottomMargin, -Utils.dp2px(UI.getActivity(), 30), Call_back).startAnimation();
				
			}
		};
		
		handler = new Handler();
		handler.postDelayed(showLogin, 1000);
		
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()) {
		case R.id.btn_signin:
			startSign();
			break;
		}
	}
	
	@Override
	public void onBackPressed() {
		if (loginBtn.isEnabled()) {
			super.onBackPressed();
		} else if (request != null) {
			request.shutdown();
			handler.removeCallbacks(reFinish);
			onResp(RECOVERY, null);
		}
	}

	private void startSign() {
		log("__startSign__");
		mUser = email.getText().toString();
		if (mUser.length() == 0) {
			email.setError(getString(R.string.e1));
			return;
		}
		if (password.getText().length() == 0) {
			password.setError(getString(R.string.e2));
			return;
		}
		
		loginBtn.setEnabled(false);
		UI.hideInputMethod();
		AnimUtils.FadeOut.startAnimation(loginInput, 500);
		AnimUtils.FadeIn.startAnimation(loginProgress, 500);
		
		JSONObject requestJson = new JSONObject();
		try {
			requestJson.put("UserId", mUser);
			requestJson.put("Password", password.getText().toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		load.setText(R.string.signin_loading);
		request = new Request(Request.Verify);
		request.setJSONEntity(requestJson);
		request.asyncRequest(this, VERIFY_ID);
		
	}
	
	@Override
	public void onResp(int id, Resp resp, Object...obj) {
		if (resp != null) log(resp.json.toString());
		boolean recovery = true;
		switch (id) {
		case VERIFY_ID:
			if (resp.success) {
				load.setQueuedText(R.string.loading_info);
				recovery = false;
				request = new Request(Request.GetGroupList, true);
				request.asyncRequest(this, GET_GROUP_LIST_ID);
			} else {
				try {
					password.setError(resp.json.getString("ErrInfo"));
					password.requestFocus();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			break;
		case GET_GROUP_LIST_ID:
			if (resp.success) {
				try {
					mGroup = resp.json.getJSONArray("GroupList");
				} catch (JSONException e) {
					e.printStackTrace();
				}
				waitForFinish();
				recovery = false;
			} else {
				UI.toast(getString(R.string.e4));
			}
			break;
		}
		if (recovery) {
			loginBtn.setEnabled(true);
			AnimUtils.FadeOut.startAnimation(loginProgress, 300);
			AnimUtils.FadeIn.startAnimation(loginInput, 500);
		}
			
	}

	@Override
	public void onErr(int id, String err, int httpCode, Object...obj) {
		log(err);
		switch (id) {
		case VERIFY_ID:
		case GET_GROUP_LIST_ID:
			AnimUtils.FadeOut.startAnimation(loginProgress, 300);
			AnimUtils.FadeIn.startAnimation(loginInput, 500);
			loginBtn.setEnabled(true);
			UI.toast(err);
			break;
		}
	}
	
	private Runnable reFinish = new Runnable() {
		public void run() {
			waitForFinish();
		}
	};

	private void waitForFinish() {
		if (!load.isQueuedFinished()) {
			handler.postDelayed(reFinish, 300);
		} else if (loginProgress.getVisibility() == View.VISIBLE) {
			AnimUtils.FadeOut.startAnimation(loginProgress, 300);
			handler.postDelayed(reFinish, 300);
		} else {
			progressBar.setVisibility(View.GONE);
			load.setText("");
			load.setVisibility(View.VISIBLE);
			load.setTextColor(0xff008800);
			load.setQueuedText("登录成功");
			loginProgress.setVisibility(View.VISIBLE);
			// start activity
			MainApp.initSession(mUser, mGroup);
			Intent intent = new Intent(UI.getActivity(), MainActivity.class);
			startActivity(intent);
			finish();
		}
	}

}
