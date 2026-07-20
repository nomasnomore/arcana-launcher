package com.aaron.bluehour;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Parallelogram background — the signature P3R "nothing sits straight" shape. */
public class SkewDrawable extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint stroke;
    private final float skew;
    private final Path path = new Path();

    public SkewDrawable(int color, float skewPx) {
        paint.setColor(color);
        skew = skewPx;
    }

    public SkewDrawable(int color, float skewPx, int strokeColor, float strokeW) {
        this(color, skewPx);
        stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(strokeW);
        stroke.setColor(strokeColor);
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        path.reset();
        path.moveTo(b.left + skew, b.top);
        path.lineTo(b.right, b.top);
        path.lineTo(b.right - skew, b.bottom);
        path.lineTo(b.left, b.bottom);
        path.close();
        c.drawPath(path, paint);
        if (stroke != null) c.drawPath(path, stroke);
    }

    @Override public void setAlpha(int a) { paint.setAlpha(a); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter f) { paint.setColorFilter(f); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
