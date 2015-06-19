package com.mmx.miinsurance.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.mmx.miinsurance.R;

public class TuteScreenThree extends Fragment {

	public View onCreateView(LayoutInflater inflater, ViewGroup container,	Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.screen_tute_three, container, false);
		LinearLayout llViewParent = (LinearLayout) rootView.findViewById(R.id.ll_tute_three);

		final GestureDetector gestureDetector = new GestureDetector(new GestureListener());
		llViewParent.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (gestureDetector.onTouchEvent(event)) {
					return true;
				}
				return false;
			}
		});
		return rootView;
	}

	private final class GestureListener extends SimpleOnGestureListener {

		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			try {
				float diffY = e2.getY() - e1.getY();
				float diffX = e2.getX() - e1.getX();
				if (Math.abs(diffX) > Math.abs(diffY)) {
					if (diffX < 0) {
						Intent itn = new Intent(getActivity(), CouponCodeScreen.class);
						startActivity(itn);
						getActivity().finish();
					}
				}
			} catch (Exception exception) {
				exception.printStackTrace();
			}
			return super.onScroll(e1, e2, distanceX, distanceY);
		}
	}

}
