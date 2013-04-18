package com.zigvine.zagriculture;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.http.entity.mime.content.InputStreamBody;

import com.zigvine.android.http.Request;
import com.zigvine.android.http.Request.Resp;
import com.zigvine.android.utils.Utils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class PostActivity extends UIActivity<PostActivity>
		implements DialogInterface.OnClickListener {
	
	private static final int SEND_ID = 0x10;
	private static final int CAMERA_ID = 0x11;
	
	private static final int PICK_PHOTO = 0;
	private static final int TAKE_PHOTO = 1;
	private static final int DELE_PHOTO = 2;
	private static final String[] STRARR = new String[] {"从照片库中挑选", "拍摄新照片", "舍弃该照片"};
	
	TextView title;
	View titleMain, frame, btn;
	Uri outputFileUri;
	Uri contentUri;
	ImageView img, titleMenu;
	EditText postTitle, postContent;
	
	final String[] title_camera_arr = new String[] {STRARR[PICK_PHOTO], STRARR[TAKE_PHOTO]};
	final String[] image_camera_arr = new String[] {STRARR[PICK_PHOTO], STRARR[TAKE_PHOTO], STRARR[DELE_PHOTO]};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UI.setContentView(R.layout.main_post);
		UI.setBackNavVisibility(View.VISIBLE);
		//UI.setParentBackground(R.drawable.light_bg);
		UI.setMainBackground(R.drawable.light_bg);
		//findViewById(R.id.main_parent).setBackgroundColor(0xffc5c5c5);
		
		UI.addCustomMenuIcon(R.drawable.ic_menu_send, "发送").setId(SEND_ID);
		UI.addCustomMenuIcon(R.drawable.ic_menu_camera, "附上照片").setId(CAMERA_ID);
		
		title = (TextView) findViewById(R.id.title_text);
		title.setText("发表提问");
		
		titleMain = findViewById(R.id.title_main);
		titleMain.setOnClickListener(this);
		
		titleMenu = (ImageView) findViewById(R.id.title_menu);
		titleMenu.setImageResource(R.drawable.ic_menu_compose);
		
		frame = findViewById(R.id.post_frame);
		img = (ImageView) findViewById(R.id.post_image);
		btn = findViewById(R.id.post_image_btn);
		btn.setOnClickListener(this);
		
		postTitle = (EditText) findViewById(R.id.post_title);
		postContent = (EditText) findViewById(R.id.post_content);
	}
	
	@Override
	public void onBackPressed() {
		askToFinish();
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.title_main:
				askToFinish();
				break;
			case R.id.post_image_btn:
				new AlertDialog.Builder(this)
				.setItems(image_camera_arr, this)
				.setNegativeButton(android.R.string.cancel, null)
				.show();
				break;
			case SEND_ID:
				sendPrepare();
				break;
			case CAMERA_ID:
				new AlertDialog.Builder(this)
				.setTitle("插入照片")
				.setIcon(R.drawable.ic_dialog_gallery)
				.setItems(title_camera_arr, this)
				.setNegativeButton(android.R.string.cancel, null)
				.show();
				break;
		}
	}
	
	private void askToFinish() {
		String postT = postTitle.getText().toString();
		String postC = postContent.getText().toString();
		boolean needAsk = postT.length() != 0 || postC.length() != 0;
		if (needAsk) {
			new AlertDialog.Builder(this)
			.setTitle("提示")
			.setIcon(R.drawable.ic_dialog_info)
			.setMessage("您将舍弃已有的编辑内容\n是否决定退出？")
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.show();
		} else {
			finish();
		}
	}
	
	private void sendPrepare() {
		String postT = postTitle.getText().toString();
		String postC = postContent.getText().toString();
		if (postT.length() == 0) {
			postTitle.requestFocus();
			UI.toast("标题不能为空");
			return;
		}
		if (postC.length() == 0) {
			postContent.requestFocus();
			UI.toast("内容不能为空");
			return;
		}
		InputStreamBody upload = null;
		if (frame.getVisibility() == View.VISIBLE) {
			Bitmap bitmap = null;
			try {
				if (contentUri != null) {
					bitmap = resizeBitmap(getContentResolver(), contentUri, 800, 600);
				} else if (outputFileUri != null) {
					bitmap = resizeBitmap(outputFileUri, 800, 600);
				}
			} catch (FileNotFoundException e) {}
			if (bitmap == null) {
				UI.toast("图片已经不存在，请重新选择");
				return;
			}
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			bitmap.compress(CompressFormat.JPEG, 90, stream);
			InputStream is = new ByteArrayInputStream(stream.toByteArray());
			upload = new InputStreamBody(is, "upload.jpg");
		}
		sendNow(postT, postC, upload);
	}
	
	private void sendNow(final String postT, final String postC, final InputStreamBody upload) {
		UI.hideInputMethod();
		findViewById(SEND_ID).setEnabled(false);
		final Request request = new Request(Request.SUBMITCONSU);
		request.setParam("subject", postT);
		request.setParam("content", postC);
		request.setParam("parent_id", "0"); // TODO to indicate this is a new subject a just a sub response to a parent subject
		request.setSoTimeout(30000);
		if (upload != null) {
			request.setMartipartStream("plant_photo", upload);
		}
		
		final ProgressDialog pd = new ProgressDialog(this);
    	pd.setMessage("正在发表提问");
    	pd.setCanceledOnTouchOutside(false);
    	pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				request.shutdown();
				findViewById(SEND_ID).setEnabled(true);
			}
		});
    	pd.show();
		
		request.asyncRequest(new Request.ResponseListener() {
			@Override
			public void onResp(int id, Resp resp, Object... obj) {
				if (resp != null) {
					if (resp.success) {
						UI.toast("发帖成功");
						finish();
					} else {
						UI.toast("上传失败");
					}
					pd.dismiss();
				}
				findViewById(SEND_ID).setEnabled(true);
			}
			
			@Override
			public void onErr(int id, String err, int httpCode, Object... obj) {
				pd.dismiss();
				UI.toast(err);
				findViewById(SEND_ID).setEnabled(true);
			}
		}, 0);
	}
	
	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.static_anim, R.anim.slide_out_to_right);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		Intent intent = null;
		switch(which) {
		case PICK_PHOTO:
			intent = new Intent();  
            intent.setType("image/*");  
            intent.setAction(Intent.ACTION_GET_CONTENT);   
            /* 取得相片后返回本画面 */  

            startActivityForResult(intent, PICK_PHOTO);
            break;
		case TAKE_PHOTO:
			intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			File out = new File(Environment.getExternalStorageDirectory(), "_camera.jpg");
			outputFileUri = Uri.fromFile(out);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
			startActivityForResult(intent, TAKE_PHOTO);
			break;
		case DELE_PHOTO:
			contentUri = null;
			outputFileUri = null;
			img.setImageResource(0);
            frame.setVisibility(View.GONE);
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICK_PHOTO) {
			if (resultCode == RESULT_OK) {
				contentUri = data.getData();
				outputFileUri = null;
				ContentResolver cr = getContentResolver();
				try {
					int sizeW = Utils.dp2px(this, 160) - frame.getPaddingLeft() - frame.getPaddingRight();
					int sizeH = Utils.dp2px(this, 120) - frame.getPaddingTop() - frame.getPaddingBottom();
	                Bitmap bitmap = resizeBitmap(cr, contentUri, sizeW, sizeH);
	                img.setImageBitmap(bitmap);
	                frame.setVisibility(View.VISIBLE);
	                
	            } catch (FileNotFoundException e) {  
	                Log.e("Exception", e.getMessage(),e);  
	            }


			}
		} else if (requestCode == TAKE_PHOTO) {
			if (resultCode == RESULT_OK) {
				if (outputFileUri != null) {
					contentUri = null;
					int sizeW = Utils.dp2px(this, 160) - frame.getPaddingLeft() - frame.getPaddingRight();
					int sizeH = Utils.dp2px(this, 120) - frame.getPaddingTop() - frame.getPaddingBottom();
	                Bitmap bitmap = resizeBitmap(outputFileUri, sizeW, sizeH);
	                new File(outputFileUri.getPath()).deleteOnExit();
	                img.setImageBitmap(bitmap);
	                frame.setVisibility(View.VISIBLE);
				}
			}
		}
	}
	
	
	private static float getRateH2W(int w, int h) {
		return (float) h / (float) w;
	}
	
	private static Bitmap resizeBitmap(ContentResolver cr, Uri uri, int vWidth, int vHeight)
			throws FileNotFoundException {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		Rect outPadding = new Rect();
		BitmapFactory.decodeStream(cr.openInputStream(uri), outPadding, options);
		float rateOrigin = getRateH2W(vWidth, vHeight);
		float newOrigin = getRateH2W(options.outWidth, options.outHeight);
		
		Boolean scaleByHeight = rateOrigin > newOrigin;
		if (options.outHeight > vHeight || options.outWidth > vWidth) {
			// Load, scaling to smallest power of 2 that'll get it <= desired
			double sampleSize = scaleByHeight ? options.outHeight
					/ vHeight : options.outWidth / vWidth;
			options.inSampleSize = (int) Math.pow(2d,
					Math.floor(Math.log(sampleSize) / Math.log(2d)));
			//Log.i(TAG, "photo sample size: " + options.inSampleSize);
		}
		options.inJustDecodeBounds = false;
		Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri), outPadding, options);
		return bitmap;
	}
	
	private static Bitmap resizeBitmap(Uri fileUri, int vWidth, int vHeight) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(fileUri.getPath(), options);
		float rateOrigin = getRateH2W(vWidth, vHeight);
		float newOrigin = getRateH2W(options.outWidth, options.outHeight);
		
		Boolean scaleByHeight = rateOrigin > newOrigin;
		if (options.outHeight > vHeight || options.outWidth > vWidth) {
			// Load, scaling to smallest power of 2 that'll get it <= desired
			double sampleSize = scaleByHeight ? options.outHeight
					/ vHeight : options.outWidth / vWidth;
			options.inSampleSize = (int) Math.pow(2d,
					Math.floor(Math.log(sampleSize) / Math.log(2d)));
			//Log.i(TAG, "photo sample size: " + options.inSampleSize);
		}
		options.inJustDecodeBounds = false;
		Bitmap bitmap = BitmapFactory.decodeFile(fileUri.getPath(), options);
		return bitmap;
	}

}
