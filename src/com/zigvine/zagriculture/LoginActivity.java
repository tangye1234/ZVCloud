package com.zigvine.zagriculture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zigvine.android.anim.AnimUtils;
import com.zigvine.android.anim.QueuedTextView;
import com.zigvine.android.http.Request;
import com.zigvine.android.http.Request.Resp;
import com.zigvine.android.http.Request.ResponseListener;
import com.zigvine.android.utils.JSONObjectExt;
import com.zigvine.android.utils.MD5;
import com.zigvine.android.utils.Utils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class LoginActivity extends UIActivity<LoginActivity>
		implements ResponseListener {

	View logo, loginPan, loginBtn, loginInput, loginProgress, toolbar, moreMenu;
	ProgressBar progressBar;
	EditText user, password;
	CheckBox autoLogin;
	QueuedTextView load;
	Handler handler;
	Request request;
	String mNoticeTitle;
	String mNoticeContent;
	
	String mUser;
	JSONArray mGroup;
	
	private static final int RECOVERY = -1;
	private static final int VERIFY_ID = 0;
	private static final int GET_GROUP_LIST_ID = 1;
	
	private static final String CERTIFICATE = "certificate";
	private static final String LOGININFO = "logininfo";
	private static final String KEY_CERT = "Cert";
	private static final boolean NORMAL_LOGIN = false;
	
	private static final boolean isPreAccountSet = false;//BuildConfig.DEBUG;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(MainApp.needUpgrade()){
			Intent intent = new Intent(this, UpdateActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            super.startActivity(intent);
            super.finish();
            return;
		}
		
		if (MainApp.isSignIn()) {
			Intent intent = new Intent(this, MainActivity.class);
			Bundle b = getIntent().getExtras();
			if (b != null && b.size() > 0) {
				intent.putExtras(b);
			}
			startActivity(intent);
			finish();
			return;
		}
		
		mNoticeTitle = "";
		mNoticeContent = "";
		
		setContentView(R.layout.activity_login);
		
		user = (EditText) findViewById(R.id.login_user);
		password = (EditText) findViewById(R.id.login_password);
		autoLogin = (CheckBox) findViewById(R.id.auto_login);
		moreMenu = findViewById(R.id.login_more_menu);
		
		UI.setupMoreMenu(moreMenu);
		
		readLoginInfo();
		
		if (isPreAccountSet) {
			user.setText("zigvine");
			password.setText("zigvine123");
		}
		
		logo = findViewById(R.id.logo);
		loginPan = findViewById(R.id.login_pan);
		loginBtn = findViewById(R.id.btn_signin);
		loginInput = findViewById(R.id.login_input);
		loginProgress = findViewById(R.id.login_progress);
		load = (QueuedTextView) findViewById(R.id.loading_text);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		toolbar = findViewById(R.id.login_toolbar);
		toolbar.setVisibility(View.INVISIBLE);
		
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
					public void onAnimationEnd() {
						AnimUtils.SlideInUp.startAnimation(toolbar, 500);
					}
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
			prepareLoginInfo();
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
	
	private void prepareLoginInfo() {
		mUser = user.getText().toString();
		String pswd = password.getText().toString();
		if (mUser.length() == 0) {
			user.setError(getString(R.string.e1));
			return;
		}
		if (pswd.length() == 0) {
			password.setError(getString(R.string.e2));
			return;
		}
		SharedPreferences sp = UI.getSharedPrefsForUsers(CERTIFICATE, mUser);
		String cert = sp.getString(KEY_CERT, null);
		final String mPass = pswd;
		if (cert != null) {
			startSign(mUser, mPass, cert);
		} else {
			final View v = View.inflate(this, R.layout.input_dialog_view, null);
			final EditText phoneText = (EditText) v.findViewById(R.id.dialog_input);
			phoneText.setHint(R.string.hint_mobile);
			TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
			if (tm != null) {
				String p = tm.getLine1Number();
				if (p != null) {
					phoneText.setText(p);
				}
			}
			UI.hideInputMethod();
			new AlertDialog.Builder(this)
			.setTitle("首次登录需要绑定手机")
			.setIcon(R.drawable.ic_dialog_info)
			.setView(v)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String phone = phoneText.getText().toString();
					if (phone.length() == 0 || phone.length() < 11) {
						// TODO
					} else {
						// phone = phone.substring(phone.length() - 11);
						startBind(mUser, mPass, phone);
					}
				}
			}).show();
		}
	}
	
	private void startBind(final String user, final String password, final String mobile) {
		log("__startBind__");
		String time = Utils.DATETIME.format(new java.util.Date());
		
		loginBtn.setEnabled(false);
		UI.hideInputMethod();
		AnimUtils.FadeOut.startAnimation(loginInput, 500);
		AnimUtils.FadeIn.startAnimation(loginProgress, 500);
		
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		String did = null;
		if (tm != null) {
			did = tm.getDeviceId();
		}
		if (did == null) {
			did = tm.getSubscriberId();
		}
		if (did == null) {
			did = "xxxx";
		}
		//UI.toast(did);
		
		JSONObject requestJson = new JSONObject();
		try {
			requestJson.put("UserId", user);
			requestJson.put("Password", NORMAL_LOGIN ? password : MD5.getMD5ofStr(user + password + time));
			requestJson.put("Mobile", mobile);
			requestJson.put("IMEI", did);
			requestJson.put("Timestamp", time);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		load.setText(R.string.signin_loading);
		request = new Request(NORMAL_LOGIN ? "/verify" : Request.MobileBound);
		request.setDebug(false);
		request.setJSONEntity(requestJson);
		request.asyncRequest(this, VERIFY_ID);
		
	}

	private void startSign(final String user, final String password, final String cert) {
		log("__startSign__");
		String time = Utils.DATETIME.format(new java.util.Date());
		
		loginBtn.setEnabled(false);
		UI.hideInputMethod();
		AnimUtils.FadeOut.startAnimation(loginInput, 500);
		AnimUtils.FadeIn.startAnimation(loginProgress, 500);
		
		JSONObject requestJson = new JSONObject();
		try {
			requestJson.put("UserId", user);
			requestJson.put("Password", MD5.getMD5ofStr(user + password + time));
			requestJson.put("Certificate", MD5.getMD5ofStr(cert + password + time));
			requestJson.put("Timestamp", time);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		load.setText(R.string.signin_loading);
		request = new Request(Request.SafeVerify);
		request.setJSONEntity(requestJson);
		request.asyncRequest(this, VERIFY_ID);
		
	}
	
	@Override
	public void onResp(int id, Resp resp, Object...obj) {
		// if (resp != null) log(resp.json.toString());
		boolean recovery = true;
		switch (id) {
		case VERIFY_ID:
			if (resp.success) {
				load.setQueuedText(R.string.loading_info);
				recovery = false;
				process(resp);
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
	
	private void process(Resp resp) {
		if (resp != null) {
			JSONObjectExt json = new JSONObjectExt(resp.json);
			mNoticeTitle = json.getString("NoticeTitle", "");
			mNoticeContent = json.getString("NoticeContent", "");
			String cert = json.getString("Certificate", null);
			if (cert != null) {
				SharedPreferences sp = UI.getSharedPrefsForUsers(CERTIFICATE, mUser);
				sp.edit().putString(KEY_CERT, cert).commit();
			}
		}
	}

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
			// save login info depends on user decision
			if (autoLogin.isChecked()) {
				saveLoginInfo();
			} else {
				deleteLoginInfo();
			}
			// start activity
			MainApp.initSession(mUser, mGroup);
			Intent intent = new Intent(UI.getActivity(), MainActivity.class);
			intent.putExtra(MainActivity.NOTICE_EXTRA, new String[] {mNoticeTitle, mNoticeContent});
			startActivity(intent);
			finish();
		}
	}
	
	private void saveLoginInfo() {
		SharedPreferences sp = UI.getSharedPrefsForUsers(LOGININFO, "currentUser");
		sp.edit().putString("user", user.getText().toString())
		.putString("pass", password.getText().toString())
		.commit();
	}
	
	private void deleteLoginInfo() {
		SharedPreferences sp = UI.getSharedPrefsForUsers(LOGININFO, "currentUser");
		sp.edit().clear().commit();
	}
	
	private void readLoginInfo() {
		SharedPreferences sp = UI.getSharedPrefsForUsers(LOGININFO, "currentUser");
		String u = sp.getString("user", "");
		if (u.length() > 0) {
			user.setText(u);
			password.setText(sp.getString("pass", ""));
			autoLogin.setChecked(true);
		} else {
			autoLogin.setChecked(false);
		}
	}

}
