
package com.zigvine.android.anim;

import com.zigvine.zagriculture.R;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LayoutAnimationController;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

public class AnimUtils {
	
	public static AnimationListener loadStartListener(final View v, final int visibility, final Runnable...r) {
		return new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				for (Runnable run : r) {
					if (run != null) {
						run.run();
					}
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
				v.setVisibility(visibility);
			}

		};
	}
	
	public static AnimationListener loadEndListener(final View v, final int visibility, final Runnable...r) {
		return new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				v.setVisibility(visibility);
				for (Runnable run : r) {
					if (run != null) {
						run.run();
					}
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
			}

		};
	}

	public static class FadeIn {

		public static Animation loadAnimation(Context context, long duration) {
			Animation anim = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
			anim.setDuration(duration);
			return anim;
		}

		public static void startAnimation(View view, long duration) {
			Animation anim = AnimationUtils
					.loadAnimation(view.getContext(), android.R.anim.fade_in);
			anim.setDuration(duration);
			anim.setAnimationListener(loadStartListener(view, View.VISIBLE));
			view.startAnimation(anim);
		}
		
	}

	public static class FadeOut {

		public static Animation loadAnimation(Context context, long duration) {
			Animation anim = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
			anim.setDuration(duration);
			return anim;
		}

		public static void startAnimation(View view, long duration) {
			Animation anim = AnimationUtils.loadAnimation(view.getContext(),
					android.R.anim.fade_out);
			anim.setDuration(duration);
			anim.setAnimationListener(loadEndListener(view, View.INVISIBLE));
			view.startAnimation(anim);
		}
	}

	public static class ScaleIn {

		public static Animation loadAnimation(Context context, long duration) {
			Animation anim = new ScaleAnimation(1, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			return anim;
		}

		public static void startAnimation(View view, long duration) {
			Animation anim = new ScaleAnimation(1, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			anim.setAnimationListener(loadStartListener(view, View.VISIBLE));
			view.startAnimation(anim);
		}
	}

	public static class ScaleOut {

		public static Animation loadAnimation(Context context, long duration) {
			Animation anim = new ScaleAnimation(1, 1, 1, 0, Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			return anim;
		}

		public static void startAnimation(View view, long duration) {
			Animation anim = new ScaleAnimation(1, 1, 1, 0, Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			anim.setAnimationListener(loadEndListener(view, View.INVISIBLE));
			view.startAnimation(anim);
		}
	}

	public static class DropIn {

		public static Animation loadAnimation(Context context, long duration) {
			Animation anim = new TranslateAnimation(0, 0, -500, 0);
			anim.setDuration(duration);
			anim.setInterpolator(context, android.R.anim.decelerate_interpolator);
			return anim;
		}

		public static void startAnimation(View view, long duration) {
			Animation anim = loadAnimation(view.getContext(), duration);
			anim.setAnimationListener(loadStartListener(view, View.VISIBLE));
			view.startAnimation(anim);
		}
	}

	public static class DropOut {

		public static Animation loadAnimation(Context context, long duration) {
			Animation anim = new TranslateAnimation(0, 0, 0, -500);
			anim.setDuration(duration);
			anim.setInterpolator(context, android.R.anim.accelerate_interpolator);
			return anim;
		}

		public static void startAnimation(View view, long duration) {
			Animation anim = loadAnimation(view.getContext(), duration);
			anim.setAnimationListener(loadEndListener(view, View.GONE));
			view.startAnimation(anim);
		}
	}
	
	public static class SlideInUp {
		
		public static Animation loadAnimation(Context context, long duration) {
			Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_in_child_bottom);
			anim.setDuration(duration);
			return anim;
		}
		
		public static void startAnimation(View view, long duration) {
			Animation anim = loadAnimation(view.getContext(), duration);
			anim.setAnimationListener(loadStartListener(view, View.VISIBLE));
			view.startAnimation(anim);
		}
		
	}

	public static class CustomAnimation implements Runnable {
		long duration;

		int start, end, step, lastStep;

		Handler handler;

		Callback callback;

		long startTime;

		int distance;

		float accelerater;

		long minTime = 15;

		public static interface Callback {
			public void process(int value);
			public void onAnimationEnd();
		}

		public CustomAnimation(long Duration, int Start, int End, Callback Call_back) {
			duration = Duration;
			start = Start;
			end = End;

			distance = end - start;
			accelerater = 6f * (float)distance / duration / duration / duration;

			callback = Call_back;
			handler = new Handler();
		}

		public void setAnimationFrequency(int freq) {
			minTime = 1000 / freq;
		}

		public void startAnimation() {
			startTime = SystemClock.elapsedRealtime();
			// android.util.Log.i("anim", "start");
			// TODO Reset all variable
			lastStep = 0;
			run();
		}

		public void run() {
			float elapsed = (float)(SystemClock.elapsedRealtime() - startTime);
			boolean post = false;
			if (elapsed < duration) {
				step = Math.round(accelerater * ((float)duration / 2f - elapsed / 3f) * elapsed
						* elapsed);
				// android.util.Log.i("anim", "step = " + (step - lastStep));
				// step = Math.round((float)distance / (float)duration *
				// (float)elapsed);
				post = true;
				// handler.postDelayed(this, minTime);
			} else {
				step = distance;
			}
			// callback.process(step + start);
			if (step != lastStep) {
				if (post)
					handler.postDelayed(this, minTime);
				callback.process(step + start);
				lastStep = step;
			} else if (post) {
				handler.postDelayed(this, 5);
			}
			if (!post) {
				callback.onAnimationEnd();
			}
		}
		
		public void cancel() {
			handler.removeCallbacks(this);
		}
	}
	
	public static class CustomLayoutAnimationController extends LayoutAnimationController {

		public static final int ORDER_FROM_INDEX = 100;
		
		private Animation mAnimation;
		private int mStartIndex;
		private long mDuration;
		
		public CustomLayoutAnimationController(Animation animation) {
			super(animation);
			// TODO Auto-generated constructor stub
		}
		public CustomLayoutAnimationController(Animation animation, float delay) {
			super(animation, delay);
			// TODO Auto-generated constructor stub
		}
		public CustomLayoutAnimationController(Context context,
				AttributeSet attrs) {
			super(context, attrs);
			// TODO Auto-generated constructor stub
		}
		
		public void setStartIndex(int startIndex) {
			mStartIndex = startIndex;
			setOrder(ORDER_FROM_INDEX);
			mAnimation = getAnimation();
			mDuration = mAnimation.getDuration();
		}
		
		@Override
		protected long getDelayForView(View view) {
			if (getOrder() == ORDER_FROM_INDEX) {
				ViewGroup.LayoutParams lp = view.getLayoutParams();
		        AnimationParameters params = lp.layoutAnimationParameters;
		        if (params.index < mStartIndex) {
		        	mAnimation.setDuration(0);
		        } else {
		        	mAnimation.setDuration(mDuration);
		        }
			}
			return super.getDelayForView(view);
		}
		
		@Override
		protected int getTransformedIndex(AnimationParameters params) {
	        switch (getOrder()) {
	        	case ORDER_FROM_INDEX:
	        		if (params.index < mStartIndex) {
	        			return 0;
	        		} else {
	        			return params.index - mStartIndex;
	        		}
	            default:
	                return super.getTransformedIndex(params);
	        }
	    }

	}

}
