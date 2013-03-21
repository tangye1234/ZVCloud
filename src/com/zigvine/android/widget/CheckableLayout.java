package com.zigvine.android.widget;

import com.zigvine.zagriculture.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class CheckableLayout extends LinearLayout implements Checkable {
	
	public CheckableLayout (Context context) {
        super(context);
    }

    public CheckableLayout (Context context, AttributeSet attrs) {
        super(context, attrs);
    }

	private boolean mChecked;

	@Override
    public void setChecked(boolean checked) {
        mChecked = checked;
        setBackgroundResource(checked ? R.drawable.select
                : R.color.transparent);
    }

	@Override
    public boolean isChecked() {
        return mChecked;
    }

	@Override
    public void toggle() {
        setChecked(!mChecked);
    }

}
