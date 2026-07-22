package com.aaron.bluehour;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/**
 * Ivory Hour's illuminated-manuscript frame: a thin gilt double-rule border
 * inset from the screen edges, with an ornamental scroll flourish in each
 * corner, plus a whisper-faint warm vignette. Baked into a bitmap once
 * (static, cheap) and only shown in Ivory Hour. Touch-transparent, so the
 * user's wallpaper reads straight through the open center.
 */
public class FiligreeView extends View {

    private Bitmap tex;
    private final Paint bmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private boolean on = false;
    // safe-area insets so the border sits inside the status/nav bars
    private int insTop = 0, insBottom = 0, insLeft = 0, insRight = 0;

    public FiligreeView(Context c) {
        super(c);
        setClickable(false);
        setFocusable(false);
    }

    /** Keep the gilt frame clear of the status bar / gesture area. */
    public void setInsets(int top, int bottom, int left, int right) {
        if (top == insTop && bottom == insBottom
                && left == insLeft && right == insRight) return;
        insTop = top; insBottom = bottom; insLeft = left; insRight = right;
        if (on && getWidth() > 0) build();
        invalidate();
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
        Context ctx = getContext();

        int gold = Theme.get().filigree;

        // whisper-faint warm vignette so the frame feels lit, not pasted on
        Paint vig = new Paint(Paint.ANTI_ALIAS_FLAG);
        vig.setShader(new RadialGradient(w / 2f, h / 2f,
                (float) Math.hypot(w, h) / 2f,
                new int[]{0x00000000, 0x00000000, 0x1AC79A3B},
                new float[]{0f, 0.68f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, vig);

        float side = Ui.dp(ctx, 16);  // margin from the left/right edges
        float vert = Ui.dp(ctx, 12);  // extra margin beyond the safe-area insets
        float gap = Ui.dp(ctx, 44);   // corner flourish spans this (bigger: sole ornament now)
        float d = Ui.dp(ctx, 3.6f);   // inner-rule offset

        Paint rule = new Paint(Paint.ANTI_ALIAS_FLAG);
        rule.setStyle(Paint.Style.STROKE);
        rule.setColor(gold);
        rule.setStrokeWidth(Ui.dp(ctx, 1.7f));
        Paint thin = new Paint(Paint.ANTI_ALIAS_FLAG);
        thin.setStyle(Paint.Style.STROKE);
        thin.setColor(gold);
        thin.setStrokeWidth(Ui.dp(ctx, 0.9f));
        thin.setAlpha(0xCC);
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(gold);

        float l = insLeft + side, t = insTop + vert;
        float r = w - insRight - side, b = h - insBottom - vert;

        // corners only — no caged edge rules; the wallpaper breathes to the edge.
        // four corner flourishes (same template, rotated into each corner)
        drawCorner(c, ctx, rule, thin, fill, l, t, 0f, gap, d);
        drawCorner(c, ctx, rule, thin, fill, r, t, 90f, gap, d);
        drawCorner(c, ctx, rule, thin, fill, r, b, 180f, gap, d);
        drawCorner(c, ctx, rule, thin, fill, l, b, 270f, gap, d);
    }

    /**
     * Draws one corner scroll in local coords (corner at origin, screen
     * interior toward +x/+y) then rotates it into place.
     */
    private void drawCorner(Canvas c, Context ctx, Paint rule, Paint thin,
                            Paint fill, float cx, float cy, float deg,
                            float g, float d) {
        c.save();
        c.translate(cx, cy);
        c.rotate(deg);

        // rounded elbow joining the two straight rules (double line)
        Path p = new Path();
        p.moveTo(g, 0);
        p.quadTo(0, 0, 0, g);
        c.drawPath(p, rule);
        Path p2 = new Path();
        p2.moveTo(g, d);
        p2.quadTo(d, d, d, g);
        c.drawPath(p2, thin);

        // small curling flourishes tapering off each arm end (so the corner
        // reads as an ornament, not a cut-off frame)
        float e = Ui.dp(ctx, 1f);
        Path h1 = new Path();
        h1.moveTo(g, 0);
        h1.cubicTo(g + e * 8, 0, g + e * 10, e * 5, g + e * 4, e * 7);
        c.drawPath(h1, thin);
        Path h2 = new Path();
        h2.moveTo(0, g);
        h2.cubicTo(0, g + e * 8, e * 5, g + e * 10, e * 7, g + e * 4);
        c.drawPath(h2, thin);
        // terminal buds where the elbow meets the arms
        c.drawCircle(g, 0, Ui.dp(ctx, 2.3f), fill);
        c.drawCircle(0, g, Ui.dp(ctx, 2.3f), fill);

        // an inward scroll curl riding the diagonal
        float m = g * 0.52f;
        Path curl = new Path();
        curl.moveTo(m * 0.4f, m * 0.4f);
        curl.cubicTo(m * 1.15f, m * 0.30f, m * 1.25f, m * 1.15f,
                m * 0.55f, m * 1.25f);
        curl.cubicTo(m * 0.10f, m * 1.30f, m * 0.20f, m * 0.85f,
                m * 0.62f, m * 0.86f);
        c.drawPath(curl, thin);

        // a small gilt diamond accent on the diagonal
        c.save();
        c.translate(m * 0.72f, m * 0.72f);
        c.rotate(45f);
        float s = Ui.dp(ctx, 3.4f);
        c.drawRect(-s, -s, s, s, fill);
        c.restore();

        c.restore();
    }

    @Override
    protected void onDraw(Canvas c) {
        if (on && tex != null) c.drawBitmap(tex, 0, 0, bmpPaint);
    }
}
