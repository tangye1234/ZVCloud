package com.zigvine.android.widget;

import java.text.DecimalFormat;
import java.util.Calendar;

import com.zigvine.android.utils.Utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class GraphView extends View {
	
	private static final String TAG = "GroupView";
	
	protected Context mContext;
	protected int width, height, dataH, wordH, dp;
	private int[] x;
	private float[] y;
	private float YMIN, YMAX, YLEN;
	private int XMIN, XMAX, XLEN;
	private int length;
	private long xOffset;
	private Handler handler;
	
	private Paint mPaint, linePaint, axisPaint;

	public GraphView(Context context) {
		super(context);
		init(context);
	}
	
	public GraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	public GraphView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	private void init(Context context) {
		mContext = context;
		wordH = Utils.dp2px(context, 40);
		dp = Utils.dp2px(context, 2);
		handler = new Handler();
		Typeface tf = Typeface.createFromAsset(getContext().getAssets(),"fonts/eurostileRegular.ttf");
		
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setColor(0xff26c3f7);
		mPaint.setStrokeWidth(1);
		mPaint.setTypeface(tf);
		//mPaint.setFakeBoldText(true);
		mPaint.setTextSize(Utils.dp2px(context, 14));
		
		linePaint = new Paint();
		linePaint.setAntiAlias(true);
		linePaint.setStrokeWidth(2);
		linePaint.setColor(0xbb1699cf);
		
		axisPaint = new Paint();
		axisPaint.setAntiAlias(true);
		axisPaint.setStrokeWidth(1);
		axisPaint.setColor(0x9926c3f7);
	}
	
	/**
	 * 
	 * @param datax
	 * @param datay
	 */
	public void setDataXY(int[] datax, float[] datay, int len) {
		length = len;
		x = datax.clone();
		y = datay.clone();
		// TODO invalid();
		/*float ymax = MathFloat.max(y);
		float ymin = MathFloat.min(y);
		setAxisY(ymin, ymax);
		*/
		//postInvalidate();
	}
	
	public void setXStart(long start) {
		xOffset = start;
	}
	
	public void setAxisX(int xmin, int xmax) {
		XMIN = xmin;
		XMAX = xmax;
		XLEN = XMAX - XMIN;
	}
	
	Runnable yanim = new Runnable() {
		public void run() {
			Log.i(TAG, "animation run YMIN=" + YMIN + " YMAX=" + YMAX);
			float min = _ymin - YMIN;
			float delta = min * 0.16f;
			float rate = delta / _ylen * 1000;
			boolean ytar = false;
			if (Math.abs(rate) < 1) {
				YMIN = _ymin;
				ytar = true;
			} else {
				YMIN += delta;
			}
			
			float max = _ymax - YMAX;
			delta = max * 0.16f;
			rate = delta / _ylen * 1000;
			if (Math.abs(rate) < 1) {
				YMAX = _ymax;
				if (ytar) {
					YLEN = YMAX - YMIN;
					invalidate();
					return;
				}
			} else {
				YMAX += delta;
			}
			YLEN = YMAX - YMIN;
			postInvalidate();
			handler.postDelayed(this, 15);
		}
	};
	
	float _ymin, _ymax, _ylen;
	
	public void setAxisY(float ymin, float ymax) {
		handler.removeCallbacks(yanim);
		YMIN = ymin;
		YMAX = ymax;
		YLEN = YMAX - YMIN;
	}
	
	public  void setAxisYAnimation(float oldmin, float oldmax, float ymin, float ymax) {
		setAxisY(oldmin, oldmax);
		_ymin = ymin;
		_ymax = ymax;
		_ylen = _ymax - _ymin;
		handler.removeCallbacks(yanim);
		handler.postDelayed(yanim, 15);
	}

	@Override
    protected void onDraw(Canvas canvas) {
		drawAxis(canvas);
		drawData(canvas);
    }
	
	private void setWH(int w, int h) {
		width = w + 1;
        height = h;
        dataH = h - wordH;
	}
	
	@Override  
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {  
        super.onSizeChanged(w, h, oldw, oldh);  
        setWH(w, h);
    }
	
	private void drawAxis(Canvas c) {
		c.save();
		int start = XMIN;
		boolean longX = true;
		Calendar calendar = Calendar.getInstance();
		for (int i = 0; i < 13; i++) {
			float starty, stopy;
			if (longX) {
				starty = 0;
				stopy = dataH + dp * 2;
			} else {
				starty = dataH;
				stopy = dataH + dp;
			}
			int time = start + i * 30 * 60;
			float startX = convertX(time);
			c.drawLine(startX, starty, startX, stopy, axisPaint);
			if (longX) {
				calendar.setTimeInMillis((xOffset + time) * 1000);
				int q = calendar.get(Calendar.HOUR_OF_DAY);
				String text = q + ":00";
				float txtWidth = mPaint.measureText(text);
				c.drawText(text, startX - txtWidth / 2, dataH + wordH / 2, mPaint);
			}
			longX = !longX;
		}
		calendar.setTimeInMillis((xOffset + XMAX) * 1000);
		int h = calendar.get(Calendar.HOUR_OF_DAY);
		if (h > 0 && h < 6) {
			float startX = convertX(start + (6 - h) * 60 * 60);
			c.drawLine(startX, dataH + wordH / 2 + dp * 2, startX, dataH + wordH, axisPaint);
			if (h < 3) {
				calendar.add(Calendar.HOUR, -h - 1);
				String text = Utils.DATE.format(calendar.getTime());
				float txtWidth = mPaint.measureText(text);
				c.drawText(text, startX - txtWidth - 20, dataH + wordH - 5, mPaint);
			} else {
				String text = Utils.DATE.format(calendar.getTime());
				//float txtWidth = mPaint.measureText(text);
				c.drawText(text, startX + 20, dataH + wordH - 5, mPaint);
			}
		} else {
			if (h == 0) {
				calendar.add(Calendar.HOUR, -1);
			}
			String text = Utils.DATE.format(calendar.getTime());
			float txtWidth = mPaint.measureText(text);
			c.drawText(text, (width - txtWidth) / 2, dataH + wordH - 5, mPaint);
		}
		
		// draw y
		float starty = dataH / 3;
		c.drawLine(0, starty, width, starty, axisPaint);
		
		starty = starty * 2;
		c.drawLine(0, starty, width, starty, axisPaint);
		
		c.restore();
	}
	
	private void drawData(Canvas c) {
		c.save();
		Rect rect = new Rect(0, 0, width, dataH - 1);  
		c.clipRect(rect);
		for (int i = 1; i < length; i++) {
			if (x[i] < XMIN || x[i - 1] > XMAX) continue;
			float startX = convertX(x[i - 1]);
			float startY = convertY(y[i - 1]);
			float stopX = convertX(x[i]);
			float stopY = convertY(y[i]);
			c.drawLine(startX, startY, stopX, stopY, linePaint);
		}
		
		// word on the data
		float starty = dataH / 3;
		String ystr1 = new DecimalFormat("#0.00").format(getYValue(starty));
		
		starty = starty * 2;
		String ystr2 = new DecimalFormat("#0.00").format(getYValue(starty));

		if (callback != null) {
			callback.onText(new String[] {ystr1, ystr2});
		}
		
		c.restore();
	}
	
	public static interface OnAxisYTextCallback {
		public void onText(String[] text);
	}
	
	private OnAxisYTextCallback callback = null;
	
	public void setOnAxisYTextCallback(OnAxisYTextCallback Callback) {
		callback = Callback;
	}
	
	private float convertX(int x) {
		return (x - XMIN) / (float) XLEN * (width - 1);
	}
	
	private float convertY(float y) {
		return (YMAX - y) / YLEN * (dataH - 1);
	}
	
	/*private int getXValue(float x) {
		return Math.round(x / (float) (width - 1) * XLEN + XMIN);
	}*/
	
	private float getYValue(float y) {
		return YMAX - y / (float) (dataH - 1) * YLEN;
	}

}
