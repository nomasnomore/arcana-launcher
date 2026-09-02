package com.aaron.bluehour;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;
import java.io.File;
import java.util.Objects;

/* loaded from: classes.dex */
public class BackgroundView extends View {
    private Path accent;
    private final Paint accentPaint;
    private Path card;
    private final Paint cardPaint;
    private boolean darkHour;
    private final Paint dhPaint;
    private float drawerProgress;
    private final Paint scrimPaint;
    private final Paint skyPaint;
    private Bitmap wpLandscape;
    private final Matrix wpMatrix;
    private final Paint wpPaint;
    private Bitmap wpPortrait;

    public BackgroundView(Context context) {
        super(context);
        this.skyPaint = new Paint();
        this.scrimPaint = new Paint();
        this.cardPaint = new Paint(1);
        this.accentPaint = new Paint(1);
        this.drawerProgress = 0.0f;
        this.darkHour = false;
        this.dhPaint = new Paint();
        this.wpPaint = new Paint(2);
        this.wpMatrix = new Matrix();
        Theme theme = Theme.get();
        this.scrimPaint.setColor(-16777216);
        this.cardPaint.setColor(theme.cardFace);
        this.accentPaint.setColor(theme.cardAccent);
    }

    private Bitmap loadWp(String str) {
        int i = 1;
        try {
            File file = new File(getContext().getFilesDir(), str);
            if (!file.exists()) {
                return null;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            while (Math.max(options.outWidth, options.outHeight) / i > 2048) {
                i *= 2;
            }
            BitmapFactory.Options options2 = new BitmapFactory.Options();
            options2.inSampleSize = i;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options2);
        } catch (Throwable th) {
            return null;
        }
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        float f = this.drawerProgress;
        Bitmap bitmap = width > height ? this.wpLandscape : this.wpPortrait;
        if (bitmap != null && bitmap.getWidth() > 0 && bitmap.getHeight() > 0) {
            float width2 = bitmap.getWidth();
            float height2 = bitmap.getHeight();
            float max = Math.max(width / width2, height / height2);
            this.wpMatrix.setScale(max, max);
            this.wpMatrix.postTranslate((width - (width2 * max)) / 2.0f, (height - (height2 * max)) / 2.0f);
            canvas.drawBitmap(bitmap, this.wpMatrix, this.wpPaint);
        }
        if (this.darkHour) {
            canvas.drawRect(0.0f, 0.0f, width, height, this.dhPaint);
        }
        if (f > 0.0f) {
            this.scrimPaint.setAlpha((int) (150.0f * f));
            canvas.drawRect(0.0f, 0.0f, width, height, this.scrimPaint);
            canvas.save();
            canvas.translate(0.0f, (1.0f - f) * height);
            if (this.card != null) {
                canvas.drawPath(this.card, this.cardPaint);
                canvas.drawPath(this.accent, this.accentPaint);
            }
            if (f < 0.995f) {
                float sin = (float) Math.sin(f * 3.141592653589793d);
                float f2 = height * 0.32f;
                float f3 = width * 0.22f;
                Path path = new Path();
                path.moveTo(0.0f, -f2);
                path.lineTo(width, (-f2) - f3);
                path.lineTo(width, (-f3) * 0.3f);
                path.lineTo(0.0f, 0.0f);
                path.close();
                Theme theme = Theme.get();
                Paint paint = new Paint(1);
                paint.setColor(theme.accent);
                paint.setAlpha((int) (sin * 255.0f));
                canvas.drawPath(path, paint);
                Path path2 = new Path();
                float dp = Ui.dp(getContext(), 12.0f);
                path2.moveTo(0.0f, (-f2) - (dp * 2.2f));
                path2.lineTo(width, ((-f2) - f3) - (dp * 2.2f));
                path2.lineTo(width, ((-f2) - f3) - dp);
                path2.lineTo(0.0f, (-f2) - dp);
                path2.close();
                Paint paint2 = new Paint(1);
                paint2.setColor(theme.accentBright);
                paint2.setAlpha((int) (230.0f * sin));
                canvas.drawPath(path2, paint2);
                Paint paint3 = new Paint(1);
                paint3.setColor(-1);
                paint3.setStrokeWidth(Ui.dp(getContext(), 3.0f));
                paint3.setStyle(Paint.Style.STROKE);
                paint3.setAlpha((int) (sin * 255.0f));
                canvas.drawLine(0.0f, -f2, width, (-f2) - f3, paint3);
            }
            canvas.restore();
        }
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (i == 0 || i2 == 0) {
            return;
        }
        Theme theme = Theme.get();
        this.skyPaint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, i2, new int[]{theme.bgTop, theme.bgMid, theme.bgBot}, new float[]{0.0f, 0.55f, 1.0f}, Shader.TileMode.CLAMP));
        float dp = Ui.dp(getContext(), 7.0f);
        this.card = new Path();
        this.card.moveTo(dp, i2 * 0.03f);
        this.card.lineTo(i - dp, i2 * 0.012f);
        this.card.lineTo(i - dp, i2 + 10);
        this.card.lineTo(dp, i2 + 10);
        this.card.close();
        this.accent = new Path();
        this.accent.moveTo(dp, i2 * 0.955f);
        this.accent.lineTo(i - dp, i2 * 0.905f);
        this.accent.lineTo(i - dp, i2 * 0.94f);
        this.accent.lineTo(dp, i2 * 0.99f);
        this.accent.close();
        if (Theme.get().rainbow) {
            this.accentPaint.setShader(new LinearGradient(dp, 0.0f, i - dp, 0.0f, Theme.RAINBOW, (float[]) null, Shader.TileMode.CLAMP));
        } else {
            this.accentPaint.setShader(null);
        }
    }

    public void reloadWallpapers() {
        this.wpPortrait = loadWp("wallpaper_portrait.jpg");
        this.wpLandscape = loadWp("wallpaper_landscape.jpg");
        invalidate();
    }

    public void setDarkHour(boolean z) {
        this.darkHour = z;
        Paint paint = this.dhPaint;
        Objects.requireNonNull(Theme.get());
        paint.setColor(774881146);
        invalidate();
    }

    public void setDrawerProgress(float f) {
        this.drawerProgress = f;
        invalidate();
    }
}
