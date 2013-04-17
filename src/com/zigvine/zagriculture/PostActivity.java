package com.zigvine.zagriculture;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class PostActivity extends UIActivity<PostActivity> {
	
	TextView title;
	View titleMain;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UI.setContentView(R.layout.main_post);
		//UI.setParentBackground(R.drawable.light_bg);
		UI.setMainBackground(R.drawable.light_bg);
		//findViewById(R.id.main_parent).setBackgroundColor(0xffc5c5c5);
		
		UI.addCustomMenuIcon(android.R.drawable.ic_menu_send, "发送");
		UI.addCustomMenuIcon(android.R.drawable.ic_menu_camera, "附上照片");
		
		title = (TextView) findViewById(R.id.title_text);
		title.setText("发表提问");
		
		titleMain = findViewById(R.id.title_main);
		titleMain.setOnClickListener(this);
		
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.title_main:
				finish();
				break;
		}
	}
	
	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.static_anim, R.anim.slide_out_to_right);
	}

}
