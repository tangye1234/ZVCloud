/*
 * Copyright (C) 2011 Patrik Åkerfeldt
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

import java.util.ArrayList;
import java.util.LinkedList;

import com.zigvine.android.utils.Utils;
import com.zigvine.zagriculture.R;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Scroller;

/**
 * A horizontally scrollable {@link ViewGroup} with items populated from an
 * {@link Adapter}. The ViewFlow uses a buffer to store loaded {@link View}s in.
 * The default size of the buffer is 3 elements on both sides of the currently
 * visible {@link View}, making up a total buffer size of 3 * 2 + 1 = 7. The
 * buffer size can be changed using the {@code sidebuffer} xml attribute.
 * 
 */
public class ViewFlow extends AdapterView<Adapter> {

	private static final boolean DBG = false;
	private static final int SNAP_VELOCITY = 1000;
	private static final int INVALID_SCREEN = -1;
	private static final int TOUCH_STATE_REST = 0;
	private static final int TOUCH_STATE_SCROLLING = 1;
	private static final int TIMEOUT_SWITCH = 5000;

	private LinkedList<View> mLoadedViews;
	private int mCurrentBufferIndex;
	private int mCurrentAdapterIndex;
	private int mSideBuffer = 2;
	private Scroller mScroller;
	private VelocityTracker mVelocityTracker;
	private int mTouchState = TOUCH_STATE_REST;
	private float mLastMotionX;
	private float mLastMotionY;
	private int mTouchSlop;
	private int mMaximumVelocity;
	private int mCurrentScreen;
	private int mNextScreen = INVALID_SCREEN;
	private boolean mFirstLayout = true;
	private ViewSwitchListener mViewSwitchListener;
	private SwitchDecideListener switchListener;
	private Adapter mAdapter;
	private int mLastScrollDirection;
	private AdapterDataSetObserver mDataSetObserver;
	private FlowIndicator mIndicator;
	private int mLastOrientation = -1;
	private boolean auto;
	private float xMarginBegin;
	private float marginMax, marginMin;
	
	/**
	 * set an intercept x margin
	 * @param max in dp
	 * @param min in dp
	 */
	public void setInterceptXMarginInDp(float max, float min) {
		marginMax = Utils.dp2px(getContext(), max);
		marginMin = Utils.dp2px(getContext(), min);
	}
	
	private boolean isXOutMargin() {
		return (xMarginBegin > marginMax || xMarginBegin < marginMin);
	}

	private OnGlobalLayoutListener orientationChangeListener = new OnGlobalLayoutListener() {

		@SuppressWarnings("deprecation")
		@Override
		public void onGlobalLayout() {
			getViewTreeObserver().removeGlobalOnLayoutListener( // instead by removeOnClobalLayoutListener
					orientationChangeListener);
			setSelection(mCurrentAdapterIndex);
		}
	};

	/**
	 * Receives call backs when a new {@link View} has been scrolled to.
	 */
	public static interface ViewSwitchListener {

		/**
		 * This method is called when a new View has been scrolled to.
		 * 
		 * @param view
		 *            the {@link View} currently in focus.
		 * @param position
		 *            The position in the adapter of the {@link View} currently
		 *            in focus.
		 */
		void onSwitched(View view, int position);

	}
	
	public static interface SwitchDecideListener {
		void onSwitchDecide(int position);
	}

	public ViewFlow(Context context) {
		super(context);
		mSideBuffer = 3;
		init();
	}

	public ViewFlow(Context context, int sideBuffer) {
		super(context);
		mSideBuffer = sideBuffer;
		init();
	}

	public ViewFlow(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray styledAttrs = context.obtainStyledAttributes(attrs,
				R.styleable.ViewFlow);
		mSideBuffer = styledAttrs.getInt(R.styleable.ViewFlow_sidebuffer, 3);
		auto = styledAttrs.getBoolean(R.styleable.ViewFlow_autoflow, false);
		setAutomaticScroll(auto);
		init();
		styledAttrs.recycle();
	}

	private void init() {
		mLoadedViews = new LinkedList<View>();
		mScroller = new Scroller(getContext());
		final ViewConfiguration configuration = ViewConfiguration
				.get(getContext());
		mTouchSlop = configuration.getScaledTouchSlop();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
	}

	public void onConfigurationChanged(Configuration newConfig) {
		if (newConfig.orientation != mLastOrientation) {
			mLastOrientation = newConfig.orientation;
			getViewTreeObserver().addOnGlobalLayoutListener(
					orientationChangeListener);
		}
	}

