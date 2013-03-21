/*
 * Copyright (C) 2011
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.taptwo.android.widget;

import com.zigvine.zagriculture.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;

/**
 * A FlowIndicator which draws circles (one for each view). <br/>
 * Availables attributes are:<br/>
 * <ul>
 * activeColor: Define the color used to draw the active circle (default to
 * white)
 * </ul>
 * <ul>
 * inactiveColor: Define the color used to draw the inactive circles (default to
 * 0x44FFFFFF)
 * </ul>
 * <ul>
 * inactiveType: Define how to draw the inactive circles, either stroke or fill
 * (default to stroke)
 * </ul>
 * <ul>
 * activeType: Define how to draw the active circle, either stroke or fill
 * (default to fill)
 * </ul>
 * <ul>
 * fadeOut: Define the time (in ms) until the indicator will fade out (default
 * to 0 = never fade out)
 * </ul>
 * <ul>
 * radius: Define the circle radius (default to 4.0)
 * </ul>
 */
public class SimpleFlowIndicator extends View implements FlowIndicator,
		AnimationListener {
	private static final int STYLE_STROKE = 0;
	private static final int STYLE_FILL = 1;
	
	public static final int TYPE_CIRCLE = 0;
	public static final int TYPE_SQUARE = 1;

	private float width;
	private float height;
	private float radius;
	private float gap;
	private int type;
	private int fadeOutTime = 0;
	private final Paint mPaintInactive = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint mPaintActive = new Paint(Paint.ANTI_ALIAS_FLAG);
	private ViewFlow viewFlow;
	private int currentScroll = 0;
	private int flowWidth = 0;
	private FadeTimer timer;
	public AnimationListener animationListener = this;
	private Animation animation;
	//private boolean mCentered = false;
	private float w, h;

	/**
	 * Default constructor
	 * 
	 * @param context
	 */
	public SimpleFlowIndicator(Context context, int type) {
		super(context);
		this.type = type;
		if (type == TYPE_CIRCLE) {
			radius = 2;
			w = 4;
			h = 4;
			gap = 2;
		} else if (type == TYPE_SQUARE) {
			width = 4;
			height = 2;
			w = 4;
			h = 2;
			gap = 2;
		} else {
			throw new IllegalArgumentException("type should be one of TYPE_CIRCLE or TYPE_SQUARE");
		}
		initColors(0xFFFFFFFF, 0xFFFFFFFF, STYLE_FILL, STYLE_STROKE);
	}

	/**
	 * The contructor used with an inflater
	 * 
	 * @param context
	 * @param attrs
	 */
	public SimpleFlowIndicator(Context context, AttributeSet attrs) {
		super(context, attrs);
		// Retrieve styles attributs
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.SimpleFlowIndicator);

		// Gets the inactive circle type, defaulting to "fill"
		int activeType = a.getInt(R.styleable.SimpleFlowIndicator_activeType,
				STYLE_FILL);

		int activeDefaultColor = 0xFFFFFFFF;

		// Get a custom inactive color if there is one
		int activeColor = a
				.getColor(R.styleable.SimpleFlowIndicator_activeColor,
						activeDefaultColor);

		// Gets the inactive circle type, defaulting to "stroke"
		int inactiveType = a.getInt(
				R.styleable.SimpleFlowIndicator_inactiveType, STYLE_STROKE);

		int inactiveDefaultColor = 0x44FFFFFF;
		// Get a custom inactive color if there is one
		int inactiveColor = a.getColor(
				R.styleable.SimpleFlowIndicator_inactiveColor,
				inactiveDefaultColor);

		// Retrieve the radius when circle
		radius = a.getDimension(R.styleable.SimpleFlowIndicator_radius, 4.0f);
		
		// Retrieve the width when square
		width = a.getDimension(R.styleable.SimpleFlowIndicator_width, 5.0f);
		
		// Retrieve the height when square
		height = a.getDimension(R.styleable.SimpleFlowIndicator_height, 2.0f);
				
		// Retrieve the indicator type
		type = a.getInt(
				R.styleable.SimpleFlowIndicator_type, TYPE_CIRCLE);

		if (type == TYPE_CIRCLE) {
			w = radius * 2;
			h = radius * 2;
		} else if (type == TYPE_SQUARE) {
			w = width;
			h = height;
		}
		
		// Retrieve the gap when square
		gap = a.getDimension(R.styleable.SimpleFlowIndicator_gap, w / 2);
		
		// Retrieve the fade out time
		fadeOutTime = a.getInt(R.styleable.SimpleFlowIndicator_fadeOut, 0);

		/*mCentered = a.getBoolean(R.styleable.CircleFlowIndicator_centered,
				false);*/

		
		initColors(activeColor, inactiveColor, activeType, inactiveType);
		a.recycle();
	}

	private void initColors(int activeColor, int inactiveColor, int activeType,
			int inactiveType) {
		// Select the paint type given the type attr
		switch (inactiveType) {
		case STYLE_FILL:
			mPaintInactive.setStyle(Style.FILL);
			break;
		default:
			mPaintInactive.setStyle(Style.STROKE);
		}
		mPaintInactive.setColor(inactiveColor);

		// Select the paint type given the type attr
		switch (activeType) {
		case STYLE_STROKE:
			mPaintActive.setStyle(Style.STROKE);
			break;
		default:
			mPaintActive.setStyle(Style.FILL);
		}
		mPaintActive.setColor(activeColor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View#onDraw(android.graphics.Canvas)
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int count = 3;
		if (viewFlow != null) {
			count = viewFlow.getViewsCount();
		}
		if (count == 0) return;

		float circleSeparation = w + gap;
		
		// this is the amount the first circle should be offset to make the
		// entire thing centered
		float centeringOffset = gap / 2;

		int leftPadding = getPaddingLeft();

		// Draw stroked circles
		float cx = 0;
		if (flowWidth != 0) {
			// Draw the filled circle according to the current scroll
			cx = (currentScroll * circleSeparation) / flowWidth;
		}
		float left = leftPadding + cx + centeringOffset;
		float top = getPaddingTop();
		float right = left + w;
		float bottom = top + h;
		float gleft = left - 1, gright = left - 1;
		
		
		for (int iLoop = 0; iLoop < count; iLoop++) {
			/*canvas.drawCircle(leftPadding + radius + (iLoop * circleSeparation)
					+ centeringOffset, getPaddingTop() + radius, radius,
					mPaintInactive);*/
			/*******************/
			float left1 = leftPadding + (iLoop * circleSeparation)
					+ centeringOffset;
			float top1 = getPaddingTop();
			float right1 = left1 + w;
			float bottom1 = top1 + h;
			if (type == TYPE_CIRCLE) {
				canvas.drawCircle(left1 + radius, top1 + radius, radius,
						mPaintInactive);
			} else if (type == TYPE_SQUARE) {
				canvas.drawRect(left1, top1, right1, bottom1, mPaintInactive);
				if (left1 > left && gleft < left) {
					gleft = left1;
				}
				if (right1 > left && gright < left) {
					gright = right1;
				}
			}
			/*******************/
		}
		
		// The flow width has been upadated yet. Draw the default position
		/*canvas.drawCircle(leftPadding + radius + cx + centeringOffset,
				getPaddingTop() + radius, radius, mPaintActive);*/
		/******************/
		if (type == TYPE_CIRCLE) {
			canvas.drawCircle(left + radius, top + radius, radius, mPaintActive);
		} else if (type == TYPE_SQUARE) {
			if (gright >= left && right >= gright) {
				canvas.drawRect(left, top, gright, bottom, mPaintActive);
			}
			if (right >= gleft && gleft >= left) {
				canvas.drawRect(gleft, top, right, bottom, mPaintActive);
			}
		}
		/******************/
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.taptwo.android.widget.ViewFlow.ViewSwitchListener#onSwitched(android
	 * .view.View, int)
	 */
	@Override
	public void onSwitched(View view, int position) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.taptwo.android.widget.FlowIndicator#setViewFlow(org.taptwo.android
	 * .widget.ViewFlow)
	 */
	@Override
	public void setViewFlow(ViewFlow view) {
		resetTimer();
		viewFlow = view;
		flowWidth = viewFlow.getWidth();
		invalidate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.taptwo.android.widget.FlowIndicator#onScrolled(int, int, int,
	 * int)
	 */
	@Override
	public void onScrolled(int h, int v, int oldh, int oldv) {
		setVisibility(View.VISIBLE);
		resetTimer();
		currentScroll = h;
		flowWidth = viewFlow.getWidth();
		invalidate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View#onMeasure(int, int)
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(measureWidth(widthMeasureSpec),
				measureHeight(heightMeasureSpec));
	}

	/**
	 * Determines the width of this view
	 * 
	 * @param measureSpec
	 *            A measureSpec packed into an int
	 * @return The width of the view, honoring constraints from measureSpec
	 */
	private int measureWidth(int measureSpec) {
		int result = 0;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);
		
		int count = 3;
		if (viewFlow != null) {
			count = viewFlow.getViewsCount();
		}

		// We were told how big to be
		if (specMode == MeasureSpec.EXACTLY) {
			result = specSize;
			if (count > 0) {
				w = specSize / count - gap;
			}
		}
		// Calculate the width according the views count
		else {
			
			result = (int) (getPaddingLeft() + getPaddingRight()
					+ count * (w + gap) + 1);
			// Respect AT_MOST value if that was what is called for by
			// measureSpec
			if (specMode == MeasureSpec.AT_MOST) {
				result = Math.min(result, specSize);
			}
		}
		return result;
	}

	/**
	 * Determines the height of this view
	 * 
	 * @param measureSpec
	 *            A measureSpec packed into an int
	 * @return The height of the view, honoring constraints from measureSpec
	 */
	private int measureHeight(int measureSpec) {
		int result = 0;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		// We were told how big to be
		if (specMode == MeasureSpec.EXACTLY) {
			result = specSize;
		}
		// Measure the height
		else {
			result = (int) (h + getPaddingTop() + getPaddingBottom() + 1);
			// Respect AT_MOST value if that was what is called for by
			// measureSpec
			if (specMode == MeasureSpec.AT_MOST) {
				result = Math.min(result, specSize);
			}
		}
		return result;
	}

	/**
	 * Sets the fill color
	 * 
	 * @param color
	 *            ARGB value for the text
	 */
	public void setFillColor(int color) {
		mPaintActive.setColor(color);
		invalidate();
	}

	/**
	 * Sets the stroke color
	 * 
	 * @param color
	 *            ARGB value for the text
	 */
	public void setStrokeColor(int color) {
		mPaintInactive.setColor(color);
		invalidate();
	}
	
	/**
	 * Sets the square dimension
	 * 
	 * @param width
	 *            width of the square
	 * @param height
	 *            height of the square
	 */
	public void setSquareDimension(float width, float height) {
		this.width = width;
		this.height = height;
		w = width;
		h = height;
		invalidate();
	}
	
	/**
	 * Sets the gap between indicators
	 * 
	 * @param g
	 *            the gap in px
	 */
	public void setGap(float g) {
		gap = g;
		invalidate();
	}

	/**
	 * Resets the fade out timer to 0. Creating a new one if needed
	 */
	private void resetTimer() {
		// Only set the timer if we have a timeout of at least 1 millisecond
		if (fadeOutTime > 0) {
			// Check if we need to create a new timer
			if (timer == null || timer._run == false) {
				// Create and start a new timer
				timer = new FadeTimer();
				timer.execute();
			} else {
				// Reset the current tiemr to 0
				timer.resetTimer();
			}
		}
	}

	/**
	 * Counts from 0 to the fade out time and animates the view away when
	 * reached
	 */
	private class FadeTimer extends AsyncTask<Void, Void, Void> {
		// The current count
		private int timer = 0;
		// If we are inside the timing loop
		private boolean _run = true;

		public void resetTimer() {
			timer = 0;
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			while (_run) {
				try {
					// Wait for a millisecond
					Thread.sleep(1);
					// Increment the timer
					timer++;

					// Check if we've reached the fade out time
					if (timer == fadeOutTime) {
						// Stop running
						_run = false;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			animation = AnimationUtils.loadAnimation(getContext(),
					android.R.anim.fade_out);
			animation.setAnimationListener(animationListener);
			startAnimation(animation);
		}
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		setVisibility(View.GONE);
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
	}

	@Override
	public void onAnimationStart(Animation animation) {
	}
}