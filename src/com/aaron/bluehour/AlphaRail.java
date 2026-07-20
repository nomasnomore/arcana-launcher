package com.aaron.bluehour;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/** A-Z fast-scroll rail for the drawer list. */
public class AlphaRail extends View {

    public interface Listener {
        void onLetter(char letter);
    }

    private static final String LETTERS = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Listener listener;
    private int activeIndex = -1;

    public AlphaRail(Context c) {
        super(c);
        paint.setTypeface(Ui.tfUpright(c));
        paint.setTextSize(Ui.dp(c, 10));
        paint.setTextAlign(Paint.Align.CENTER);
        setClickable(true);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                int h = getHeight();
                if (h == 0) return true;
                int i = (int) (e.getY() / h * LETTERS.length());
                if (i < 0) i = 0;
                if (i >= LETTERS.length()) i = LETTERS.length() - 1;
                if (i != activeIndex) {
                    activeIndex = i;
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    if (listener != null) listener.onLetter(LETTERS.charAt(i));
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activeIndex = -1;
                invalidate();
                return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    protected void onDraw(Canvas c) {
        int h = getHeight();
        float cx = getWidth() / 2f;
        float step = (float) h / LETTERS.length();
        for (int i = 0; i < LETTERS.length(); i++) {
            boolean active = i == activeIndex;
            paint.setColor(active ? Theme.get().accent : 0x8A06080C);
            paint.setTextSize(Ui.dp(getContext(), active ? 13 : 10));
            float y = step * i + step * 0.75f;
            c.drawText(String.valueOf(LETTERS.charAt(i)), cx, y, paint);
        }
    }
}
