package com.zigvine.zagriculture;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.slidingmenu.lib.SlidingMenu;
import com.zigvine.android.http.Request;
import com.zigvine.android.widget.Pager;
import com.zigvine.zagriculture.MainApp.UpdateCheckListener;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

abstract public class UIPreferenceActivity<T extends UIPreferenceActivity<?>> extends PreferenceActivity implements OnClickListener {
	
	public UITool<T> UI;
	protected String TAG;
	protected final static String BTAG = "UIPreferenceActivity";
	private View mOverflowMenuButton;
	
	@Override
	protected void onResume() {
		super.onResume();
		if (UI.needSignIn && !MainApp.isSignIn()) {
			super.finish();
			return;
		}
		if (MainApp.needUpgrade()) {
			MainApp.quitSession();
			super.finish();
		}
		UI.startOnlineService(false);
	}

	@Override
	protected void onUserLeaveHint() {
		super.onUserLeaveHint();
		UI.startOnlineService(true);
	}
	
	public static class UITool<S extends UIPreferenceActivity<?>> {
		
		private S activity;
		private Toast toast;
		private SlidingMenu menu;
		private ViewGroup mContentView;
		private ListView list;
		private boolean useCustomContentView;
		private boolean needSignIn;
		
		public UITool(S activity) {
			this.activity = activity;
			useCustomContentView = false;
			needSignIn = false;
		}
		
		/**
		 * To fetch child Activity.this, the class will be the child activity itself
		 * @return the instance of the child activity
		 */
		public S getActivity() {
			return activity;
		}
		
		/**
		 * should call very first of all initialization
		 */
		public void requestSignIn() {
			needSignIn = true;
		}
		
