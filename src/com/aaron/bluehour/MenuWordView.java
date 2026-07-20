package com.aaron.bluehour;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.TextPaint;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Category word in the home column, styled after the mock: big cyan italic
 * word, thin underline, small Japanese subtitle. Pressed: word sits black
 * on a white slash with a black underbar (the mock's MAPS treatment).
 */
public class MenuWordView extends View {

    private final String label;
    private final String sub;
    private final int baseColor;
    private boolean dot;
    private float press = 0f;

    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint subPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path slash = new Path();
    private final Path star = new Path();
    private final Path dotPath = new Path();
    private ValueAnimator anim;
    private final float wordH;

    public MenuWordView(Context c, String labelText, String subText, int color) {
        super(c);
        label = labelText;
        sub = subText;
        baseColor = color;
        wordH = Ui.dp(c, 56);
        textPaint.setTypeface(Ui.tf(c));
        textPaint.setTextSize(Ui.dp(c, 27));
        subPaint.setTypeface(Ui.tfUpright(c));
        subPaint.setTextSize(Ui.dp(c, 11.5f));
        // P4 sits straight; P5 leans hard; P3 leans a little
        int ss = Theme.get().shapeStyle;
        setRotation(ss == 1 ? 0f : (ss == 2 ? -3f : -1.6f));
        slashPaint.setColor(0xFFFFFFFF);
        barPaint.setColor(0xFF06080C);
        linePaint.setStrokeWidth(Ui.dp(c, 1.5f));
        setClickable(true);
    }

    public String label() {
        return label;
    }

    public void setDot(boolean d) {
        if (dot != d) {
            dot = d;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        float tw = Math.max(textPaint.measureText(label), subPaint.measureText(sub));
        setMeasuredDimension((int) (tw + Ui.dp(getContext(), 54)), (int) wordH);
    }

    @Override
    public void setPressed(boolean pressed) {
        boolean was = isPressed();
        super.setPressed(pressed);
        if (was == pressed) return;
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(press, pressed ? 1f : 0f);
        anim.setDuration(pressed ? 70 : 250);
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

    /** Builds a torn-paper jagged rectangle into `slash` (deterministic). */
    private void buildJagged(float l, float t, float r, float b) {
        Context ctx = getContext();
        float j = Ui.dp(ctx, 3.5f);
        int seed = label.hashCode() & 0x7FFFFFFF;
        slash.reset();
        int steps = 5;
        // top edge L→R
        for (int i = 0; i <= steps; i++) {
            float x = l + (r - l) * i / steps;
            float y = t + jitter(seed + i, j);
            if (i == 0) slash.moveTo(x, y); else slash.lineTo(x, y);
        }
        // right edge T→B
        for (int i = 1; i <= 2; i++) {
            slash.lineTo(r + jitter(seed + 40 + i, j), t + (b - t) * i / 2f);
        }
        // bottom edge R→L
        for (int i = 0; i <= steps; i++) {
            float x = r - (r - l) * i / steps;
            float y = b + jitter(seed + 80 + i, j);
            slash.lineTo(x, y);
        }
        // left edge B→T
        slash.lineTo(l + jitter(seed + 120, j), t + (b - t) * 0.5f);
        slash.close();
    }

    /** Builds an N-point star into `star`. */
    private void buildStar(float cx, float cy, float outer, float inner, int points) {
        star.reset();
        for (int i = 0; i < points * 2; i++) {
            float r = (i % 2 == 0) ? outer : inner;
            double a = -Math.PI / 2 + i * Math.PI / points;
            float x = cx + (float) Math.cos(a) * r;
            float y = cy + (float) Math.sin(a) * r;
            if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
        }
        star.close();
    }

    private static float jitter(int seed, float amp) {
        int h = (seed * 1103515245 + 12345) & 0x7FFFFFFF;
        return ((h % 1000) / 1000f - 0.5f) * 2f * amp;
    }

    /** P5 ransom-note lettering: per-letter tilt/scale, some in cut-out boxes. */
    private void drawRansom(Canvas c, Context ctx, String text,
                            float startX, float baseY, float ts, int color) {
        float x = startX;
        int base = text.hashCode();
        Paint box = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i < text.length(); i++) {
            String ch = text.substring(i, i + 1);
            float cw = textPaint.measureText(ch);
            if (ch.equals(" ")) { x += cw; continue; }
            int seed = (base * 31 + i * 2654435) & 0x7FFFFFFF;
            float ang = jitter(seed, 7f);
            float scale = 0.86f + ((seed >> 8) % 1000) / 1000f * 0.3f;
            float dy = jitter(seed + 5, Ui.dp(ctx, 4));
            boolean boxed = (seed % 3 == 0);

            c.save();
            c.translate(x + cw / 2f, baseY + dy);
            c.rotate(ang);
            c.scale(scale, scale);
            if (boxed) {
                boolean red = ((seed >> 4) % 2) == 0;
                box.setColor(red ? Theme.get().accent : 0xFF111111);
                float pad = Ui.dp(ctx, 2.5f);
                c.drawRect(-cw / 2f - pad, -ts * 0.82f,
                        cw / 2f + pad, ts * 0.20f, box);
                textPaint.setColor(0xFFFFFFFF);
            } else {
                textPaint.setColor(color);
            }
            c.drawText(ch, -cw / 2f, 0, textPaint);
            c.restore();
            x += cw * 0.98f;
        }
        textPaint.setColor(color);
    }

    @Override
    protected void onDraw(Canvas c) {
        Context ctx = getContext();
        float tx = Ui.dp(ctx, 12);
        float ts = textPaint.getTextSize();
        float wordBase = Ui.dp(ctx, 26);
        float lineY = wordBase + Ui.dp(ctx, 7);
        float subBase = wordBase + Ui.dp(ctx, 22);
        float tw = textPaint.measureText(label);

        if (press > 0.04f) {
            float left = tx - Ui.dp(ctx, 12);
            float right = tx + tw + Ui.dp(ctx, 22);
            float top = wordBase - ts * 0.86f;
            float bot = wordBase + ts * 0.26f;
            int a = (int) (press * 255);
            slashPaint.setAlpha(a);
            if (Theme.get().shapeStyle == 1) {
                // P4: rounded pill selection, no skew
                float rad = (bot - top) * 0.5f;
                android.graphics.RectF pill = new android.graphics.RectF(
                        left, top, right + Ui.dp(ctx, 4), bot);
                c.drawRoundRect(pill, rad, rad, slashPaint);
                barPaint.setAlpha(a);
                android.graphics.RectF ub = new android.graphics.RectF(
                        left + Ui.dp(ctx, 8), bot + Ui.dp(ctx, 2),
                        right, bot + Ui.dp(ctx, 6));
                c.drawRoundRect(ub, Ui.dp(ctx, 2), Ui.dp(ctx, 2), barPaint);
            } else if (Theme.get().shapeStyle == 2) {
                // P5: a red star bursts behind the left of the word...
                float starCx = left + Ui.dp(ctx, 2);
                float starCy = (top + bot) / 2f;
                float starR = (bot - top) * 0.62f;
                buildStar(starCx, starCy, starR, starR * 0.44f, 5);
                barPaint.setColor(Theme.get().accent);
                barPaint.setAlpha(a);
                c.save();
                c.rotate(-14, starCx, starCy);
                c.drawPath(star, barPaint);
                c.restore();
                barPaint.setColor(0xFF06080C);
                // ...then the jagged torn-paper banner on top
                buildJagged(left - Ui.dp(ctx, 4), top, right + Ui.dp(ctx, 6), bot);
                c.drawPath(slash, slashPaint);
                // red under-shard peeking out
                barPaint.setColor(Theme.get().accent);
                barPaint.setAlpha(a);
                buildJagged(left + Ui.dp(ctx, 10), bot - Ui.dp(ctx, 1),
                        right - Ui.dp(ctx, 6), bot + Ui.dp(ctx, 9));
                c.drawPath(slash, barPaint);
                barPaint.setColor(0xFF06080C);
            } else {
                float skew = Ui.dp(ctx, 10);
                // P3: an electric cyan fringe slash sits behind the white one
                float fx = Ui.dp(ctx, 4);
                barPaint.setColor(Theme.get().accentBright);
                barPaint.setAlpha((int) (a * 0.85f));
                slash.reset();
                slash.moveTo(left + skew - fx, top - fx);
                slash.lineTo(right + skew * 0.4f - fx, top - Ui.dp(ctx, 2) - fx);
                slash.lineTo(right - skew - fx, bot - fx);
                slash.lineTo(left - fx, bot - fx);
                slash.close();
                c.drawPath(slash, barPaint);
                barPaint.setColor(0xFF06080C);
                // the white slash on top
                slash.reset();
                slash.moveTo(left + skew, top);
                slash.lineTo(right + skew * 0.4f, top - Ui.dp(ctx, 2));
                slash.lineTo(right - skew, bot);
                slash.lineTo(left, bot);
                slash.close();
                c.drawPath(slash, slashPaint);
                // bright cyan hairline along the top edge (electric)
                edgePaint.setColor(Theme.get().accentBright);
                edgePaint.setAlpha(a);
                edgePaint.setStrokeWidth(Ui.dp(ctx, 2));
                c.drawLine(left + skew, top, right + skew * 0.4f,
                        top - Ui.dp(ctx, 2), edgePaint);
                // underbar just below the slash
                barPaint.setAlpha(a);
                c.save();
                c.skew(-0.18f, 0f);
                c.drawRect(left + Ui.dp(ctx, 14), bot + Ui.dp(ctx, 2),
                        right + Ui.dp(ctx, 4), bot + Ui.dp(ctx, 7), barPaint);
                c.restore();
            }
        }

        if (press > 0.3f) {
            textPaint.clearShadowLayer();
        } else {
            textPaint.setShadowLayer(Ui.dp(ctx, 5), 0, Ui.dp(ctx, 2), 0xB3000000);
        }
        int mainColor = lerpColor(baseColor, 0xFF06080C, press);
        textPaint.setColor(mainColor);
        if (Theme.get().ransom) {
            drawRansom(c, ctx, label, tx, wordBase, ts, mainColor);
        } else {
            c.drawText(label, tx, wordBase, textPaint);
        }

        // thin underline under the word (hidden while pressed)
        if (press < 0.5f) {
            float lineRight = tx + tw + Ui.dp(ctx, 32);
            if (Theme.get().rainbow) {
                // P4's rainbow line
                linePaint.setShader(new android.graphics.LinearGradient(
                        tx - Ui.dp(ctx, 2), 0, lineRight, 0,
                        Theme.RAINBOW, null,
                        android.graphics.Shader.TileMode.CLAMP));
                linePaint.setAlpha((int) (255 * (1f - press * 2f)));
                linePaint.setStrokeWidth(Ui.dp(ctx, 2.5f));
            } else {
                linePaint.setShader(null);
                int ruleC = Theme.get().rule;
                linePaint.setColor(lerpColor(ruleC, ruleC & 0x00FFFFFF, press * 2f));
                linePaint.setStrokeWidth(Ui.dp(ctx, 1.5f));
            }
            c.drawLine(tx - Ui.dp(ctx, 2), lineY, lineRight, lineY, linePaint);
            linePaint.setShader(null);
        }

        subPaint.setColor(0xCCDCF2FF);
        subPaint.setShadowLayer(Ui.dp(ctx, 3), 0, Ui.dp(ctx, 1), 0x99000000);
        c.drawText(sub, tx + Ui.dp(ctx, 2), subBase, subPaint);

        if (dot) {
            float dx = tx + tw + Ui.dp(ctx, 13);
            float dh = Ui.dp(ctx, 11);
            float dy = wordBase - ts * 0.55f;
            float ds = Ui.dp(ctx, 3);
            dotPaint.setColor(Theme.get().pop);
            dotPath.reset();
            dotPath.moveTo(dx + ds, dy);
            dotPath.lineTo(dx + dh, dy);
            dotPath.lineTo(dx + dh - ds, dy + dh);
            dotPath.lineTo(dx, dy + dh);
            dotPath.close();
            c.drawPath(dotPath, dotPaint);
        }
    }
}
