package com.aaron.bluehour;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Torn-paper jagged rectangle — P5's cut-out shape, for backgrounds. */
public class JaggedDrawable extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint stroke;
    private final float jit;
    private final int seed;
    private final Path path = new Path();

    public JaggedDrawable(int color, float jitterPx) {
        this(color, jitterPx, 0, 0);
    }

    public JaggedDrawable(int color, float jitterPx, int strokeColor, float strokeW) {
        paint.setColor(color);
        jit = jitterPx;
        seed = color * 31 + (int) jitterPx;
        if (strokeW > 0) {
            stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(strokeW);
            stroke.setColor(strokeColor);
        }
    }

    private float j(int s) {
        int h = (s * 1103515245 + 12345) & 0x7FFFFFFF;
        return ((h % 1000) / 1000f - 0.5f) * 2f * jit;
    }

    private void buildPath(Rect b) {
        path.reset();
        int steps = 6;
        for (int i = 0; i <= steps; i++) {
            float x = b.left + (b.width()) * i / (float) steps;
            float y = b.top + j(seed + i);
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        for (int i = 1; i <= 2; i++) {
            path.lineTo(b.right + j(seed + 40 + i), b.top + b.height() * i / 2f);
        }
        for (int i = 0; i <= steps; i++) {
            float x = b.right - b.width() * i / (float) steps;
            path.lineTo(x, b.bottom + j(seed + 80 + i));
        }
        path.lineTo(b.left + j(seed + 120), b.top + b.height() * 0.5f);
        path.close();
    }

    @Override
    public void draw(Canvas c) {
        buildPath(getBounds());
        c.drawPath(path, paint);
        if (stroke != null) c.drawPath(path, stroke);
    }

    @Override public void setAlpha(int a) { paint.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter f) { paint.setColorFilter(f); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
