package com.aaron.bluehour;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * The left-side "feed" button: a speech bubble with a "…" that shape-shifts
 * to match each Hour — sharp electric (P3), rounded bubbly (P4), jagged comic
 * (P5), and painterly (Ivory). Tap opens your feed; long-press rebinds it.
 */
public class BulletinView extends View {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final android.text.TextPaint tp =
            new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Path bubble = new Path();
    private final android.graphics.Matrix flipM = new android.graphics.Matrix();
    private boolean mirrored = false;
    private float press = 0f;
    private ValueAnimator anim;

    public BulletinView(Context c) {
        super(c);
        setClickable(true);
        setLongClickable(true);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        tp.setTypeface(Ui.tfUpright(c));
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setLetterSpacing(0.02f);
    }

    /** Flip the bubble horizontally (tail to the other side). */
    public void setMirrored(boolean m) {
        if (mirrored != m) { mirrored = m; invalidate(); }
    }

    @Override
    public void setPressed(boolean p) {
        boolean was = isPressed();
        super.setPressed(p);
        if (was == p) return;
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(press, p ? 1f : 0f);
        anim.setDuration(p ? 90 : 240);
        anim.setInterpolator(new OvershootInterpolator(2f));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                press = ((Float) a.getAnimatedValue()).floatValue();
                float s = 1f + press * 0.14f;
                setScaleX(s); setScaleY(s);
                invalidate();
            }
        });
        anim.start();
    }

    private static float jt(int seed, float amp) {
        if (amp <= 0) return 0f;
        int h = (seed * 1103515245 + 12345) & 0x7FFFFFFF;
        return ((h % 1000) / 1000f - 0.5f) * 2f * amp;
    }

    @Override
    protected void onDraw(Canvas c) {
        Context ctx = getContext();
        Theme t = Theme.get();
        int ss = t.shapeStyle;
        float w = getWidth(), h = getHeight();

        int fillC = ss == 3 ? 0xFFF3EAD4 : 0xFFF8F8FB;      // cream in Ivory, else white
        int outlineC = ss == 0 ? t.accentBright : t.accent; // P3 cyan, else accent (P5 red, P4 gold…)
        float ow = ss == 2 ? Ui.dp(ctx, 3.2f) : Ui.dp(ctx, 2.2f);
        float rad = ss == 1 ? Ui.dp(ctx, 8) : (ss == 3 ? Ui.dp(ctx, 6)
                : (ss == 0 ? Ui.dp(ctx, 1.5f) : 0f));
        float jit = ss == 2 ? Ui.dp(ctx, 2.2f) : 0f;

        float pad = ow + Ui.dp(ctx, 2);
        float bl = pad, bt = pad, br = w - pad, bb = h * 0.70f;
        buildBubble(bl, bt, br, bb, rad, jit);

        fill.setColor(fillC);
        fill.setShadowLayer(Ui.dp(ctx, 4), 0, Ui.dp(ctx, 1.5f), 0x80000000);
        c.drawPath(bubble, fill);
        fill.clearShadowLayer();
        stroke.setColor(outlineC);
        stroke.setStrokeWidth(ow);
        c.drawPath(bubble, stroke);

        // "NEWS" label (never mirrored — stays readable)
        tp.setColor(ss == 2 ? 0xFF141414 : 0xFF1B1E24);
        tp.setTextSize(Ui.dp(ctx, 10.5f));
        float target = (br - bl) - Ui.dp(ctx, 7);
        while (tp.measureText("NEWS") > target
                && tp.getTextSize() > Ui.dp(ctx, 6.5f)) {
            tp.setTextSize(tp.getTextSize() - 1f);
        }
        float cy = (bt + bb) / 2f;
        Paint.FontMetrics fm = tp.getFontMetrics();
        float baseline = cy - (fm.ascent + fm.descent) / 2f;
        c.drawText("NEWS", (bl + br) / 2f, baseline, tp);
    }

    /** Speech-bubble outline + bottom-left tail; rad rounds corners, jit roughens. */
    private void buildBubble(float bl, float bt, float br, float bb,
                             float rad, float jit) {
        Context ctx = getContext();
        float tx1 = bl + (br - bl) * 0.44f;   // tail base (right)
        float tx2 = bl + (br - bl) * 0.24f;   // tail base (left)
        float ttip = bl + (br - bl) * 0.14f;  // tail tip
        float ty = getHeight() - Ui.dp(ctx, 2.5f);
        bubble.reset();
        bubble.moveTo(bl + rad + jt(1, jit), bt + jt(2, jit));
        bubble.lineTo(br - rad + jt(3, jit), bt + jt(4, jit));
        if (rad > 0) bubble.quadTo(br, bt, br, bt + rad);
        else bubble.lineTo(br + jt(5, jit), bt + jt(6, jit));
        bubble.lineTo(br + jt(7, jit), bb - rad + jt(8, jit));
        if (rad > 0) bubble.quadTo(br, bb, br - rad, bb);
        else bubble.lineTo(br + jt(9, jit), bb + jt(10, jit));
        // bottom edge with the tail
        bubble.lineTo(tx1 + jt(11, jit), bb + jt(12, jit));
        bubble.lineTo(ttip, ty);
        bubble.lineTo(tx2 + jt(13, jit), bb + jt(14, jit));
        bubble.lineTo(bl + rad + jt(15, jit), bb + jt(16, jit));
        if (rad > 0) bubble.quadTo(bl, bb, bl, bb - rad);
        else bubble.lineTo(bl + jt(17, jit), bb + jt(18, jit));
        bubble.lineTo(bl + jt(19, jit), bt + rad + jt(20, jit));
        if (rad > 0) bubble.quadTo(bl, bt, bl + rad, bt);
        else bubble.lineTo(bl + jt(21, jit), bt + jt(22, jit));
        bubble.close();
        if (mirrored) {
            flipM.setScale(-1f, 1f, (bl + br) / 2f, 0f);
            bubble.transform(flipM);
        }
    }
}