	public int getViewsCount() {
		return mAdapter.getCount();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		final int width = MeasureSpec.getSize(widthMeasureSpec);
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		if (widthMode != MeasureSpec.EXACTLY && !isInEditMode()) {
			throw new IllegalStateException(
					"ViewFlow can only be used in EXACTLY mode.");
		}

		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		/*if (heightMode != MeasureSpec.EXACTLY && !isInEditMode()) {
			throw new IllegalStateException(
					"ViewFlow can only be used in EXACTLY mode.");
		}*/

		// The children are given the same width and height as the workspace
		int maxH = 0;
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
			int h = getChildAt(i).getMeasuredHeight();
			if (h > maxH) maxH = h;
		}
		
		if (heightMode != MeasureSpec.EXACTLY) {
			setMeasuredDimension(width, maxH);
		} else {
			setMeasuredDimension(width, MeasureSpec.getSize(heightMeasureSpec));
		}

		if (mFirstLayout) {
			mScroller.startScroll(0, 0, mCurrentScreen * width, 0, 0);
			mFirstLayout = false;
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int childLeft = 0;

		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != View.GONE) {
				final int childWidth = child.getMeasuredWidth();
				child.layout(childLeft, 0, childLeft + childWidth,
						child.getMeasuredHeight());
				childLeft += childWidth;
			}
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (getChildCount() == 0)
			return false;

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);

		final int action = ev.getAction();
		final float x = ev.getX();
		final float y = ev.getY();
		final ViewParent parent = getParent();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			// add autoscroll feature
			removeCallbacks(r);
			// end
			xMarginBegin = x;
			if (parent != null && isXOutMargin()) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
			
			/*
			 * If being flinged and user touches, stop the fling. isFinished
			 * will be false if being flinged.
			 */
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
			}

			// Remember where the motion event started
			mLastMotionX = x;
			mLastMotionY = y;

			mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST
					: TOUCH_STATE_SCROLLING;

			break;

		case MotionEvent.ACTION_MOVE:
			final int xDiff = (int) Math.abs(x - mLastMotionX);
			final int yDiff = (int) Math.abs(y - mLastMotionY);

			boolean xMoved = xDiff > mTouchSlop && xDiff > yDiff;

			if (xMoved) {
				// Scroll if the user moved far enough along the X axis
				mTouchState = TOUCH_STATE_SCROLLING;
			}

			if (mTouchState == TOUCH_STATE_SCROLLING) {
				// Scroll to follow the motion event
				final int deltaX = (int) (mLastMotionX - x);
				mLastMotionX = x;
				mLastMotionY = y;

				final int scrollX = getScrollX();
				if (deltaX < 0) {
					if (scrollX > 0) {
						scrollBy(Math.max(-scrollX, deltaX), 0);
					} else if (parent != null && isXOutMargin()) {
			        	parent.requestDisallowInterceptTouchEvent(false);
					}
				} else if (deltaX > 0) {
					final int availableToScroll = getChildAt(
							getChildCount() - 1).getRight()
							- scrollX - getWidth();
					if (availableToScroll > 0) {
						scrollBy(Math.min(availableToScroll, deltaX), 0);
					} else if (parent != null && isXOutMargin()) {
			        	parent.requestDisallowInterceptTouchEvent(false);
					}
				}
				return true;
			}
			break;

		case MotionEvent.ACTION_UP:
			if (auto) {
				postDelayed(r, 5000);
			}
			if (mTouchState == TOUCH_STATE_SCROLLING) {
				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
				int velocityX = (int) velocityTracker.getXVelocity();

				if (velocityX > SNAP_VELOCITY && mCurrentScreen > 0) {
					// Fling hard enough to move left
					snapToScreen(mCurrentScreen - 1, velocityX);
				} else if (velocityX < -SNAP_VELOCITY
						&& mCurrentScreen < getChildCount() - 1) {
					// Fling hard enough to move right
					snapToScreen(mCurrentScreen + 1, velocityX);
				} else {
					snapToDestination();
				}

				if (mVelocityTracker != null) {
					mVelocityTracker.recycle();
					mVelocityTracker = null;
				}
			}

			mTouchState = TOUCH_STATE_REST;

			break;
		case MotionEvent.ACTION_CANCEL:
			mTouchState = TOUCH_STATE_REST;
		}
		return mTouchState == TOUCH_STATE_SCROLLING;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (getChildCount() == 0)
			return false;

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);

		final int action = ev.getAction();
		final float x = ev.getX();
		final float y = ev.getY();
		final ViewParent parent = getParent();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			// add autoscroll feature
			removeCallbacks(r);
			// end
			/*
			 * If being flinged and user touches, stop the fling. isFinished
			 * will be false if being flinged.
			 */
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
			}

			// Remember where the motion event started
			mLastMotionX = x;
			mLastMotionY = y;

			mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST
					: TOUCH_STATE_SCROLLING;

			break;

		case MotionEvent.ACTION_MOVE:
			final int xDiff = (int) Math.abs(x - mLastMotionX);
			final int yDiff = (int) Math.abs(y - mLastMotionY);

			boolean xMoved = xDiff > mTouchSlop && xDiff > yDiff;

			if (xMoved) {
				// Scroll if the user moved far enough along the X axis
				mTouchState = TOUCH_STATE_SCROLLING;
			}

			if (mTouchState == TOUCH_STATE_SCROLLING) {
				// Scroll to follow the motion event
				final int deltaX = (int) (mLastMotionX - x);
				mLastMotionX = x;
				mLastMotionY = y;

				final int scrollX = getScrollX();
				if (deltaX < 0) {
					if (scrollX > 0) {
						scrollBy(Math.max(-scrollX, deltaX), 0);
					} else if (parent != null && isXOutMargin()) {
			        	parent.requestDisallowInterceptTouchEvent(false);
					}
				} else if (deltaX > 0) {
					final int availableToScroll = getChildAt(
							getChildCount() - 1).getRight()
							- scrollX - getWidth();
					if (availableToScroll > 0) {
						scrollBy(Math.min(availableToScroll, deltaX), 0);
					} else if (parent != null && isXOutMargin()) {
			        	parent.requestDisallowInterceptTouchEvent(false);
					}
				}
				return true;
			}
			break;

		case MotionEvent.ACTION_UP:
			if (auto) {
				postDelayed(r, 5000);
			}
			if (mTouchState == TOUCH_STATE_SCROLLING) {
				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
				int velocityX = (int) velocityTracker.getXVelocity();

				if (velocityX > SNAP_VELOCITY && mCurrentScreen > 0) {
					// Fling hard enough to move left
					snapToScreen(mCurrentScreen - 1, velocityX);
				} else if (velocityX < -SNAP_VELOCITY
						&& mCurrentScreen < getChildCount() - 1) {
					// Fling hard enough to move right
					snapToScreen(mCurrentScreen + 1, velocityX);
				} else {
					snapToDestination();
				}

				if (mVelocityTracker != null) {
					mVelocityTracker.recycle();
					mVelocityTracker = null;
				}
			}

			mTouchState = TOUCH_STATE_REST;

			break;
		case MotionEvent.ACTION_CANCEL:
			snapToDestination();
			mTouchState = TOUCH_STATE_REST;
		}
		return true;
	}

	@Override
	protected void onScrollChanged(int h, int v, int oldh, int oldv) {
		super.onScrollChanged(h, v, oldh, oldv);
		if (mIndicator != null) {
			/*
			 * The actual horizontal scroll origin does typically not match the
			 * perceived one. Therefore, we need to calculate the perceived
			 * horizontal scroll origin here, since we use a view buffer.
			 */
			int hPerceived = h + (mCurrentAdapterIndex - mCurrentBufferIndex)
					* getWidth();
			mIndicator.onScrolled(hPerceived, v, oldh, oldv);
		}
	}

	private void snapToDestination() {
		final int screenWidth = getWidth();
		final int whichScreen = (getScrollX() + (screenWidth / 2))
				/ screenWidth;

		snapToScreen(whichScreen, 0);
	}

	private void snapToScreen(int whichScreen, int velocity) {
		mLastScrollDirection = whichScreen - mCurrentScreen;
		if (!mScroller.isFinished())
			return;

		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
		
		if (switchListener != null) {
			switchListener.onSwitchDecide(mCurrentAdapterIndex + whichScreen - mCurrentScreen);
		}

		mNextScreen = whichScreen;

		final int newX = whichScreen * getWidth();
		final int delta = newX - getScrollX();
		if (velocity == 0) {
			mScroller.startScroll(getScrollX(), 0, delta, 0, Math.abs(delta));
		} else {
			mScroller.startScroll(getScrollX(), 0, delta, 0, Math.abs(2000*delta/velocity));
		}
		invalidate();
	}
	
	private Runnable r = new Runnable() {
		public void run() {
			snapNext();
			postDelayed(r, TIMEOUT_SWITCH);
		}
	};
	
	public void snapNext() {
		if (getChildCount() != 0) {
			if (mCurrentAdapterIndex == getViewsCount() - 1) {
				setSelection(0);
			} else {
				snapToScreen((mCurrentScreen + 1), 0);
			}
		}
	}
	
	public void setAutomaticScroll(boolean automatic) {
		auto = automatic;
		removeCallbacks(r);
		if (auto) {
			postDelayed(r, TIMEOUT_SWITCH);
		}
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			postInvalidate();
		} else if (mNextScreen != INVALID_SCREEN) {
			mCurrentScreen = Math.max(0,
					Math.min(mNextScreen, getChildCount() - 1));
			mNextScreen = INVALID_SCREEN;
			postViewSwitched(mLastScrollDirection);
		}
	}

	/**
	 * Scroll to the {@link View} in the view buffer specified by the index.
	 * 
	 * @param indexInBuffer
	 *            Index of the view in the view buffer.
	 */
	private void setVisibleView(int indexInBuffer, boolean uiThread) {
		mCurrentScreen = Math.max(0,
				Math.min(indexInBuffer, getChildCount() - 1));
		int dx = (mCurrentScreen * getWidth()) - mScroller.getCurrX();
		mScroller.startScroll(mScroller.getCurrX(), mScroller.getCurrY(), dx,
				0, 0);
		if (dx == 0)
			onScrollChanged(mScroller.getCurrX() + dx, mScroller.getCurrY(),
					mScroller.getCurrX() + dx, mScroller.getCurrY());
		if (uiThread)
			invalidate();
		else
			postInvalidate();
	}

	/**
	 * Set the listener that will receive notifications every time the {code
	 * ViewFlow} scrolls.
	 * 
	 * @param l
	 *            the scroll listener
	 */
	public void setOnViewSwitchListener(ViewSwitchListener l) {
		mViewSwitchListener = l;
	}
	
	public void setOnSwitchDecideListener(SwitchDecideListener l) {
		switchListener = l;
	}

	@Override
	public Adapter getAdapter() {
		return mAdapter;
	}

	@Override
	public void setAdapter(Adapter adapter) {
		setAdapter(adapter, 0);
	}

	public void setAdapter(Adapter adapter, int initialPosition) {
		if (mAdapter != null) {
			mAdapter.unregisterDataSetObserver(mDataSetObserver);
		}

		mAdapter = adapter;

		if (mAdapter == null)
			return;

		if (mAdapter != null) {
			mDataSetObserver = new AdapterDataSetObserver();
			mAdapter.registerDataSetObserver(mDataSetObserver);

		}

		setSelection(initialPosition);
	}

	@Override
	public View getSelectedView() {
		return (mCurrentBufferIndex < mLoadedViews.size() ? mLoadedViews
				.get(mCurrentBufferIndex) : null);
	}

	@Override
	public int getSelectedItemPosition() {
		return mCurrentAdapterIndex;
	}

	/**
	 * Set the FlowIndicator
	 * 
	 * @param flowIndicator
	 */
	public void setFlowIndicator(FlowIndicator flowIndicator) {
		mIndicator = flowIndicator;
		mIndicator.setViewFlow(this);
	}

	@Override
	public void setSelection(int position) {
		mNextScreen = INVALID_SCREEN;
		mScroller.forceFinished(true);
		if (mAdapter == null)
			return;

		position = Math.max(position, 0);
		position = Math.min(position, mAdapter.getCount() - 1);
		if (position < 0) return;

		ArrayList<View> recycleViews = new ArrayList<View>();
		View recycleView;
		View currentView = null;
		while (!mLoadedViews.isEmpty()) {
			recycleViews.add(recycleView = mLoadedViews.remove());
			detachViewFromParent(recycleView);
		}

		int removePos = position;
		currentView = makeAndAddView(position, true,
				(recycleViews.isEmpty() ? null : recycleViews.remove(removePos)));
		mLoadedViews.addLast(currentView);

		for (int offset = 1; mSideBuffer - offset >= 0; offset++) {
			int leftIndex = position - offset;
			int rightIndex = position + offset;
			if (leftIndex >= 0)
				mLoadedViews
						.addFirst(makeAndAddView(
								leftIndex,
								false,
								(recycleViews.isEmpty() ? null : recycleViews
										.remove(--removePos))));
			if (rightIndex < mAdapter.getCount())
				mLoadedViews
						.addLast(makeAndAddView(rightIndex, true, (recycleViews
								.isEmpty() ? null : recycleViews.remove(removePos))));
		}

		mCurrentBufferIndex = mLoadedViews.indexOf(currentView);
		mCurrentAdapterIndex = position;

		for (View view : recycleViews) {
			removeDetachedView(view, false);
		}
		requestLayout();
		setVisibleView(mCurrentBufferIndex, false);
		if (mIndicator != null) {
			mIndicator.onSwitched(mLoadedViews.get(mCurrentBufferIndex),
					mCurrentAdapterIndex);
		}
		if (mViewSwitchListener != null) {
			mViewSwitchListener
					.onSwitched(mLoadedViews.get(mCurrentBufferIndex),
							mCurrentAdapterIndex);
		}
		if (switchListener != null) {
			switchListener.onSwitchDecide(mCurrentAdapterIndex);
		}
	}

	private void resetFocus() {
		logBuffer();
		mLoadedViews.clear();
		removeAllViewsInLayout();

		for (int i = Math.max(0, mCurrentAdapterIndex - mSideBuffer); i < Math
				.min(mAdapter.getCount(), mCurrentAdapterIndex + mSideBuffer
						+ 1); i++) {
			mLoadedViews.addLast(makeAndAddView(i, true, null));
			if (i == mCurrentAdapterIndex)
				mCurrentBufferIndex = mLoadedViews.size() - 1;
		}
		logBuffer();
		requestLayout();
	}

	private void postViewSwitched(int direction) {
		if (direction == 0)
			return;

		if (direction > 0) { // to the right
			for (; direction > 0; direction-- ) {
				mCurrentAdapterIndex++;
				mCurrentBufferIndex++;
	
				View recycleView = null;
	
				// Remove view outside buffer range
				if (mCurrentAdapterIndex > mSideBuffer) {
					recycleView = mLoadedViews.removeFirst();
					detachViewFromParent(recycleView);
					// removeView(recycleView);
					mCurrentBufferIndex--;
				}
	
				// Add new view to buffer
				int newBufferIndex = mCurrentAdapterIndex + mSideBuffer;
				if (newBufferIndex < mAdapter.getCount())
					mLoadedViews.addLast(makeAndAddView(newBufferIndex, true,
							recycleView));
			}

		} else { // to the left
			for (; direction < 0; direction++ ) {
				mCurrentAdapterIndex--;
				mCurrentBufferIndex--;
				View recycleView = null;
	
				// Remove view outside buffer range
				if (mAdapter.getCount() - 1 - mCurrentAdapterIndex > mSideBuffer) {
					recycleView = mLoadedViews.removeLast();
					detachViewFromParent(recycleView);
				}
	
				// Add new view to buffer
				int newBufferIndex = mCurrentAdapterIndex - mSideBuffer;
				if (newBufferIndex > -1) {
					mLoadedViews.addFirst(makeAndAddView(newBufferIndex, false,
							recycleView));
					mCurrentBufferIndex++;
				}
			}

		}

		requestLayout();
		setVisibleView(mCurrentBufferIndex, true);
		if (mIndicator != null) {
			mIndicator.onSwitched(mLoadedViews.get(mCurrentScreen),
					mCurrentAdapterIndex);
		}
		if (mViewSwitchListener != null) {
			mViewSwitchListener
					.onSwitched(mLoadedViews.get(mCurrentScreen),
							mCurrentAdapterIndex);
		}
		logBuffer();
	}

	private View setupChild(View child, boolean addToEnd, boolean recycle) {
		ViewGroup.LayoutParams p = (ViewGroup.LayoutParams) child
				.getLayoutParams();
		if (p == null) {
			p = new AbsListView.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT, 0);
		}
		if (recycle)
			attachViewToParent(child, (addToEnd ? -1 : 0), p);
		else
			addViewInLayout(child, (addToEnd ? -1 : 0), p, true);
		return child;
	}

	private View makeAndAddView(int position, boolean addToEnd, View convertView) {
		View view = mAdapter.getView(position, convertView, this);
		return setupChild(view, addToEnd, convertView != null);
	}

	class AdapterDataSetObserver extends DataSetObserver {

		@Override
		public void onChanged() {
			View v = getChildAt(mCurrentBufferIndex);
			if (v != null) {
				for (int index = 0; index < mAdapter.getCount(); index++) {
					if (v.equals(mAdapter.getItem(index))) {
						mCurrentAdapterIndex = index;
						break;
					}
				}
			}
			resetFocus();
		}

		@Override
		public void onInvalidated() {
			// Not yet implemented!
		}

	}

	private void logBuffer() {
		if (DBG) {
			Log.d("viewflow", "Size of mLoadedViews: " + mLoadedViews.size()
					+ "X: " + mScroller.getCurrX() + ", Y: " + mScroller.getCurrY());
			Log.d("viewflow", "IndexInAdapter: " + mCurrentAdapterIndex
					+ ", IndexInBuffer: " + mCurrentBufferIndex);
		}
	}
}
