package com.aaron.bluehour;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * A-Z fast-scroll rail with an iOS-style magnify: as you drag, the letters
 * bulge out around your finger — the one you're on is biggest and brightest,
 * neighbours taper off with a smooth cosine falloff — so you always see where
 * you are. The bulge overflows to the left over the list (the parent must have
 * clipChildren=false for that to show).
 */
public class AlphaRail extends View {

    public interface Listener {
        void onLetter(char letter);
    }

    private static final String LETTERS = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Listener listener;
    private int activeIndex = -1;
    private float touchY = -1f;      // current finger Y (-1 when not touching)
    private float bulge = 0f;        // 0 = flat, 1 = full magnify (animated)
    private ValueAnimator bulgeAnim;

    public AlphaRail(Context c) {
        super(c);
        paint.setTypeface(Ui.tfUpright(c));
        paint.setTextAlign(Paint.Align.RIGHT);   // letters sit at the right edge
        setClickable(true);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    private void animateBulge(float target) {
        if (bulgeAnim != null) bulgeAnim.cancel();
        bulgeAnim = ValueAnimator.ofFloat(bulge, target);
        bulgeAnim.setDuration(target > 0f ? 130 : 200);
        bulgeAnim.setInterpolator(new DecelerateInterpolator());
        bulgeAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                bulge = ((Float) a.getAnimatedValue()).floatValue();
                invalidate();
            }
        });
        bulgeAnim.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                animateBulge(1f);
                // fall through to position handling
            case MotionEvent.ACTION_MOVE: {
                int h = getHeight();
                if (h == 0) return true;
                touchY = e.getY();
                int i = (int) (touchY / h * LETTERS.length());
                if (i < 0) i = 0;
                if (i >= LETTERS.length()) i = LETTERS.length() - 1;
                if (i != activeIndex) {
                    activeIndex = i;
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    if (listener != null) listener.onLetter(LETTERS.charAt(i));
                }
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activeIndex = -1;
                animateBulge(0f);      // collapse; touchY stays for the animation
                return true;
        }
        return super.onTouchEvent(e);
    }

    private static int lerpColor(int a, int b, float t) {
        int aa = (a >>> 24), ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24), br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24)
                | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8)
                | (int) (ab + (bb - ab) * t);
    }

    @Override
    protected void onDraw(Canvas c) {
        Context ctx = getContext();
        int h = getHeight();
        float rightX = getWidth() - Ui.dp(ctx, 8);
        float step = (float) h / LETTERS.length();
        float base = Ui.dp(ctx, 12);
        float maxAdd = Ui.dp(ctx, 24);       // ~36dp at the finger (bigger)
        float reach = step * 3.6f;           // how many letters the wave spans
        float leftShift = Ui.dp(ctx, 58);    // push the bulge well left of the thumb
        float spread = Ui.dp(ctx, 12);       // push neighbours apart for room
        int dim = Theme.get().shapeStyle == 4 ? 0x66EFE7DA : 0x8A06080C;
        int hot = Theme.get().accent;

        for (int i = 0; i < LETTERS.length(); i++) {
            float slotCy = step * i + step * 0.5f;
            float f = 0f;                    // 0..1 magnify amount for this letter
            float away = 0f;                 // vertical push away from the finger
            if (touchY >= 0f && bulge > 0f) {
                float dd = slotCy - touchY;
                float d = Math.abs(dd);
                if (d < reach) {
                    f = 0.5f * (1f + (float) Math.cos(Math.PI * d / reach)) * bulge;
                    away = (dd >= 0f ? 1f : -1f) * spread * f;
                }
            }
            float size = base + maxAdd * f;
            paint.setTextSize(size);
            paint.setColor(lerpColor(dim, hot, f));
            // magnified letters get a soft shadow so they read over the list rows
            if (f > 0.05f) {
                paint.setShadowLayer(Ui.dp(ctx, 5) * f, 0, Ui.dp(ctx, 1.5f), 0x73000000);
            } else {
                paint.clearShadowLayer();
            }
            float x = rightX - leftShift * f;
            // centre each letter on its slot as it grows, plus the spread
            float y = slotCy + away + size * 0.35f;
            c.drawText(String.valueOf(LETTERS.charAt(i)), x, y, paint);
        }
    }
}
