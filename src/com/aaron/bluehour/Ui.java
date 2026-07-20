package com.aaron.bluehour;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextPaint;
import android.text.TextUtils;

/** Small shared UI helpers + the palette. */
public final class Ui {

    // ---- Palette (P3R-inspired: deep ocean blues, hard cyan accents) ----
    public static final int BG_TOP        = 0xFF040A18;
    public static final int BG_BOTTOM     = 0xFF0A1E3E;
    public static final int ROW_BASE      = 0xC8081226;
    public static final int ROW_PRESSED   = 0xFF2FD4FF;
    public static final int ACCENT        = 0xFF2FD4FF;
    public static final int ACCENT_DEEP   = 0xFF1FB6E8;
    public static final int TEXT_MAIN     = 0xFFF2F7FF;
    public static final int TEXT_ON_CYAN  = 0xFF04182E;
    public static final int TEXT_DIM      = 0x8AFFFFFF;
    public static final int SHADOW        = 0xFF03101F;
    public static final int DARK_HOUR     = 0xFF9BFFC8;

    private static Typeface tfMain;
    private static Typeface tfHeavy;

    private Ui() {}

    public static float dp(Context c, float v) {
        return v * c.getResources().getDisplayMetrics().density;
    }

    public static int dpi(Context c, float v) {
        return Math.round(dp(c, v));
    }

    /** Barlow Condensed Black Italic (bundled, OFL) — the heavy P3R weight. */
    public static Typeface tf(Context c) {
        if (tfMain == null) {
            try {
                tfMain = Typeface.createFromAsset(c.getAssets(),
                        "BarlowCondensed-BlackItalic.ttf");
            } catch (Exception e) {
                tfMain = Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC);
            }
        }
        return tfMain;
    }

    public static Typeface tfUpright(Context c) {
        if (tfHeavy == null) {
            try {
                tfHeavy = Typeface.createFromAsset(c.getAssets(),
                        "BarlowCondensed-Bold.ttf");
            } catch (Exception e) {
                tfHeavy = Typeface.create("sans-serif-condensed", Typeface.BOLD);
            }
        }
        return tfHeavy;
    }

    public static String ellipsize(TextPaint p, String text, float maxW) {
        CharSequence cs = TextUtils.ellipsize(text, p, maxW, TextUtils.TruncateAt.END);
        return cs.toString();
    }

    /** A pressed/base selector out of two skewed parallelogram drawables. */
    public static Drawable skewSelector(Context c, int baseColor, int pressedColor, float skewPx) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed},
                new SkewDrawable(pressedColor, skewPx));
        s.addState(new int[]{},
                new SkewDrawable(baseColor, skewPx));
        return s;
    }
}
