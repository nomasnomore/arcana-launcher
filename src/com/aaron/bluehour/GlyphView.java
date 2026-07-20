package com.aaron.bluehour;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * Dock tile from the mock: skewed black tile with a white slash accent,
 * flat white glyph inside, bilingual label underneath (PHONE / 電話).
 */
public class GlyphView extends View {

    public static final int PHONE = 0;
    public static final int MSG = 1;
    public static final int PLAY = 2;
    public static final int WEB = 3;
    public static final int CAM = 4;

    private static final int TILE_BG = 0xF00A0E16;

    private final int kind;             // -2 = bitmap mode
    private final String label;
    private final String jp;
    private final android.graphics.Bitmap appIcon;
    private final Paint iconPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    {
        // desaturate custom app icons so the dock stays monochrome
        android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();
        cm.setSaturation(0f);
        iconPaint.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm));
    }
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean dot = false;
    private float press = 0f;
    private ValueAnimator anim;

    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tileStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hole = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint jpPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path p = new Path();

    public GlyphView(Context c, int glyphKind, String labelText, String jpText) {
        this(c, glyphKind, labelText, jpText, null);
    }

    /** Bitmap mode: a user-picked app in the slot. */
    public GlyphView(Context c, android.graphics.Bitmap icon, String labelText) {
        this(c, -2, labelText, "", icon);
    }

    private GlyphView(Context c, int glyphKind, String labelText, String jpText,
                      android.graphics.Bitmap icon) {
        super(c);
        kind = glyphKind;
        label = labelText;
        jp = jpText;
        appIcon = icon;
        tilePaint.setColor(TILE_BG);
        tileStroke.setStyle(Paint.Style.STROKE);
        tileStroke.setStrokeWidth(Ui.dp(c, 1.5f));
        tileStroke.setColor(0x40FFFFFF);
        slashPaint.setColor(0xFFFFFFFF);
        fill.setColor(0xFFF2F5FA);
        stroke.setColor(0xFFF2F5FA);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Ui.dp(c, 2.6f));
        stroke.setStrokeCap(Paint.Cap.ROUND);
        hole.setColor(0xFF0A0E16);
        labelPaint.setTypeface(Ui.tfUpright(c));
        labelPaint.setTextSize(Ui.dp(c, 11));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        jpPaint.setTypeface(Ui.tfUpright(c));
        jpPaint.setTextSize(Ui.dp(c, 9.5f));
        jpPaint.setTextAlign(Paint.Align.CENTER);
        dotPaint.setColor(Theme.get().pop);
        setClickable(true);
    }

    public void setDot(boolean d) {
        if (dot != d) {
            dot = d;
            invalidate();
        }
    }

    @Override
    public void setPressed(boolean pressed) {
        boolean was = isPressed();
        super.setPressed(pressed);
        if (was == pressed) return;
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(press, pressed ? 1f : 0f);
        anim.setDuration(pressed ? 80 : 260);
        anim.setInterpolator(new OvershootInterpolator(2.2f));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                press = ((Float) a.getAnimatedValue()).floatValue();
                float s = 1f + press * 0.12f;
                setScaleX(s);
                setScaleY(s);
                invalidate();
            }
        });
        anim.start();
    }

    private static float jag(int seed, float amp) {
        int h = (seed * 1103515245 + 12345) & 0x7FFFFFFF;
        return ((h % 1000) / 1000f - 0.5f) * 2f * amp;
    }

    @Override
    protected void onDraw(Canvas c) {
        Context ctx = getContext();
        float w = getWidth();
        float tile = Ui.dp(ctx, 56);
        float skew = Ui.dp(ctx, 5);
        float tx0 = (w - tile) / 2f;
        float ty0 = Ui.dp(ctx, 2);

        // tile: jagged torn shape in P5, skewed elsewhere
        p.reset();
        if (Theme.get().shapeStyle == 2) {
            int sd = (label.hashCode() ^ 0x5A5A) & 0x7FFFFFFF;
            float jj = Ui.dp(ctx, 3f);
            int steps = 4;
            for (int i = 0; i <= steps; i++) {
                float x = tx0 + tile * i / steps;
                float y = ty0 + jag(sd + i, jj);
                if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            p.lineTo(tx0 + tile + jag(sd + 30, jj), ty0 + tile / 2f);
            for (int i = 0; i <= steps; i++) {
                float x = tx0 + tile - tile * i / steps;
                p.lineTo(x, ty0 + tile + jag(sd + 50 + i, jj));
            }
            p.lineTo(tx0 + jag(sd + 70, jj), ty0 + tile / 2f);
            p.close();
        } else {
            p.moveTo(tx0 + skew, ty0);
            p.lineTo(tx0 + tile, ty0);
            p.lineTo(tx0 + tile - skew, ty0 + tile);
            p.lineTo(tx0, ty0 + tile);
            p.close();
        }
        c.drawPath(p, tilePaint);
        tileStroke.setColor(press > 0.4f ? Theme.get().accentBright : 0x40FFFFFF);
        c.drawPath(p, tileStroke);

        // white slash accent across the top-left corner
        p.reset();
        p.moveTo(tx0 + skew - Ui.dp(ctx, 2), ty0);
        p.lineTo(tx0 + skew + Ui.dp(ctx, 12), ty0);
        p.lineTo(tx0 + Ui.dp(ctx, 4), ty0 + Ui.dp(ctx, 16));
        p.lineTo(tx0 - Ui.dp(ctx, 2), ty0 + Ui.dp(ctx, 10));
        p.close();
        c.drawPath(p, slashPaint);

        // glyph or app icon centered in the tile
        float cx = tx0 + tile / 2f;
        float cy = ty0 + tile / 2f;
        if (kind == -2) {
            if (appIcon != null) {
                float is = tile * 0.62f;
                r.set(cx - is / 2f, cy - is / 2f, cx + is / 2f, cy + is / 2f);
                c.drawBitmap(appIcon, null, r, iconPaint);
            }
        } else {
            drawGlyph(c, cx, cy, tile * 0.27f);
        }

        // notification dot on the tile's top-right corner
        if (dot) {
            c.drawCircle(tx0 + tile - Ui.dp(ctx, 4), ty0 + Ui.dp(ctx, 6),
                    Ui.dp(ctx, 5), dotPaint);
        }

        // labels
        float labelY = ty0 + tile + Ui.dp(ctx, 15);
        labelPaint.setColor(0xFFF2F5FA);
        labelPaint.setShadowLayer(Ui.dp(ctx, 3), 0, Ui.dp(ctx, 1), 0xB3000000);
        String lab = label;
        if (labelPaint.measureText(lab) > w + Ui.dp(ctx, 8)) {
            while (lab.length() > 2
                    && labelPaint.measureText(lab + "…") > w + Ui.dp(ctx, 8)) {
                lab = lab.substring(0, lab.length() - 1);
            }
            lab = lab + "…";
        }
        c.drawText(lab, w / 2f, labelY, labelPaint);
        if (jp.length() > 0) {
            jpPaint.setColor(0xCCBFE0FF);
            jpPaint.setShadowLayer(Ui.dp(ctx, 3), 0, Ui.dp(ctx, 1), 0x99000000);
            c.drawText(jp, w / 2f, labelY + Ui.dp(ctx, 13), jpPaint);
        }
    }

    private void drawGlyph(Canvas c, float cx, float cy, float s) {
        switch (kind) {
            case PHONE: {
                c.save();
                c.rotate(-40, cx, cy);
                r.set(cx - s, cy + s * 0.42f, cx + s, cy + s * 1.02f);
                c.drawArc(r, 15, 150, false, stroke);
                c.drawCircle(cx - s * 0.82f, cy + s * 0.42f, s * 0.30f, fill);
                c.drawCircle(cx + s * 0.82f, cy + s * 0.42f, s * 0.30f, fill);
                c.restore();
                break;
            }
            case MSG: {
                r.set(cx - s, cy - s * 0.78f, cx + s, cy + s * 0.55f);
                c.drawRoundRect(r, s * 0.32f, s * 0.32f, fill);
                p.reset();
                p.moveTo(cx - s * 0.45f, cy + s * 0.40f);
                p.lineTo(cx - s * 0.15f, cy + s * 0.98f);
                p.lineTo(cx + s * 0.12f, cy + s * 0.40f);
                p.close();
                c.drawPath(p, fill);
                break;
            }
            case PLAY: {
                r.set(cx - s * 1.08f, cy - s * 0.76f, cx + s * 1.08f, cy + s * 0.76f);
                c.drawRoundRect(r, s * 0.30f, s * 0.30f, fill);
                p.reset();
                p.moveTo(cx - s * 0.26f, cy - s * 0.36f);
                p.lineTo(cx + s * 0.44f, cy);
                p.lineTo(cx - s * 0.26f, cy + s * 0.36f);
                p.close();
                c.drawPath(p, hole);
                break;
            }
            case WEB: {
                c.drawCircle(cx, cy, s * 0.95f, stroke);
                r.set(cx - s * 0.42f, cy - s * 0.95f, cx + s * 0.42f, cy + s * 0.95f);
                c.drawOval(r, stroke);
                c.drawLine(cx - s * 0.95f, cy, cx + s * 0.95f, cy, stroke);
                break;
            }
            case CAM: {
                r.set(cx - s * 1.05f, cy - s * 0.60f, cx + s * 1.05f, cy + s * 0.72f);
                c.drawRoundRect(r, s * 0.22f, s * 0.22f, fill);
                r.set(cx - s * 0.34f, cy - s * 0.88f, cx + s * 0.34f, cy - s * 0.50f);
                c.drawRoundRect(r, s * 0.10f, s * 0.10f, fill);
                c.drawCircle(cx, cy + s * 0.06f, s * 0.34f, hole);
                c.drawCircle(cx, cy + s * 0.06f, s * 0.16f, fill);
                break;
            }
        }
    }
}
