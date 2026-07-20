package com.aaron.bluehour;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.view.View;

/**
 * The big clock, drawn by hand. TextView clips italic ink to the advance
 * box; Barlow Black Italic's ink overhangs its advance on BOTH sides, so
 * digits got sheared. We measure the true ink rectangle (getTextBounds)
 * and size the view to it plus a fat symmetric pad — no bearing can reach
 * the edge.
 */
public class ClockView extends View {

    private String text = "";
    private final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Rect bounds = new Rect();
    private final float pad;

    public ClockView(Context c) {
        super(c);
        paint.setTypeface(Ui.tf(c));
        paint.setTextSize(Ui.dp(c, 84));
        paint.setColor(0xFFF2F5FA);
        paint.setShadowLayer(Ui.dp(c, 7), 0, Ui.dp(c, 2), 0xAA000000);
        pad = Ui.dp(c, 22); // breathing room on every side, ink can't reach it
    }

    public void setTime(String t) {
        if (!t.equals(text)) {
            text = t;
            requestLayout();
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        paint.getTextBounds(text, 0, text.length(), bounds);
        // width from true ink extent (bounds.right can exceed advance for italics),
        // plus a fat extra margin on the right where we have open space
        float advance = paint.measureText(text);
        int w = (int) (Math.max(advance, bounds.right) + pad * 2);
        int h = (int) (bounds.height() + pad * 2);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas c) {
        // place the ink's own top-left at (pad, pad): bounds.left is negative
        // when ink spills left of the origin, so subtracting it pulls it in
        float x = pad - bounds.left;
        float y = pad - bounds.top;
        if (Theme.get().ransom) {
            drawRansom(c, x, y);
        } else {
            c.drawText(text, x, y, paint);
        }
    }

    /** P5 clock: mild per-digit tilt/scale, NO boxes so the time stays clear. */
    private void drawRansom(Canvas c, float startX, float baseY) {
        float cx = startX;
        int base = text.hashCode();
        for (int i = 0; i < text.length(); i++) {
            String ch = text.substring(i, i + 1);
            float cw = paint.measureText(ch);
            if (!ch.equals(" ")) {
                int s = (base * 31 + i * 2654435) & 0x7FFFFFFF;
                float ang = ((((s % 1000) / 1000f) - 0.5f) * 2f) * 5f; // ±5°
                float sc = 0.92f + ((s >> 8) % 1000) / 1000f * 0.16f;   // 0.92–1.08
                float dy = ((((s >> 16) % 1000) / 1000f) - 0.5f) * 2f
                        * Ui.dp(getContext(), 5);
                c.save();
                c.translate(cx + cw / 2f, baseY + dy);
                c.rotate(ang);
                c.scale(sc, sc);
                c.drawText(ch, -cw / 2f, 0, paint);
                c.restore();
            }
            cx += cw;
        }
    }
}
