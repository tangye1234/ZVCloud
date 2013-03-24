
package com.zigvine.android.anim;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

public class AnimUtils {
	
	public static AnimationListener loadStartListener(final View v, final int visibility) {
		return new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
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
	
	public static AnimationListener loadEndListener(final View v, final int visibility) {
		return new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				v.setVisibility(visibility);
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
		}
	}

}
