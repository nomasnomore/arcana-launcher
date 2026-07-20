package com.aaron.bluehour;

import android.content.Context;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

/**
 * The category stack with game-menu navigation: press and drag up/down
 * and the white slash highlight follows your finger word to word (with
 * haptic ticks); lift to open the highlighted one. A plain tap or
 * long-press on a word still works exactly as before.
 */
public class CategoryColumn extends LinearLayout {

    private float downX, downY;
    private boolean dragging = false;
    private View hovered = null;
    private final int slop;

    public CategoryColumn(Context c) {
        super(c);
        setOrientation(VERTICAL);
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                dragging = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = Math.abs(e.getY() - downY);
                float dx = Math.abs(e.getX() - downX);
                if (!dragging && dy > slop * 1.5f && dy > dx) {
                    dragging = true;
                    return true; // take over: children get ACTION_CANCEL
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // touch on column background (beside a short word): start hover
                dragging = true;
                setHover(wordAt(e.getY()), false);
                return true;
            case MotionEvent.ACTION_MOVE:
                setHover(wordAt(e.getY()), true);
                return true;
            case MotionEvent.ACTION_UP:
                if (hovered != null) {
                    final View h = hovered;
                    hovered = null;
                    h.performClick();
                    // let the white-slash press state linger through the open beat
                    postDelayed(new Runnable() {
                        @Override public void run() { h.setPressed(false); }
                    }, 160);
                }
                dragging = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                setHover(null, false);
                dragging = false;
                return true;
        }
        return super.onTouchEvent(e);
    }

    private View wordAt(float y) {
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            if (y >= c.getTop() && y < c.getBottom()) return c;
        }
        return null;
    }

    private void setHover(View v, boolean haptic) {
        if (v == hovered) return;
        if (hovered != null) hovered.setPressed(false);
        hovered = v;
        if (v != null) {
            v.setPressed(true);
            if (haptic) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
    }
}
