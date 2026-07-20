package com.aaron.bluehour;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 * P5 comic-print texture: black halftone dots pooling in the top-right and
 * bottom-left corners and fading toward center, with a few scattered stars
 * mixed in. Baked into a bitmap once (static, cheap) and only shown in Red
 * Hour. Touch-transparent.
 */
public class HalftoneView extends View {

    private Bitmap tex;
    private final Paint bmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private boolean on = false;

    public HalftoneView(Context c) {
        super(c);
        setClickable(false);
        setFocusable(false);
    }

    public void setOn(boolean enable) {
        on = enable;
        setVisibility(enable ? VISIBLE : GONE);
        if (enable && tex == null && getWidth() > 0) build();
        invalidate();
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent e) {
        return false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (on && w > 0 && h > 0) build();
    }

    private void build() {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        if (tex != null) tex.recycle();
        tex = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(tex);
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot.setColor(0xFF0A0505);
        float step = Ui.dp(getContext(), 12);
        float maxD = (float) Math.hypot(w, h);
        // two corner anchors: top-right, bottom-left
        float[][] anchors = {{w, 0}, {0, h}};
        for (float[] an : anchors) {
            for (float y = 0; y < h; y += step) {
                for (float x = 0; x < w; x += step) {
                    float d = (float) Math.hypot(x - an[0], y - an[1]);
                    // tighter reach: dots fade out much closer to the corner
                    float t = 1f - d / (maxD * 0.30f);
                    if (t <= 0) continue;
                    float r = t * t * (step * 0.42f); // bigger dots near corner
                    if (r < 0.6f) continue;
                    c.drawCircle(x, y, r, dot);
                }
            }
        }
        // a few scattered stars (deterministic)
        Paint star = new Paint(Paint.ANTI_ALIAS_FLAG);
        int seed = 998877;
        for (int i = 0; i < 7; i++) {
            seed = seed * 1103515245 + 12345;
            float sx = ((seed >>> 8) % 1000) / 1000f * w;
            seed = seed * 1103515245 + 12345;
            float sy = ((seed >>> 8) % 1000) / 1000f * h;
            // keep stars near the corners where halftone lives
            float dTR = (float) Math.hypot(sx - w, sy);
            float dBL = (float) Math.hypot(sx, sy - h);
            if (Math.min(dTR, dBL) > maxD * 0.34f) continue;
            seed = seed * 1103515245 + 12345;
            float sr = Ui.dp(getContext(), 6 + ((seed >>> 8) % 10));
            star.setColor((i % 2 == 0) ? 0xFFE60012 : 0xFF0A0505);
            drawStar(c, star, sx, sy, sr, sr * 0.44f);
        }
    }

    private void drawStar(Canvas c, Paint p, float cx, float cy,
                          float outer, float inner) {
        android.graphics.Path s = new android.graphics.Path();
        for (int i = 0; i < 10; i++) {
            float r = (i % 2 == 0) ? outer : inner;
            double a = -Math.PI / 2 + i * Math.PI / 5;
            float x = cx + (float) Math.cos(a) * r;
            float y = cy + (float) Math.sin(a) * r;
            if (i == 0) s.moveTo(x, y); else s.lineTo(x, y);
        }
        s.close();
        c.drawPath(s, p);
    }

    @Override
    protected void onDraw(Canvas c) {
        if (on && tex != null) c.drawBitmap(tex, 0, 0, bmpPaint);
    }
}
