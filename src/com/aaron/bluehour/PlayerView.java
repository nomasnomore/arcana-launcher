package com.aaron.bluehour;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.text.TextPaint;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Now-playing, flat and typographic. Premium pass: dwell-then-scroll
 * marquee, animated EQ bars while playing, slide-in/out entrance, and a
 * cyan progress hairline along the ribbon's bottom edge.
 */
public class PlayerView extends View {

    public interface Listener {
        void onPlayPause();
        void onPrev();
        void onNext();
        void onOpenApp();
    }

    private String title = "";
    private String artist = "";
    private boolean playing = false;
    private android.graphics.Bitmap albumArt;
    private final Paint artPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint captionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] btnScale = {1f, 1f, 1f};
    private long durationMs = 0;
    private long basePosMs = 0;
    private long baseTimeMs = 0; // elapsedRealtime basis
    private float speed = 1f;
    private Listener listener;
    private boolean shown = false;

    private final Paint ribbonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eqPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // reused each frame for album-art draw (no per-frame allocation)
    private final Matrix artMatrix = new Matrix();
    private final Paint artDarkPaint = new Paint();
    private final TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint artistPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ribbon = new RectF();
    private final Path p = new Path();

    private float btnPrevX, btnPlayX, btnNextX, btnY;
    private float scroll = 0f;
    private long dwellUntil = 0;
    private int pressed = 0; // 1 play, 2 prev, 3 next, 4 ribbon

    /** Single ticker: marquee scroll, EQ bars, progress — while visible. */
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (getVisibility() != VISIBLE || !isAttachedToWindow()) return;
            float tw = titlePaint.measureText(title);
            float avail = ribbon.width() - textStartX() - Ui.dp(getContext(), 10);
            if (tw > avail && System.currentTimeMillis() > dwellUntil) {
                scroll += Ui.dp(getContext(), 0.6f);
                if (scroll > tw + Ui.dp(getContext(), 34)) {
                    scroll = 0f;
                    dwellUntil = System.currentTimeMillis() + 1500;
                }
            }
            invalidate();
            postDelayed(this, playing ? 40 : 80);
        }
    };

    public PlayerView(Context c) {
        super(c);
        ribbonPaint.setColor(0xF0060A12);
        glyphPaint.setColor(0xFFF2F5FA);
        shadowPaint.setColor(0xB3000000);
        pressPaint.setColor((Theme.get().accent & 0x00FFFFFF) | 0x66000000);
        Theme th = Theme.get();
        eqPaint.setColor(th.accentBright);
        progressPaint.setColor(th.accentBright);
        titlePaint.setTypeface(Ui.tf(c));
        titlePaint.setTextSize(Ui.dp(c, 15));
        titlePaint.setColor(0xFFF2F5FA);
        artistPaint.setTypeface(Ui.tfUpright(c));
        artistPaint.setTextSize(Ui.dp(c, 10.5f));
        artistPaint.setColor(th.subtitle);
        artistPaint.setShadowLayer(Ui.dp(c, 3), 0, Ui.dp(c, 1), 0x99000000);
        captionPaint.setTypeface(Ui.tfUpright(c));
        captionPaint.setTextSize(Ui.dp(c, 8.5f));
        captionPaint.setColor(th.subtitle);
        captionPaint.setLetterSpacing(0.28f);
        captionPaint.setShadowLayer(Ui.dp(c, 3), 0, Ui.dp(c, 1), 0x99000000);
        setClickable(true);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void setAlbumArt(android.graphics.Bitmap b) {
        albumArt = b;
        // tint the art blue so it stays in-palette: multiply toward cyan
        if (b != null) {
            android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix(new float[]{
                    0.33f, 0.33f, 0.33f, 0, 8,
                    0.30f, 0.40f, 0.45f, 0, 20,
                    0.45f, 0.55f, 0.75f, 0, 40,
                    0, 0, 0, 1, 0});
            artPaint.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm));
        }
        invalidate();
    }

    public void update(String t, String a, boolean isPlaying,
                       long durMs, long posMs, long posTimeMs, float spd) {
        String nt = t == null ? "" : t.toUpperCase(java.util.Locale.getDefault());
        if (!nt.equals(title)) {
            scroll = 0f;
            dwellUntil = System.currentTimeMillis() + 1500;
        }
        title = nt;
        artist = a == null ? "" : a;
        playing = isPlaying;
        durationMs = durMs;
        basePosMs = posMs;
        baseTimeMs = posTimeMs;
        speed = spd <= 0f ? 1f : spd;
        removeCallbacks(ticker);
        post(ticker);
        invalidate();
    }

    public void showAnimated() {
        if (shown) return;
        shown = true;
        setVisibility(VISIBLE);
        setTranslationX(Ui.dp(getContext(), 230));
        animate().translationX(0f).setDuration(330)
                .setInterpolator(new OvershootInterpolator(1.1f)).start();
        removeCallbacks(ticker);
        post(ticker);
    }

    public void hideAnimated() {
        if (!shown) return;
        shown = false;
        animate().translationX(Ui.dp(getContext(), 250)).setDuration(220)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        setVisibility(GONE);
                        setTranslationX(0f);
                    }
                }).start();
    }

    public boolean isShown2() {
        return shown;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(ticker);
    }

    /** Stop the ticker when the host backgrounds (media watch reposts on return). */
    public void hostStopped() { removeCallbacks(ticker); }

    private float textStartX() {
        return Ui.dp(getContext(), 30); // room for the EQ bars
    }

    private float progressFrac() {
        if (durationMs <= 0) return -1f;
        long pos = basePosMs;
        if (playing) {
            pos += (long) ((SystemClock.elapsedRealtime() - baseTimeMs) * speed);
        }
        float f = pos / (float) durationMs;
        return f < 0f ? 0f : (f > 1f ? 1f : f);
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension(Ui.dpi(getContext(), 172), Ui.dpi(getContext(), 96));
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        ribbon.set(0, Ui.dp(getContext(), 12), w, Ui.dp(getContext(), 44));
        btnY = h - Ui.dp(getContext(), 16);
        btnPlayX = w * 0.56f; // biased right, centered on the right half
        btnPrevX = btnPlayX - Ui.dp(getContext(), 46);
        btnNextX = btnPlayX + Ui.dp(getContext(), 46);
    }

    @Override
    protected void onDraw(Canvas c) {
        Context ctx = getContext();

        // NOW PLAYING micro-caption above the ribbon
        c.drawText("NOW PLAYING", ribbon.left + Ui.dp(ctx, 4),
                ribbon.top - Ui.dp(ctx, 5), captionPaint);

        // black slash ribbon
        float skew = Ui.dp(ctx, 9);
        p.reset();
        p.moveTo(ribbon.left + skew, ribbon.top);
        p.lineTo(ribbon.right, ribbon.top);
        p.lineTo(ribbon.right - skew, ribbon.bottom);
        p.lineTo(ribbon.left, ribbon.bottom);
        p.close();
        c.drawPath(p, ribbonPaint);
        // tinted album art filling the ribbon behind the text
        if (albumArt != null) {
            c.save();
            c.clipPath(p);
            float scale = Math.max(ribbon.width() / albumArt.getWidth(),
                    ribbon.height() / albumArt.getHeight());
            artMatrix.reset();
            artMatrix.setScale(scale, scale);
            artMatrix.postTranslate(ribbon.left
                            + (ribbon.width() - albumArt.getWidth() * scale) / 2f,
                    ribbon.top
                            + (ribbon.height() - albumArt.getHeight() * scale) / 2f);
            c.drawBitmap(albumArt, artMatrix, artPaint);
            // darken so white title stays readable
            artDarkPaint.setColor(0x99000000);
            c.drawPath(p, artDarkPaint);
            c.restore();
        }
        if (pressed == 4) c.drawPath(p, pressPaint);

        // EQ bars at the ribbon's left end
        float bx = ribbon.left + Ui.dp(ctx, 13);
        float bw = Ui.dp(ctx, 3);
        float gap = Ui.dp(ctx, 2);
        float maxH = Ui.dp(ctx, 14);
        float baseY = ribbon.centerY() + maxH / 2f;
        long now = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            float frac;
            if (playing) {
                double t = now / 1000.0 * (2.1 + i * 0.7) + i * 1.9;
                frac = 0.35f + 0.65f * (float) Math.abs(Math.sin(t));
            } else {
                frac = 0.25f;
            }
            float x0 = bx + i * (bw + gap);
            c.drawRect(x0, baseY - maxH * frac, x0 + bw, baseY, eqPaint);
        }

        // title (dwell-then-scroll)
        c.save();
        c.clipRect(ribbon.left + textStartX() - Ui.dp(ctx, 2), ribbon.top,
                ribbon.right - Ui.dp(ctx, 8), ribbon.bottom);
        float tw = titlePaint.measureText(title);
        float tx = ribbon.left + textStartX();
        float ty = ribbon.centerY() + Ui.dp(ctx, 5.5f);
        float avail = ribbon.width() - textStartX() - Ui.dp(ctx, 10);
        if (tw > avail) {
            c.drawText(title, tx - scroll, ty, titlePaint);
            c.drawText(title, tx - scroll + tw + Ui.dp(ctx, 34), ty, titlePaint);
        } else {
            c.drawText(title, tx, ty, titlePaint);
        }
        c.restore();

        // progress hairline along the ribbon's bottom edge
        float frac = progressFrac();
        if (frac >= 0f) {
            float inset = Ui.dp(ctx, 4);
            float px0 = ribbon.left + inset;
            float px1 = ribbon.right - skew - inset;
            float py = ribbon.bottom - Ui.dp(ctx, 2);
            progressPaint.setStrokeWidth(Ui.dp(ctx, 2));
            progressPaint.setAlpha(70);
            c.drawLine(px0, py, px1, py, progressPaint);
            progressPaint.setAlpha(255);
            c.drawLine(px0, py, px0 + (px1 - px0) * frac, py, progressPaint);
        }

        // artist line
        String art = Ui.ellipsize(artistPaint, artist, getWidth() - Ui.dp(ctx, 16));
        c.drawText(art, Ui.dp(ctx, 6), ribbon.bottom + Ui.dp(ctx, 16), artistPaint);

        // controls
        drawControl(c, btnPrevX, btnY, 2);
        drawControl(c, btnPlayX, btnY, 1);
        drawControl(c, btnNextX, btnY, 3);
    }

    private void drawControl(Canvas c, float cx, float cy, int kind) {
        Context ctx = getContext();
        float off = Ui.dp(ctx, 2);
        // ease the scale-bump toward target each frame
        float target = pressed == kind ? 1.35f : 1f;
        int idx = kind - 1;
        btnScale[idx] += (target - btnScale[idx]) * 0.4f;
        if (Math.abs(target - btnScale[idx]) > 0.01f) invalidate();
        c.save();
        c.scale(btnScale[idx], btnScale[idx], cx, cy);
        if (pressed == kind) {
            c.drawCircle(cx, cy, Ui.dp(ctx, 15), pressPaint);
        }
        for (int pass = 0; pass < 2; pass++) {
            Paint paint = pass == 0 ? shadowPaint : glyphPaint;
            float d = pass == 0 ? off : 0;
            float s = Ui.dp(ctx, 7);
            if (kind == 1) {
                if (playing) {
                    c.drawRect(cx - s + d, cy - s + d, cx - s * 0.25f + d, cy + s + d, paint);
                    c.drawRect(cx + s * 0.25f + d, cy - s + d, cx + s + d, cy + s + d, paint);
                } else {
                    p.reset();
                    p.moveTo(cx - s * 0.7f + d, cy - s + d);
                    p.lineTo(cx + s + d, cy + d);
                    p.lineTo(cx - s * 0.7f + d, cy + s + d);
                    p.close();
                    c.drawPath(p, paint);
                }
            } else {
                float dir = kind == 2 ? -1f : 1f;
                for (int i = 0; i < 2; i++) {
                    float xo = (i - 0.5f) * s * 1.2f * dir;
                    p.reset();
                    p.moveTo(cx + xo - dir * s * 0.6f + d, cy - s * 0.85f + d);
                    p.lineTo(cx + xo + dir * s * 0.6f + d, cy + d);
                    p.lineTo(cx + xo - dir * s * 0.6f + d, cy + s * 0.85f + d);
                    p.close();
                    c.drawPath(p, paint);
                }
            }
        }
        c.restore();
    }

    private int regionAt(float x, float y) {
        if (ribbon.contains(x, y)) return 4;
        float bs = Ui.dp(getContext(), 20);
        if (Math.abs(y - btnY) < bs) {
            if (Math.abs(x - btnPrevX) < bs) return 2;
            if (Math.abs(x - btnPlayX) < bs) return 1;
            if (Math.abs(x - btnNextX) < bs) return 3;
        }
        return 0;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = regionAt(e.getX(), e.getY());
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                int r = regionAt(e.getX(), e.getY());
                if (r != 0 && r == pressed && listener != null) {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    if (r == 1) listener.onPlayPause();
                    else if (r == 2) listener.onPrev();
                    else if (r == 3) listener.onNext();
                    else listener.onOpenApp();
                }
                pressed = 0;
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = 0;
                invalidate();
                return true;
        }
        return super.onTouchEvent(e);
    }
}
