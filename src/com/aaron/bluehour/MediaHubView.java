package com.aaron.bluehour;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * The Midnight Channel — the tablet media hub. Opens like flipping on a retro
 * TV (a bright line that expands with a static flash and scanlines), then shows
 * the user's attached streaming apps as "channels" to tap into. Full-colour app
 * icons act as the channel logos. Theme-tinted.
 */
public class MediaHubView extends FrameLayout {

    public interface Host {
        List<String> mediaPkgs();
        Bitmap iconFor(String pkg);
        String labelFor(String pkg);
        void launch(String pkg);
        void addFlow();
        void remove(String pkg);
    }

    private Host host;
    private LinearLayout grid;      // vertical stack of tile rows
    private View flash;             // white power-on flash
    private final Paint crt = new Paint();
    private float scan = 0f;        // 0..1 scan-bar position
    private ValueAnimator scanAnim;
    private float swDownY, swDownX;
    private boolean edgeClose;

    public MediaHubView(Context c) {
        super(c);
        setVisibility(GONE);
        setClickable(true);
        setWillNotDraw(false);
        build();
    }

    public void setHost(Host h) { host = h; }

    private void build() {
        // opaque dark base so the CRT effect reads over any wallpaper
        setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xF404070E, 0xFA02040A}));

        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(LinearLayout.VERTICAL);

        // header
        HeaderView header = new HeaderView(getContext());
        col.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dpi(getContext(), 96)));

        ScrollView sv = new ScrollView(getContext());
        sv.setVerticalScrollBarEnabled(false);
        grid = new LinearLayout(getContext());
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(Ui.dpi(getContext(), 22), Ui.dpi(getContext(), 6),
                Ui.dpi(getContext(), 22), Ui.dpi(getContext(), 40));
        sv.addView(grid, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        col.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // power-on flash
        flash = new View(getContext());
        flash.setBackgroundColor(0xFFFFFFFF);
        flash.setAlpha(0f);
        addView(flash, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    // -------------------------------------------------------------- open/close

    public void open() {
        populate();
        setVisibility(VISIBLE);
        bringToFront();
        // channel-flip: collapse to a bright line, then snap open with a flash
        setTranslationY(0f);
        setPivotY(getHeight() > 0 ? getHeight() / 2f : 400f);
        setScaleY(0.015f);
        setScaleX(1f);
        animate().scaleY(1f).setDuration(200)
                .setInterpolator(new DecelerateInterpolator(1.6f)).start();
        flash.setAlpha(0.9f);
        flash.animate().alpha(0f).setDuration(240).start();
        startScan();
    }

    public boolean handleBack() {
        if (getVisibility() != VISIBLE) return false;
        close();
        return true;
    }

    /** CRT power-off: the picture snaps to a bright horizontal line, then that
     *  line collapses to a point of light and fades — like switching off an
     *  old TV. All exits (swipe, back) run through here. */
    public void close() {
        stopScan();
        setPivotX(getWidth() / 2f);
        setPivotY(getHeight() / 2f);
        flash.animate().cancel();
        flash.setAlpha(0.7f);   // phosphor glow as it collapses
        animate().scaleY(0.014f).setDuration(140)
                .setInterpolator(new android.view.animation.AccelerateInterpolator(1.4f))
                .withEndAction(new Runnable() {
            @Override public void run() {
                flash.animate().alpha(0f).setDuration(170).start();
                animate().scaleX(0.02f).alpha(0f).setDuration(180)
                        .setInterpolator(new android.view.animation.AccelerateInterpolator(1.7f))
                        .withEndAction(new Runnable() {
                    @Override public void run() {
                        setVisibility(GONE);
                        setScaleX(1f);
                        setScaleY(1f);
                        setAlpha(1f);
                        flash.setAlpha(0f);
                    }
                }).start();
            }
        }).start();
    }

    public void refresh() {
        if (getVisibility() == VISIBLE) populate();
    }

    // ---- swipe up from the bottom edge to dismiss ----

    @Override
    public boolean onInterceptTouchEvent(android.view.MotionEvent e) {
        switch (e.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
                swDownY = e.getY();
                swDownX = e.getX();
                edgeClose = swDownY > getHeight() - Ui.dp(getContext(), 96);
                break;
            case android.view.MotionEvent.ACTION_MOVE:
                if (edgeClose) {
                    float up = swDownY - e.getY();
                    float dx = Math.abs(e.getX() - swDownX);
                    if (up > Ui.dp(getContext(), 22) && up > dx) return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent e) {
        if (edgeClose) {
            switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_MOVE: {
                    float up = swDownY - e.getY();
                    if (up > 0) setTranslationY(-Math.min(up * 0.4f,
                            Ui.dp(getContext(), 70)));
                    return true;
                }
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL: {
                    float up = swDownY - e.getY();
                    edgeClose = false;
                    if (up > Ui.dp(getContext(), 70)) {
                        setTranslationY(0f);
                        close();
                    } else {
                        animate().translationY(0f).setDuration(150).start();
                    }
                    return true;
                }
            }
        }
        return super.onTouchEvent(e);
    }

    // ---------------------------------------------------------------- content

    private void populate() {
        grid.removeAllViews();
        List<String> pkgs = host == null ? null : host.mediaPkgs();
        int n = pkgs == null ? 0 : pkgs.size();
        boolean land = getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        int cols = land ? 4 : 3;

        // build the list of tile views: each channel + a trailing "+ ADD"
        int total = n + 1;
        int rows = (total + cols - 1) / cols;
        int idx = 0;
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int cIdx = 0; cIdx < cols; cIdx++) {
                View tile;
                if (idx < n) {
                    tile = channelTile(pkgs.get(idx));
                } else if (idx == n) {
                    tile = addTile();
                } else {
                    tile = new View(getContext()); // spacer to keep widths even
                }
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(Ui.dpi(getContext(), 6), Ui.dpi(getContext(), 6),
                        Ui.dpi(getContext(), 6), Ui.dpi(getContext(), 6));
                row.addView(tile, lp);
                idx++;
            }
            grid.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private View channelTile(final String pkg) {
        Context ctx = getContext();
        Theme t = Theme.get();
        FrameLayout tile = new FrameLayout(ctx);
        LinearLayout inner = new LinearLayout(ctx);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setClickable(true);
        inner.setBackground(new ChannelBg());
        inner.setPadding(0, Ui.dpi(ctx, 16), 0, Ui.dpi(ctx, 12));

        ImageView iv = new ImageView(ctx);           // full-colour logo
        Bitmap ic = host == null ? null : host.iconFor(pkg);
        if (ic != null) iv.setImageBitmap(ic);
        int is = Ui.dpi(ctx, 62);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(is, is);
        ilp.bottomMargin = Ui.dpi(ctx, 10);
        inner.addView(iv, ilp);

        TextView name = new TextView(ctx);
        name.setTypeface(Ui.display(ctx));
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setGravity(Gravity.CENTER);
        String label = host == null ? pkg : host.labelFor(pkg);
        name.setText(label == null ? pkg : label.toUpperCase(Locale.getDefault()));
        name.setTextColor(t.textLight);
        name.setPadding(Ui.dpi(ctx, 6), 0, Ui.dpi(ctx, 6), 0);
        inner.addView(name, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        inner.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                if (host != null) host.launch(pkg);
                close();
            }
        });
        inner.setOnLongClickListener(new OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                if (host != null) host.remove(pkg);
                populate();
                return true;
            }
        });
        tile.addView(inner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dpi(ctx, 150)));
        return tile;
    }

    private View addTile() {
        Context ctx = getContext();
        Theme t = Theme.get();
        LinearLayout inner = new LinearLayout(ctx);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setClickable(true);
        inner.setBackground(new ChannelBg());

        TextView plus = new TextView(ctx);
        plus.setTypeface(Ui.display(ctx));
        plus.setText("+");
        plus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44);
        plus.setTextColor(t.accentBright);
        plus.setGravity(Gravity.CENTER);
        inner.addView(plus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView cap = new TextView(ctx);
        cap.setTypeface(Ui.tfUpright(ctx));
        cap.setText("ADD CHANNEL");
        cap.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        cap.setTextColor(t.subtitle);
        cap.setGravity(Gravity.CENTER);
        inner.addView(cap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        inner.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                if (host != null) host.addFlow();
            }
        });
        FrameLayout tile = new FrameLayout(ctx);
        tile.addView(inner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dpi(ctx, 150)));
        return tile;
    }

    // ------------------------------------------------------------ CRT overlay

    private void startScan() {
        if (scanAnim != null) return;
        scanAnim = ValueAnimator.ofFloat(0f, 1f);
        scanAnim.setDuration(2600);
        scanAnim.setRepeatCount(ValueAnimator.INFINITE);
        scanAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                scan = ((Float) a.getAnimatedValue()).floatValue();
                invalidate();
            }
        });
        scanAnim.start();
    }

    private void stopScan() {
        if (scanAnim != null) { scanAnim.cancel(); scanAnim = null; }
    }

    @Override
    protected void dispatchDraw(Canvas c) {
        super.dispatchDraw(c);
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        // scanlines
        crt.setStyle(Paint.Style.FILL);
        crt.setShader(null);
        crt.setColor(0x14000000);
        float step = Ui.dp(getContext(), 3);
        for (float y = 0; y < h; y += step) {
            c.drawRect(0, y, w, y + step * 0.5f, crt);
        }
        // travelling scan bar
        float by = scan * h;
        crt.setShader(new LinearGradient(0, by - Ui.dp(getContext(), 40), 0, by,
                0x00FFFFFF, 0x18FFFFFF, Shader.TileMode.CLAMP));
        c.drawRect(0, by - Ui.dp(getContext(), 40), w, by, crt);
        crt.setShader(null);
        // vignette
        crt.setShader(new RadialGradient(w / 2f, h / 2f, Math.max(w, h) * 0.72f,
                0x00000000, 0x99000000, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, crt);
        crt.setShader(null);
    }

    /** The dark, faintly-outlined face of one channel tile. */
    private final class ChannelBg extends android.graphics.drawable.Drawable {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        @Override public void draw(Canvas c) {
            android.graphics.Rect b = getBounds();
            float r = Ui.dp(getContext(), 10);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xE6060C16);
            c.drawRoundRect(b.left, b.top, b.right, b.bottom, r, r, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Ui.dp(getContext(), 1.5f));
            p.setColor(withA(Theme.get().accent, 0x66));
            float in = p.getStrokeWidth();
            c.drawRoundRect(b.left + in, b.top + in, b.right - in, b.bottom - in,
                    r, r, p);
        }
        @Override public void setAlpha(int a) {}
        @Override public void setColorFilter(android.graphics.ColorFilter f) {}
        @Override public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }

    private static int withA(int color, int a) {
        return (a << 24) | (color & 0x00FFFFFF);
    }

    // ---------------------------------------------------------------- header

    private static final class HeaderView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        HeaderView(Context c) { super(c); }

        @Override
        protected void onDraw(Canvas c) {
            Context ctx = getContext();
            Theme t = Theme.get();
            float w = getWidth(), h = getHeight();
            float pad = Ui.dp(ctx, 24);

            // accent slash under the wordmark
            p.setStyle(Paint.Style.FILL);
            p.setColor(withAlpha(t.accent, 0x55));
            android.graphics.Path slash = new android.graphics.Path();
            float sy = h * 0.34f, sh = h * 0.46f, sk = Ui.dp(ctx, 22);
            slash.moveTo(pad + sk, sy);
            slash.lineTo(w, sy);
            slash.lineTo(w - sk, sy + sh);
            slash.lineTo(pad, sy + sh);
            slash.close();
            c.drawPath(slash, p);

            p.setTypeface(Ui.display(ctx));
            p.setColor(t.textLight);
            p.setTextSize(Ui.dp(ctx, 40));
            p.setShadowLayer(Ui.dp(ctx, 4), 0, Ui.dp(ctx, 2), 0xCC000000);
            c.drawText("MIDNIGHT CHANNEL", pad, h * 0.60f, p);
            p.clearShadowLayer();

            p.setTypeface(Ui.tfUpright(ctx));
            p.setColor(t.subtitle);
            p.setTextSize(Ui.dp(ctx, 12));
            c.drawText("真夜中チャンネル  ·  TAP A CHANNEL", pad, h - Ui.dp(ctx, 10), p);

            p.setColor(withAlpha(t.accent, 0xCC));
            c.drawRect(pad, h - Ui.dp(ctx, 2.5f), w - pad, h, p);
        }

        private static int withAlpha(int color, int a) {
            return (a << 24) | (color & 0x00FFFFFF);
        }
    }
}
