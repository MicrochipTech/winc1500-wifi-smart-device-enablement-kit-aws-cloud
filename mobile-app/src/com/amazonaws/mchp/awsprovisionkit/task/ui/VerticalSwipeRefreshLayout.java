package com.amazonaws.mchp.awsprovisionkit.task.ui;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class VerticalSwipeRefreshLayout extends SwipeRefreshLayout {
	private int mTouchSlop;
	private float mPrevX;

	public VerticalSwipeRefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mPrevX = event.getX();
			break;

		case MotionEvent.ACTION_MOVE:
			final float eventX = event.getX();
			float xDiff = Math.abs(eventX - mPrevX);
			// Log.d("refresh" ,"move----" + eventX + " " + mPrevX + " " +
			// mTouchSlop);
			if (xDiff > mTouchSlop + 60) {
				return false;
			}
		}

		return super.onInterceptTouchEvent(event);
	}
}
