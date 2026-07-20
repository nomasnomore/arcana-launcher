package com.aaron.bluehour;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/**
 * A visible-but-subtle CRT overlay drawn on top of everything (UI +
 * wallpaper): dp-scaled scanlines (so they read on hi-dpi screens), a
 * soft vignette, a warm tint, and a slow rolling scan bar so you can tell
 * it's alive. Touch-transparent — never intercepts input.
 */
public class CRTView extends View {

    private final Paint scanPaint = new Paint();
    private final Paint vignettePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rollPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap scanTile;
    private boolean enabled = false;
    private int tint = 0x00000000;
    private float rollPos = 0f;
    private ValueAnimator roll;
    private long lastFrame = 0;
    // rolling scan-bar gradient built once per size; moved each frame via a
    // reused matrix so onDraw allocates nothing.
    private LinearGradient rollShader;
    private final android.graphics.Matrix rollMatrix = new android.graphics.Matrix();
    private float bandH = 0f;

    public CRTView(Context c) {
        super(c);
        setClickable(false);
        setFocusable(false);
        buildScanTile();
    }

    private void buildScanTile() {
        // scanline pitch in real dp so it reads on 3x-density screens:
        // a ~1dp dark band every ~3dp
        int pitch = Math.max(4, Ui.dpi(getContext(), 2.4f));
        int dark = Math.max(1, pitch / 2);
        scanTile = Bitmap.createBitmap(1, pitch, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < pitch; y++) {
            scanTile.setPixel(0, y, y >= pitch - dark ? 0x40000000 : 0x00000000);
        }
        scanPaint.setShader(new android.graphics.BitmapShader(scanTile,
                Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
    }

    public void setEnabled2(boolean on, int warmTint) {
        enabled = on;
        tint = warmTint;
        setVisibility(on ? VISIBLE : GONE);
        if (on) startRoll(); else stopRoll();
        invalidate();
    }

    private void startRoll() {
        if (roll != null) return;
        roll = ValueAnimator.ofFloat(0f, 1f);
        roll.setDuration(4200);
        roll.setRepeatCount(ValueAnimator.INFINITE);
        roll.setInterpolator(null);
        roll.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                rollPos = ((Float) a.getAnimatedValue()).floatValue();
                // cap redraw to ~20fps — the roll is slow, no need for 120
                long now = android.os.SystemClock.uptimeMillis();
                if (now - lastFrame >= 50) {
                    lastFrame = now;
                    invalidate();
                }
            }
        });
        roll.start();
    }

    private void stopRoll() {
        if (roll != null) { roll.cancel(); roll = null; }
    }

    /** Pause/resume the animator with the host lifecycle (saves background battery). */
    public void hostStopped() { stopRoll(); }
    public void hostResumed() { if (enabled) startRoll(); }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopRoll();
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent e) {
        return false; // pass every touch through to the UI below
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (w == 0 || h == 0) return;
        vignettePaint.setShader(new RadialGradient(w / 2f, h / 2f,
                (float) Math.hypot(w, h) / 2f,
                new int[]{0x00000000, 0x1A000000, 0x66000000},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        // build the roll band gradient once at canonical y=0..bandH; onDraw
        // just slides it with rollMatrix (no per-frame allocation)
        bandH = h * 0.16f;
        rollShader = new LinearGradient(0, 0, 0, bandH,
                new int[]{0x00FFFFFF, 0x14FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
        rollPaint.setShader(rollShader);
    }

    @Override
    protected void onDraw(Canvas c) {
        if (!enabled) return;
        int w = getWidth();
        int h = getHeight();
        if (tint != 0) c.drawColor(tint);
        c.drawRect(0, 0, w, h, vignettePaint);
        c.drawRect(0, 0, w, h, scanPaint);

        // slow rolling scan bar — a soft bright band drifting down.
        // Slide the pre-built gradient instead of rebuilding it each frame.
        if (rollShader != null) {
            float cy = rollPos * (h + bandH) - bandH / 2f;
            float top = cy - bandH / 2f;
            rollMatrix.setTranslate(0, top);
            rollShader.setLocalMatrix(rollMatrix);
            c.drawRect(0, top, w, top + bandH, rollPaint);
        }
    }
}
