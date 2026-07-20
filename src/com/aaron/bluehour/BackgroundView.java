package com.aaron.bluehour;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

/**
 * v6 backdrop — deliberately plain: a clean royal-blue field (stand-in
 * for Aaron's own wallpaper later), plus the drawer's white card, which
 * is UI, not wallpaper. When user wallpapers land, drawHome() becomes a
 * bitmap draw with defined safe zones for the UI elements.
 */
public class BackgroundView extends View {

    private final Paint skyPaint = new Paint();
    private final Paint scrimPaint = new Paint();
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Path card;
    private Path accent;
    private float drawerProgress = 0f;
    private boolean darkHour = false;
    private final Paint dhPaint = new Paint();

    public BackgroundView(Context c) {
        super(c);
        Theme t = Theme.get();
        scrimPaint.setColor(0xFF000000);
        cardPaint.setColor(t.cardFace);
        accentPaint.setColor(t.cardAccent);
    }

    public void setDrawerProgress(float p) {
        drawerProgress = p;
        invalidate();
    }

    public void setDarkHour(boolean dh) {
        darkHour = dh;
        dhPaint.setColor(Theme.get().darkHourWash); // sickly green wash
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (w == 0 || h == 0) return;

        // plain field, faint vertical falloff so it isn't dead flat.
        // (Only visible if the user has no wallpaper set; still themed.)
        Theme t = Theme.get();
        skyPaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{t.bgTop, t.bgMid, t.bgBot},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));

        float m = Ui.dp(getContext(), 7);
        card = new Path();
        card.moveTo(m, h * 0.030f);
        card.lineTo(w - m, h * 0.012f);
        card.lineTo(w - m, h + 10);
        card.lineTo(m, h + 10);
        card.close();

        accent = new Path();
        accent.moveTo(m, h * 0.955f);
        accent.lineTo(w - m, h * 0.905f);
        accent.lineTo(w - m, h * 0.940f);
        accent.lineTo(m, h * 0.990f);
        accent.close();
        // P4: the drawer accent bar goes full rainbow
        if (Theme.get().rainbow) {
            accentPaint.setShader(new LinearGradient(m, 0, w - m, 0,
                    Theme.RAINBOW, null, Shader.TileMode.CLAMP));
        } else {
            accentPaint.setShader(null);
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth();
        int h = getHeight();
        float p = drawerProgress;

        // home backdrop is the SYSTEM WALLPAPER (windowShowWallpaper) —
        // nothing painted here, so the user's art shows through
        if (darkHour) {
            c.drawRect(0, 0, w, h, dhPaint); // the world goes green at midnight
        }
        if (p > 0f) {
            scrimPaint.setAlpha((int) (p * 150));
            c.drawRect(0, 0, w, h, scrimPaint);
            c.save();
            c.translate(0, (1f - p) * h);
            if (card != null) {
                c.drawPath(card, cardPaint);
                c.drawPath(accent, accentPaint);
            }
            // blue slash wipe riding the card's leading edge mid-transition
            if (p < 0.995f) {
                float wave = (float) Math.sin(p * Math.PI);
                float bandH = h * 0.32f;
                float skew = w * 0.22f;
                Path sweep = new Path();
                sweep.moveTo(0, -bandH);
                sweep.lineTo(w, -bandH - skew);
                sweep.lineTo(w, -skew * 0.3f);
                sweep.lineTo(0, 0);
                sweep.close();
                Theme th = Theme.get();
                Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
                sp.setColor(th.accent);
                sp.setAlpha((int) (wave * 255));
                c.drawPath(sweep, sp);
                // bright echo band above the accent
                Path echo = new Path();
                float eh = Ui.dp(getContext(), 12);
                echo.moveTo(0, -bandH - eh * 2.2f);
                echo.lineTo(w, -bandH - skew - eh * 2.2f);
                echo.lineTo(w, -bandH - skew - eh);
                echo.lineTo(0, -bandH - eh);
                echo.close();
                Paint ep = new Paint(Paint.ANTI_ALIAS_FLAG);
                ep.setColor(th.accentBright);
                ep.setAlpha((int) (wave * 230));
                c.drawPath(echo, ep);
                // white leading hairline for snap
                Paint hp = new Paint(Paint.ANTI_ALIAS_FLAG);
                hp.setColor(0xFFFFFFFF);
                hp.setStrokeWidth(Ui.dp(getContext(), 3f));
                hp.setStyle(Paint.Style.STROKE);
                hp.setAlpha((int) (wave * 255));
                c.drawLine(0, -bandH, w, -bandH - skew, hp);
            }
            c.restore();
        }
    }
}
