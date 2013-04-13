package com.zigvine.zagriculture;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

public class UpdateActivity extends Activity implements OnCancelListener{
	
	private final static String TAG = "UpdateActivity";
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE); 
		SharedPreferences sp = getSharedPreferences(UpdateModel.UPDATE, MODE_PRIVATE);
        String ver = sp.getString("ver", null);
        final String src = sp.getString("src", null);
        String sze = sp.getString("sze", null);
        String ext = sp.getString("ext", null);
        
        Log.v(TAG, "ver = " + ver);
        //Log.v(TAG, "src = " + src);
        //Log.v(TAG, "ext = " + ext);
        
        if(ver == null || sze == null || src == null) {
            finish();
            return;
        }
        
        ver += "\n大小：" + sze;
        
        if(ext != null && ext.length() > 0) {
            ver += "\n" + ext;
        }
        
        AlertDialog d = new AlertDialog.Builder(this)
        .setTitle("更新提示")
        .setIcon(R.drawable.ic_dialog_alert)
        .setMessage("检测到程序更新，需更新后才能继续使用\n\n版本：" + ver)
        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
		         finish();
			}
		})
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int which) {
        		Uri uri = Uri.parse(src);
                Intent i = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(i);
                finish();
        	}
        })
        .setOnCancelListener(this)
        .create();
        d.setCanceledOnTouchOutside(false);
        d.show();
	}
	
	@Override
	public void onCancel(DialogInterface dialog) {
		finish();
	}
    
}
