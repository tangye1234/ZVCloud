package com.zigvine.zagriculture;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.slidingmenu.lib.SlidingMenu;
import com.zigvine.android.widget.Pager;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

abstract public class UIActivity<T extends UIActivity<?>> extends android.app.Activity implements OnClickListener {
	
	public UITool<T> UI;
	protected String TAG;
	protected final static String BTAG = "UIActivity";
	
	public static class UITool<S extends UIActivity<?>> {
		
		private S activity;
		private Toast toast;
		private SlidingMenu menu;
		private ListView list;
		
		public UITool(S activity) {
			this.activity = activity;
		}
		
		/**
		 * To fetch child Activity.this, the class will be the child activity itself
		 * @return the instance of the child activity
		 */
		public S getActivity() {
			return activity;
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
		 */
		public void toast(CharSequence err) {
			if (toast != null) {
				toast.cancel();
			}
			toast = Toast.makeText(activity, err, Toast.LENGTH_SHORT);
			toast.show();
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
	}
	
	@SuppressWarnings("unchecked")
	public UIActivity() {
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
	
	/*package*/ void log(String s) {
		Log.d(TAG, s);
	}
	
	public static class MenuListAdapter extends BaseAdapter implements OnItemClickListener {
		
		private UIActivity<?> activity;
		private JSONArray data;

		public MenuListAdapter(UIActivity<?> context, JSONArray json) {
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

}
