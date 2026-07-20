package com.aaron.bluehour;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

/**
 * v2: the focus band sits at 40% of the list height — whichever word is
 * there gets the red/burst treatment (setFx). Rows keep a mild diagonal
 * cascade and fade at the extremes; rows below the band tint cyan.
 */
public class AppsListView extends ListView {

    private final float slant;
    private final float rowH;

    public AppsListView(Context c) {
        super(c);
        slant = Ui.dp(c, 14);
        rowH = Ui.dp(c, 56);
        setDivider(null);
        setDividerHeight(0);
        setSelector(new ColorDrawable(0));
        setVerticalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setCacheColorHint(0);
        setPadding(Ui.dpi(c, 30), Ui.dpi(c, 8), Ui.dpi(c, 18), Ui.dpi(c, 20));
        setClipToPadding(false);
        setClipChildren(false);
        setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView v, int s) {
            }

            @Override
            public void onScroll(AbsListView v, int first, int visible, int total) {
                applyFx();
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        applyFx();
    }

    private int lastFocusPos = -1;

    /** Call when list contents change so refreshes don't fire phantom haptics. */
    public void resetFocus() {
        lastFocusPos = -1;
    }

    void applyFx() {
        int h = getHeight();
        if (h == 0) return;
        float ch = h / 2f;
        float focusY = h * 0.40f;
        int bestPos = -1;
        float bestF = 0.55f;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            float cc = (v.getTop() + v.getBottom()) / 2f;
            float t = (cc - ch) / ch; // -1 .. 1
            v.setTranslationX(-t * slant);
            float at = Math.abs(t);
            float alpha = 1f - Math.max(0f, at - 0.78f) * 3.2f;
            v.setAlpha(Math.max(0.05f, alpha));

            if (v instanceof SlantRowView) {
                float d = Math.abs(cc - focusY);
                float f = Math.max(0f, 1f - d / (rowH * 0.85f));
                f = f * f * (3f - 2f * f); // smoothstep
                float b = cc > focusY
                        ? Math.min(1f, (cc - focusY) / ch) * 0.85f : 0f;
                ((SlantRowView) v).setFx(f, b);
                if (f > bestF) {
                    bestF = f;
                    bestPos = getFirstVisiblePosition() + i;
                }
            }
        }
        // haptic tick as the focus bar lands on a new row while scrolling
        if (bestPos >= 0 && bestPos != lastFocusPos) {
            if (lastFocusPos >= 0) {
                performHapticFeedback(
                        android.view.HapticFeedbackConstants.CLOCK_TICK);
            }
            lastFocusPos = bestPos;
        }
    }
}
