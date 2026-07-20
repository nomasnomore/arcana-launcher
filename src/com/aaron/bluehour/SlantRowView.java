package com.aaron.bluehour;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * v3 app row — structured, on the white card: black label, black rounded
 * tile with a grayscale icon, royal-blue slash bar on the focused row
 * (text flips to white), red slash chevron flash on press.
 */
public class SlantRowView extends View {

    private AppEntry entry;
    private boolean dot;
    private int member = -1; // -1 normal, 0 not in category, 1 in category
    private float focus = 0f;
    private float press = 0f;
    private float jitterIndent = 0f;

    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chevronPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path bar = new Path();
    private final Path chip = new Path();
    private final RectF tileRect = new RectF();
    private final RectF iconRect = new RectF();

    private ValueAnimator anim;
    private final float rowH;

    public SlantRowView(Context c) {
        super(c);
        rowH = Ui.dp(c, 58);
        textPaint.setTypeface(Ui.tf(c));
        barPaint.setColor(Theme.get().accent);
        chevronPaint.setColor(Theme.get().pop);
        tilePaint.setColor(0xFF0A0C12);
        // grayscale icons for the mock's monochrome look
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0f);
        iconPaint.setColorFilter(new ColorMatrixColorFilter(cm));
    }

    public void bind(AppEntry e, boolean hasDot, int position, int memberState) {
        entry = e;
        dot = hasDot;
        member = memberState;
        int hash = (e.pkg.hashCode() ^ (position * 31)) & 0x7FFFFFFF;
        jitterIndent = hash % Ui.dpi(getContext(), 10);
        setRotation(0f);
        invalidate();
    }

    public void setFx(float f, float ignoredBelow) {
        if (Math.abs(f - focus) > 0.004f) {
            focus = f;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension(MeasureSpec.getSize(wSpec), (int) rowH);
    }

    @Override
    public void setPressed(boolean pressed) {
        boolean was = isPressed();
        super.setPressed(pressed);
        if (was == pressed) return;
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(press, pressed ? 1f : 0f);
        anim.setDuration(pressed ? 60 : 220);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                press = ((Float) a.getAnimatedValue()).floatValue();
                invalidate();
            }
        });
        anim.start();
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
        if (entry == null) return;
        Context ctx = getContext();
        float eff = Math.max(focus, press * 0.9f);
        if (eff > 1f) eff = 1f;

        float ts = Ui.dp(ctx, 20.5f) * (1f + 0.14f * eff);
        textPaint.setTextSize(ts);

        float cy = rowH / 2f;
        float tileS = Ui.dp(ctx, 38);
        float x0 = Ui.dp(ctx, 6) + jitterIndent;
        float tx = x0 + tileS + Ui.dp(ctx, 14);

        float maxW = getWidth() - tx - Ui.dp(ctx, 64);
        String label = Ui.ellipsize(textPaint, entry.labelUp, maxW);
        float tw = textPaint.measureText(label);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float base = cy - (fm.ascent + fm.descent) / 2f;

        // ---- blue slash bar on focus ----
        if (eff > 0.04f) {
            float skew = Ui.dp(ctx, 10);
            float left = x0 - Ui.dp(ctx, 4);
            float right = tx + tw + Ui.dp(ctx, 26) + eff * Ui.dp(ctx, 10);
            float top = cy - Ui.dp(ctx, 23);
            float bot = cy + Ui.dp(ctx, 23);
            bar.reset();
            bar.moveTo(left + skew, top);
            bar.lineTo(right, top);
            bar.lineTo(right - skew, bot);
            bar.lineTo(left, bot);
            bar.close();
            barPaint.setAlpha((int) (eff * 255));
            c.drawPath(bar, barPaint);
        }

        // ---- red chevron flash on press ----
        if (press > 0.05f) {
            float chW = Ui.dp(ctx, 7);
            float chSkew = Ui.dp(ctx, 5);
            float chL = x0 - Ui.dp(ctx, 14);
            float top = cy - Ui.dp(ctx, 23);
            float bot = cy + Ui.dp(ctx, 23);
            chip.reset();
            chip.moveTo(chL + chSkew, top);
            chip.lineTo(chL + chW + chSkew, top);
            chip.lineTo(chL + chW, bot);
            chip.lineTo(chL, bot);
            chip.close();
            chevronPaint.setAlpha((int) (press * 255));
            c.drawPath(chip, chevronPaint);
        }

        // ---- black tile + grayscale icon ----
        float tileTop = cy - tileS / 2f;
        tileRect.set(x0, tileTop, x0 + tileS, tileTop + tileS);
        tilePaint.setColor(eff > 0.5f ? 0xFF06090F : 0xFF0A0C12);
        c.drawRoundRect(tileRect, Ui.dp(ctx, 7), Ui.dp(ctx, 7), tilePaint);
        if (entry.icon != null) {
            float pad = Ui.dp(ctx, 6);
            iconRect.set(tileRect.left + pad, tileRect.top + pad,
                    tileRect.right - pad, tileRect.bottom - pad);
            c.drawBitmap(entry.icon, null, iconRect, iconPaint);
        }

        // ---- label ----
        textPaint.setColor(lerpColor(0xFF0A0C10, Theme.get().accentText(), eff));
        c.drawText(label, tx, base, textPaint);

        // ---- add-mode membership chip at the right edge ----
        if (member >= 0) {
            float chS = Ui.dp(ctx, 24);
            float chX = getWidth() - chS - Ui.dp(ctx, 18);
            float chY = cy - chS / 2f;
            float skw = Ui.dp(ctx, 5);
            Path box = new Path();
            box.moveTo(chX + skw, chY);
            box.lineTo(chX + chS, chY);
            box.lineTo(chX + chS - skw, chY + chS);
            box.lineTo(chX, chY + chS);
            box.close();
            Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            if (member == 1) {
                boxPaint.setColor(Theme.get().accent);
                c.drawPath(box, boxPaint);
                Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
                tick.setStyle(Paint.Style.STROKE);
                tick.setStrokeWidth(Ui.dp(ctx, 3));
                tick.setStrokeCap(Paint.Cap.ROUND);
                tick.setColor(Theme.get().accentText());
                c.drawLine(chX + chS * 0.26f, chY + chS * 0.52f,
                        chX + chS * 0.44f, chY + chS * 0.72f, tick);
                c.drawLine(chX + chS * 0.44f, chY + chS * 0.72f,
                        chX + chS * 0.78f, chY + chS * 0.26f, tick);
            } else {
                boxPaint.setStyle(Paint.Style.STROKE);
                boxPaint.setStrokeWidth(Ui.dp(ctx, 2));
                boxPaint.setColor(0x6606080C);
                c.drawPath(box, boxPaint);
            }
        }

        // ---- notification chip ----
        if (dot) {
            float dx = tx + tw + Ui.dp(ctx, 11);
            float dh = Ui.dp(ctx, 10);
            float dy = cy - dh / 2f;
            float ds = Ui.dp(ctx, 3);
            dotPaint.setColor(eff > 0.5f ? Theme.get().accentText() : Theme.get().pop);
            Path d2 = new Path();
            d2.moveTo(dx + ds, dy);
            d2.lineTo(dx + dh, dy);
            d2.lineTo(dx + dh - ds, dy + dh);
            d2.lineTo(dx, dy + dh);
            d2.close();
            c.drawPath(d2, dotPaint);
        }
    }
}
