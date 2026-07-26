package com.aaron.bluehour;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * The corner MAIL indicator, drawn flat and simple in the game's style: a solid
 * themed disc with a thin dark outline, a clean white envelope, and — when
 * notifications are waiting — a small count badge and a subtle outer-ring pulse.
 * No gloss, no 3D. Recolours with the current Hour.
 */
public class MailButton extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int count = 0;
    private float pulse = 0f;
    private ValueAnimator pulseAnim;

    public MailButton(Context c) {
        super(c);
        setClickable(true);
    }

    public void setCount(int n) {
        if (n == count) return;
        count = n;
        if (count > 0) startPulse(); else stopPulse();
        invalidate();
    }

    private void startPulse() {
        if (pulseAnim != null) return;
        pulseAnim = ValueAnimator.ofFloat(0f, 1f);
        pulseAnim.setDuration(1600);
        pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnim.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnim.setInterpolator(new LinearInterpolator());
        pulseAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                pulse = ((Float) a.getAnimatedValue()).floatValue();
                invalidate();
            }
        });
        pulseAnim.start();
    }

    private void stopPulse() {
        if (pulseAnim != null) { pulseAnim.cancel(); pulseAnim = null; }
        pulse = 0f;
    }

    @Override
    protected void onDetachedFromWindow() {
        stopPulse();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int side = (int) Ui.dp(getContext(), 38);
        setMeasuredDimension(side, side);
    }

    @Override
    protected void onDraw(Canvas c) {
        Context ctx = getContext();
        Theme t = Theme.get();
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float pad = Ui.dp(ctx, 3);
        float r = Math.min(w, h) / 2f - pad;
        int outline = darken(t.accent, 0.40f);
        boolean pressed = isPressed();

        // ---- subtle outer-ring pulse when unread (flat, no blur) ----
        if (count > 0) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Ui.dp(ctx, 2));
            paint.setColor(withAlpha(t.accentBright, (int) (150 * (0.15f + 0.85f * pulse))));
            c.drawCircle(cx, cy, r + Ui.dp(ctx, 1.5f) + Ui.dp(ctx, 2) * pulse, paint);
        }

        // ---- flat disc + outline ----
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(pressed ? t.accentBright : t.accent);
        c.drawCircle(cx, cy, r, paint);
        paint.setStyle(Paint.Style.STROKE);
        float ow = Ui.dp(ctx, 2.4f);
        paint.setStrokeWidth(ow);
        paint.setColor(outline);
        c.drawCircle(cx, cy, r - ow / 2f, paint);

        // ---- flat white envelope ----
        float ew = r * 1.15f, eh = ew * 0.68f;
        float ex = cx - ew / 2f, ey = cy - eh / 2f;
        RectF body = new RectF(ex, ey, ex + ew, ey + eh);
        float rad = Ui.dp(ctx, 2);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFFFFFF);
        c.drawRoundRect(body, rad, rad, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Ui.dp(ctx, 2.2f));
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(outline);
        c.drawRoundRect(body, rad, rad, paint);
        // flap: a clean inverted-V from the top corners to the middle
        Path flap = new Path();
        flap.moveTo(ex + Ui.dp(ctx, 1), ey + Ui.dp(ctx, 1.5f));
        flap.lineTo(cx, cy + eh * 0.06f);
        flap.lineTo(ex + ew - Ui.dp(ctx, 1), ey + Ui.dp(ctx, 1.5f));
        c.drawPath(flap, paint);

        // ---- flat count badge (bottom-right, like the game's "!" tab) ----
        if (count > 0) {
            float br = Ui.dp(ctx, 8.5f);
            float bx = cx + r * 0.62f;
            float by = cy + r * 0.62f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(t.pop);
            c.drawCircle(bx, by, br, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Ui.dp(ctx, 1.6f));
            paint.setColor(darken(t.pop, 0.45f));
            c.drawCircle(bx, by, br - Ui.dp(ctx, 0.8f), paint);
            String label = count > 9 ? "9+" : String.valueOf(count);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFFFFFFF);
            paint.setTypeface(Ui.tfUpright(ctx));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(br * (count > 9 ? 1.05f : 1.35f));
            Paint.FontMetrics fm = paint.getFontMetrics();
            float ty = by - (fm.ascent + fm.descent) / 2f;
            c.drawText(label, bx, ty, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    private static int withAlpha(int color, int a) {
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static int darken(int c, float f) {
        int r = (int) (((c >> 16) & 0xFF) * f);
        int g = (int) (((c >> 8) & 0xFF) * f);
        int b = (int) ((c & 0xFF) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
