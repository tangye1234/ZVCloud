package com.zigvine.android.anim;

import java.util.LinkedList;
import java.util.Queue;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.TextView;

public class QueuedTextView extends TextView {
	
	private Queue<CharSequence> queue = new LinkedList<CharSequence>();  
	private Context mContext;
	private boolean finished;

	public QueuedTextView(Context context) {
		this(context, null);
	}

	public QueuedTextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public QueuedTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		finished = true;
	}
	
	public void setQueuedText(int resid) {
		setQueuedText(getContext().getResources().getText(resid));
	}

	public void setQueuedText(CharSequence text) {
		queue.offer(text);
		if (finished) {
			fadeOut();
		}
	}
	
	private void fadeOut() {
		finished = false;
		if (getText().length() == 0) {
			CharSequence text = queue.poll();
			if (text != null) {
				setText(text);
				fadeIn();
			}
			return;
		}
		Animation anim = AnimUtils.FadeOut.loadAnimation(mContext, 300);
		anim.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				setVisibility(View.INVISIBLE);
				CharSequence text = queue.poll();
				if (text != null) {
					setText(text);
					fadeIn();
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation) {}

			@Override
			public void onAnimationStart(Animation animation) {}
			
		});
		startAnimation(anim);
	}
	
	private void fadeIn() {
		if (getText().length() == 0) {
			if (queue.size() > 0) {
				fadeOut();
			} else {
				finished = true;
			}
			return;
		}
		Animation anim = AnimUtils.FadeIn.loadAnimation(mContext, 300);
		anim.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				if (queue.size() > 0) {
					fadeOut();
				} else {
					finished = true;
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation) {}

			@Override
			public void onAnimationStart(Animation animation) {
				setVisibility(View.VISIBLE);
			}
			
		});
		startAnimation(anim);
	}
	
	public boolean isQueuedFinished() {
		return finished;
	}

}
