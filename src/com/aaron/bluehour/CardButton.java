package com.aaron.bluehour;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * The wallet card — a small face-down card in the corner of the home
 * screen; hold it to open your payment app. Ships with a generic
 * gem/coin face drawn in the launcher's style; if the user provides
 * their own image it becomes the card face (full bleed).
 */
public class CardButton extends View {

    private Bitmap custom;
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gemPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint facetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bmpPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF card = new RectF();
    private final Path p = new Path();
    private float press = 0f;
    private ValueAnimator anim;

    public CardButton(Context c) {
        super(c);
        cardPaint.setColor(0xFF23262B);           // charcoal card field
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(Ui.dp(c, 2.2f));
        borderPaint.setColor(0xFFC9D2DC);         // silver outer frame
        gemPaint.setColor(0xFFE2E7EC);            // light half of the mask
        facetPaint.setStyle(Paint.Style.STROKE);
        facetPaint.setStrokeWidth(Ui.dp(c, 1.1f));
        facetPaint.setColor(0xFF9AA2AB);          // inner frame silver
        checkPaint.setColor(0x1AFFFFFF);          // diamond field
        setClickable(true);
        setLongClickable(true);
        setRotation(6f); // tilted toward the screen edge
    }

    public void setCustomIcon(Bitmap b) {
        custom = b;
        invalidate();
    }

    @Override
    public void setPressed(boolean pressed) {
        boolean was = isPressed();
        super.setPressed(pressed);
        if (was == pressed) return;
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(press, pressed ? 1f : 0f);
        anim.setDuration(pressed ? 120 : 280);
        anim.setInterpolator(new OvershootInterpolator(2f));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                press = ((Float) a.getAnimatedValue()).floatValue();
                setScaleX(1f + press * 0.14f);
                setScaleY(1f + press * 0.14f);
                setRotation(6f - press * 4f);
                invalidate();
            }
        });
        anim.start();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension(Ui.dpi(getContext(), 42), Ui.dpi(getContext(), 58));
    }

    @Override
    protected void onDraw(Canvas c) {
        Context ctx = getContext();
        float inset = Ui.dp(ctx, 2.5f);
        card.set(inset, inset, getWidth() - inset, getHeight() - inset);
        float r = Ui.dp(ctx, 6);

        if (custom != null) {
            // user image as the full card face, clipped to the card shape
            p.reset();
            p.addRoundRect(card, r, r, Path.Direction.CW);
            c.save();
            c.clipPath(p);
            Matrix m = new Matrix();
            float scale = Math.max(card.width() / custom.getWidth(),
                    card.height() / custom.getHeight());
            m.setScale(scale, scale);
            m.postTranslate(card.left + (card.width() - custom.getWidth() * scale) / 2f,
                    card.top + (card.height() - custom.getHeight() * scale) / 2f);
            BitmapShader sh = new BitmapShader(custom,
                    Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            sh.setLocalMatrix(m);
            bmpPaint.setShader(sh);
            c.drawRoundRect(card, r, r, bmpPaint);
            c.restore();
            c.drawRoundRect(card, r, r, borderPaint);
            return;
        }

        // generic face: silver-on-charcoal card back — diamond field,
        // double frame with corner marks, split sun/moon mask medallion
        c.drawRoundRect(card, r, r, cardPaint);
        float step = Ui.dp(ctx, 8);
        c.save();
        p.reset();
        p.addRoundRect(card, r, r, Path.Direction.CW);
        c.clipPath(p);
        for (float y = card.top; y < card.bottom + step; y += step) {
            for (float x = card.left + ((int) ((y - card.top) / step) % 2) * step / 2f;
                 x < card.right + step; x += step) {
                p.reset();
                p.moveTo(x, y - step * 0.34f);
                p.lineTo(x + step * 0.34f, y);
                p.lineTo(x, y + step * 0.34f);
                p.lineTo(x - step * 0.34f, y);
                p.close();
                c.drawPath(p, checkPaint);
            }
        }
        c.restore();

        // inner frame + corner marks
        float in = Ui.dp(ctx, 4.5f);
        RectF inner = new RectF(card.left + in, card.top + in,
                card.right - in, card.bottom - in);
        c.drawRect(inner, facetPaint);
        float cm = Ui.dp(ctx, 4);
        Paint corner = new Paint(Paint.ANTI_ALIAS_FLAG);
        corner.setColor(0xFFC9D2DC);
        float[][] corners = {{inner.left, inner.top}, {inner.right, inner.top},
                {inner.left, inner.bottom}, {inner.right, inner.bottom}};
        for (float[] pt : corners) {
            p.reset();
            p.moveTo(pt[0], pt[1] - cm);
            p.lineTo(pt[0] + cm, pt[1]);
            p.lineTo(pt[0], pt[1] + cm);
            p.lineTo(pt[0] - cm, pt[1]);
            p.close();
            c.drawPath(p, corner);
        }

        // mask medallion: radiating spikes, half light / half dark
        float cx = card.centerX();
        float cy = card.centerY();
        float g = Ui.dp(ctx, 11);
        float spike = Ui.dp(ctx, 4.5f);
        Paint darkP = new Paint(Paint.ANTI_ALIAS_FLAG);
        darkP.setColor(0xFF101215);
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8;
            float ex = (float) Math.cos(a);
            float ey = (float) Math.sin(a);
            Paint sp = ex < 0 ? gemPaint : darkP;
            p.reset();
            p.moveTo(cx + ex * g, cy + ey * g);
            double a1 = a - 0.13, a2 = a + 0.13;
            p.moveTo(cx + (float) Math.cos(a1) * g, cy + (float) Math.sin(a1) * g);
            p.lineTo(cx + ex * (g + spike), cy + ey * (g + spike));
            p.lineTo(cx + (float) Math.cos(a2) * g, cy + (float) Math.sin(a2) * g);
            p.close();
            c.drawPath(p, sp);
        }
        // face disc: left half light, right half dark
        c.save();
        c.clipRect(cx - g, cy - g, cx, cy + g);
        c.drawCircle(cx, cy, g, gemPaint);
        c.restore();
        c.save();
        c.clipRect(cx, cy - g, cx + g, cy + g);
        c.drawCircle(cx, cy, g, darkP);
        c.restore();
        // eyes: mirrored contrast
        c.drawCircle(cx - g * 0.38f, cy - g * 0.15f, Ui.dp(ctx, 1.8f), darkP);
        c.drawCircle(cx + g * 0.38f, cy - g * 0.15f, Ui.dp(ctx, 1.8f), gemPaint);
        // mouth: split slit
        c.save();
        c.clipRect(cx - g * 0.4f, cy + g * 0.35f, cx, cy + g * 0.55f);
        c.drawRect(cx - g * 0.4f, cy + g * 0.38f, cx + g * 0.4f, cy + g * 0.5f, darkP);
        c.restore();
        c.save();
        c.clipRect(cx, cy + g * 0.35f, cx + g * 0.4f, cy + g * 0.55f);
        c.drawRect(cx - g * 0.4f, cy + g * 0.38f, cx + g * 0.4f, cy + g * 0.5f, gemPaint);
        c.restore();

        c.drawRoundRect(card, r, r, borderPaint);
    }
}