		/**
		 * To hide the input method from the current focused activity
		 */
		public void hideInputMethod() {
			InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
			inputManager.hideSoftInputFromWindow(activity.getCurrentFocus()
					.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		}
		
		/**
		 * To toast a prompt for short indications
		 * if param is null, then just cancel the current toast which is showing
		 */
		public void toast(final CharSequence err) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (toast != null) {
						toast.cancel();
					}
					if (err != null) {
						toast = Toast.makeText(activity, err, Toast.LENGTH_SHORT);
						toast.show();
					}
				}
			});
			
		}
		
		/**
		 * get the shared preference
		 * @param prefix
		 * @param user
		 * @return shared preference object
		 */
		public SharedPreferences getSharedPrefsForUsers(String prefix, String user) {
			SharedPreferences sp = activity.getSharedPreferences(prefix + "_" + user, MODE_PRIVATE);
			return sp;
		}
		
		/**
		 * setup the top-right options menu button
		 */
		public void setupMoreMenu(View v) {
			if (MainApp.getAPILevel() >= 11) {
				if (MainApp.getAPILevel() >= 14) {
					if (!activity.hasPermanentMenuKey()) {
						activity.createFakeMenu(v);
			        }
				} else {
					activity.createFakeMenu(v);
				}
	        }
		}
		
		public void setContentView(int layoutResID) {
			useCustomContentView = true;
			View v = View.inflate(activity, R.layout.activity_main, null);
			mContentView = (ViewGroup) v.findViewById(R.id.main_content);
			View.inflate(activity, layoutResID, mContentView);
			activity.setContentView(v);
			if (activity.findViewById(android.R.id.list) == null) {
				throw new IllegalStateException("Preference list dose not exist");
			}
		}
		
		/**
		 * setup the footer view
		 */
		public void setupFooterView() {
			checkCustomContentView();
			activity.findViewById(R.id.custom_footer).setVisibility(View.VISIBLE);
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mContentView.getLayoutParams();
			int b = (int) activity.getResources().getDimension(R.dimen.footer_height);
			lp.bottomMargin = b;
			mContentView.setLayoutParams(lp);
			
		}
		
		/**
		 * set the main content view's background resource
		 * @param resid the resource id of the background
		 */
		public void setMainBackground(int resid) {
			checkCustomContentView();
			mContentView.setBackgroundResource(resid);
		}
		
		/**
		 * set the main content view's background resource
		 * @param resid the resource id of the background
		 */
		public void setParentBackground(int resid) {
			checkCustomContentView();
			activity.findViewById(R.id.main_parent).setBackgroundResource(resid);
		}
		
		/**
		 * add a custom button into the title bar
		 * @param resid
		 * @param text
		 * @return the added button view
		 */
		public View addCustomMenuIcon(int resid, String text) {
			checkCustomContentView();
			ImageView im = new ImageView(activity);
			im.setContentDescription(text);
			im.setScaleType(ScaleType.CENTER_INSIDE);
			im.setImageResource(resid);
			im.setBackgroundResource(R.drawable.title_btn);
			im.setOnClickListener(activity);
			int dp = (int) activity.getResources().getDimension(R.dimen.title_height);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp, dp);
			ViewGroup customTitle = (ViewGroup) activity.findViewById(R.id.custom_title);
			customTitle.addView(im, 2, lp);
			return im;
		}
		
		/**
		 * change the visibility of the back navigation indicator
		 * @param visibility
		 */
		public void setBackNavVisibility(int visibility) {
			checkCustomContentView();
			activity.findViewById(R.id.title_back).setVisibility(visibility);
		}
		
		//public void setMainBackground()
		
		/**
		 * start a url site
		 * @param url the link of the website
		 */
		public void startWebSite(String url) {
	    	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
	    	activity.startActivity(intent);
	    }
		
		/**
		 * control the forground detection policy
		 * @param foreground is this activity hidden
		 */
		public void startOnlineService(boolean foreground) {
			Intent intent = new Intent(activity, OnlineService.class);
			intent.putExtra(OnlineService.FOREGROUND_EXTRA, foreground);
			activity.startService(intent);
		}
		
		/**
		 * Make a sliding menu in the activity
		 * @param resid the menu layout resouce id, if use 0, then view content is pre-set
		 * @return the menu view object
		 */
		public SlidingMenu createSlidingMenu(int resid) {
			SlidingMenu menu = new SlidingMenu(activity);
	        menu.setMode(SlidingMenu.LEFT);
	        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
	        menu.setShadowWidthRes(R.dimen.sliding_menu_shadow_width);
	        menu.setShadowDrawable(R.drawable.sliding_shadow);
	        menu.setBehindOffsetRes(R.dimen.sliding_menu_offset);
	        menu.setFadeDegree(0.35f);
	        menu.attachToActivity(activity, SlidingMenu.SLIDING_CONTENT);
	        if (resid > 0) {
	        	menu.setMenu(resid);
	        }
	        return menu;
		}
		
		/**
		 * Make a standard menu for a session-in-charged activity
		 */
		public void createStandardSlidingMenu() {
			menu = createSlidingMenu(R.layout.sliding_menu);
			TextView user = (TextView) menu.findViewById(R.id.menu_user);
			TextView signoff = (TextView) menu.findViewById(R.id.menu_signoff);
			list = (ListView) menu.findViewById(R.id.green_house_list);
			if (MainApp.isSignIn()) {
				user.setText(MainApp.getUser());
				signoff.setOnClickListener(activity);
				final MenuListAdapter adapter = new MenuListAdapter(activity, MainApp.getGroup());
				list.setDivider(null);
				list.setDividerHeight(0);
				list.setAdapter(adapter);
				list.setScrollbarFadingEnabled(false);
				list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
				list.setOnItemClickListener(adapter);
				list.post(new Runnable() {
					public void run() {
						activity.onStandardMenuSelected(adapter.getItem(0), 0, adapter.getItemId(0));
					}
				});
			} else {
				toast(activity.getString(R.string.already_signoff));
				activity.finish();
			}
			
		}
		
		/**
		 * set the standard SlidingMenu open-close listener
		 * @param l the listener callback
		 */
		public void setStandardSlidingMenuListener(final MenuListener l) {
			if (menu != null) {
				if (l != null) {
					menu.setOnOpenedListener(new SlidingMenu.OnOpenedListener() {
						@Override
						public void onOpened() {
							l.onOpened();
						}
					});
					menu.setOnClosedListener(new SlidingMenu.OnClosedListener() {
						@Override
						public void onClosed() {
							l.onClosed();
						}
					});
				} else {
					menu.setOnOpenedListener(null);
					menu.setOnClosedListener(null);
				}
			}
		}
		
		/**
		 * update the standard menu
		 */
		public void updateStandardSlidingMenu() {
			MenuListAdapter adapter = (MenuListAdapter) list.getAdapter();
			if (adapter != null) {
				adapter.notifyDataSetChanged();
			}
		}
		
		/**
		 * Toggle the standard sliding menu open or close
		 */
		public void toggleStandardMenu() {
			if (menu != null) {
				menu.toggle();
			}
		}
		
		private void checkCustomContentView() {
			if (!useCustomContentView) {
				throw new IllegalStateException("the custom view must be under UI.setContentView");
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public UIPreferenceActivity() {
		super();
		UI = new UITool<T>((T)this);
		TAG = getClass().getSimpleName();
	}
	
	@Override
	public void onClick(View view) {
		Log.w(BTAG, "onClick not implemented at all in " + TAG);
	}
	
	protected void onStandardMenuSelected(Object obj, int pos, long id) {
		try {
			JSONObject json = (JSONObject) obj;
			if (json != null) {
				UI.list.setItemChecked(pos, true);
				UI.menu.showContent();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void onRefresh(Pager page) {
		log("no page refresh needed");
	}
	
	@TargetApi(14)
	private boolean hasPermanentMenuKey() {
		return ViewConfiguration.get(this).hasPermanentMenuKey();
	}
	
	private void createFakeMenu(View v) {
		if (v == null) {
			mOverflowMenuButton = findViewById(R.id.more_menu);
		} else {
			mOverflowMenuButton = v;
		}
        if (mOverflowMenuButton != null) {
            mOverflowMenuButton.setVisibility(View.VISIBLE);
            mOverflowMenuButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showPopupMenu();
				}
			});
        }
    }

	@TargetApi(11)
	private void showPopupMenu() {
        final PopupMenu popupMenu = new PopupMenu(this, mOverflowMenuButton);
        final Menu menu = popupMenu.getMenu();
        // popupMenu.inflate(R.menu.main_menu);
        popupMenu.getMenuInflater().inflate(R.menu.main_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				return onOptionsItemSelected(item);
			}
        });
        onPrepareOptionsMenu(menu);
        if(popupMenu != null) {
        	popupMenu.show();
        }
    }
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_logoff).setVisible(MainApp.isSignIn());
        menu.findItem(R.id.menu_settings).setVisible(false);
        return true;
    }
    
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
    	ActivityInfo info = intent.resolveActivityInfo(getPackageManager(), 0);
    	if (info != null && info.packageName != null && info.packageName.equals(getPackageName())) {
    		intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
    	}
    	super.startActivityForResult(intent, requestCode);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logoff:
            	MainApp.quitSession();
    			super.finish(); // do not use override finish
                return true;
            case R.id.menu_about:
                showAboutDialog();
                return true;
            case R.id.menu_license:
            	UI.startWebSite(Request.AgreementPage);
            	return true;
            case R.id.menu_update:
            	manualUpdateCheck();
            	return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void manualUpdateCheck() {
    	final ProgressDialog pd = new ProgressDialog(this);
    	pd.setMessage("正在检查更新");
    	pd.setCanceledOnTouchOutside(false);
    	pd.show();
    	MainApp.getInstance().startCheck(new UpdateCheckListener() {
			@Override
			public void onCheckedOver(int result) {
				pd.dismiss();
				if (result == MainApp.CHECKED_NO_NEED) {
					UI.toast("您已经使用的是最新版本");
				} else if (result == MainApp.UNCHECKED) {
					UI.toast("更新检测失败，请检查网络");
				}
			}
    	}, true);
    }
    
    private void showAboutDialog() {
    	new AlertDialog.Builder(this)
    	.setTitle("关于" + getString(R.string.app_name))
        .setIcon(R.drawable.ic_launcher)
        .setMessage("版本: " + MainApp.getVersion())
        .setPositiveButton("检查更新", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				manualUpdateCheck();
			}
		})
		.setNegativeButton(android.R.string.cancel, null)
        .show();
    }
	
	/*package*/ void log(String s) {
		Log.d(TAG, s);
	}
	
	public static class MenuListAdapter extends BaseAdapter implements OnItemClickListener {
		
		private UIPreferenceActivity<?> activity;
		private JSONArray data;

		public MenuListAdapter(UIPreferenceActivity<?> context, JSONArray json) {
			activity = context;
			data = json;
		}
		
		@Override
		public int getCount() {
			return data.length();
		}

		@Override
		public Object getItem(int position) {
			try {
				return data.getJSONObject(position);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public long getItemId(int position) {
			try {
				return data.getJSONObject(position).getLong("GroupId");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = View.inflate(activity, R.layout.menu_list_item, null);
			}
			final JSONObject obj = (JSONObject) getItem(position);
			Map<Long, Integer> alarmGroup = MainApp.getAlarmGroup();
			String name = "";
			String desc = "";
			Long id = null;
			Integer count = null;
			try {
				name = obj.getString("GroupName");
				desc = obj.getString("GroupDesc");
				id = obj.getLong("GroupId");
				if (alarmGroup != null) {
					count = alarmGroup.get(id);
					if (count == null) {
			        	count = 0;
			        }
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
	        TextView txtName = (TextView) convertView.findViewById(R.id.group_name);
	        TextView txtDesc = (TextView) convertView.findViewById(R.id.group_desc);
	        TextView txtCout = (TextView) convertView.findViewById(R.id.group_alarm_count);
	        txtName.setText(name);
	        txtDesc.setText(desc);
	        if (count > 0) {
	        	if (count > 99) {
	        		txtCout.setText("99+");
	        	} else {
	        		txtCout.setText("" + count);
	        	}
	        	txtCout.setVisibility(View.VISIBLE);
	        } else {
	        	txtCout.setText("0");
	        	txtCout.setVisibility(View.GONE);
	        }
	        return convertView;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int pos,
				long id) {
			activity.onStandardMenuSelected(getItem(pos), pos, id);
		}
		
	}
	
	public static interface MenuListener {
		public void onOpened();
		public void onClosed();
	}

}
