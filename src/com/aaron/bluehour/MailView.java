package com.aaron.bluehour;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * The MAIL panel: a Persona-styled full-screen overlay with two modes.
 *   LIST   — big slashed "MAIL" header, a today banner, and a scrolling list
 *            of notifications (app icon + name + one-line preview).
 *   DETAIL — a read view with the app portrait, a "From" line, the subject in
 *            a light bar, and the full body; tapping it opens the real app.
 * Everything recolours with the current Hour.
 */
public class MailView extends FrameLayout {

    public interface Host {
        Bitmap iconFor(String pkg);
        String labelFor(String pkg);
        void openItem(NotifService.Item it);
    }

    private Host host;
    private ScrollView listPane;
    private LinearLayout listBox;
    private FrameLayout detailPane;
    private Banner banner;
    private float swDownY, swDownX;
    private boolean edgeClose;

    public MailView(Context c) {
        super(c);
        setVisibility(GONE);
        setClickable(true);   // swallow touches so home doesn't get them
        buildListPane();
        buildDetailShell();
    }

    public void setHost(Host h) { host = h; }

    // --------------------------------------------------------------- list pane

    private void buildListPane() {
        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(LinearLayout.VERTICAL);

        banner = new Banner(getContext());
        col.addView(banner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dpi(getContext(), 138)));

        listPane = new ScrollView(getContext());
        listPane.setVerticalScrollBarEnabled(false);
        listBox = new LinearLayout(getContext());
        listBox.setOrientation(LinearLayout.VERTICAL);
        listBox.setPadding(Ui.dpi(getContext(), 16), Ui.dpi(getContext(), 4),
                Ui.dpi(getContext(), 16), Ui.dpi(getContext(), 40));
        listPane.addView(listBox, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        col.addView(listPane, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void buildDetailShell() {
        detailPane = new FrameLayout(getContext());
        detailPane.setVisibility(GONE);
        detailPane.setClickable(true);
        addView(detailPane, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /** Themed dark backdrop so the wallpaper dims behind the mail. */
    private void applyBackdrop() {
        Theme t = Theme.get();
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{withA(t.bgTop, 0xF4), withA(t.bgBot, 0xFA)});
        setBackground(g);
    }

    // ------------------------------------------------------------------ open

    public void open() {
        applyBackdrop();
        showList();
        setVisibility(VISIBLE);
        bringToFront();
        setAlpha(0f);
        setTranslationY(-Ui.dp(getContext(), 24));
        animate().alpha(1f).translationY(0f).setDuration(160).start();
    }

    public void refresh() {
        if (getVisibility() == VISIBLE && detailPane.getVisibility() != VISIBLE) {
            showList();
        }
    }

    private void showList() {
        detailPane.setVisibility(GONE);
        listPane.setVisibility(VISIBLE);
        listPane.scrollTo(0, 0);
        List<NotifService.Item> feed = NotifService.feed();
        banner.setCount(feed.size());
        listBox.removeAllViews();

        if (feed.isEmpty()) {
            addEmptyLabel();
            return;
        }
        addClearAllHeader();
        for (int i = 0; i < feed.size(); i++) {
            listBox.addView(makeRow(feed.get(i)));
        }
    }

    private void addEmptyLabel() {
        TextView empty = new TextView(getContext());
        empty.setTypeface(Ui.display(getContext()));
        empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        empty.setTextColor(Theme.get().subtitle);
        empty.setText("NO NEW MAIL");
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(0, Ui.dpi(getContext(), 60), 0, 0);
        listBox.addView(empty, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addClearAllHeader() {
        Context ctx = getContext();
        Theme t = Theme.get();
        TextView clear = new TextView(ctx);
        clear.setTypeface(Ui.tfUpright(ctx));
        clear.setText("CLEAR ALL  ✕");
        clear.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        clear.setTextColor(t.accentText());
        clear.setGravity(Gravity.CENTER);
        clear.setClickable(true);
        clear.setBackground(Ui.skewSelector(ctx, t.accent, t.accentBright, Ui.dp(ctx, 8)));
        clear.setPadding(Ui.dpi(ctx, 16), Ui.dpi(ctx, 7),
                Ui.dpi(ctx, 16), Ui.dpi(ctx, 7));
        clear.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                NotifService.clearAll();
                listBox.removeAllViews();
                banner.setCount(0);
                addEmptyLabel();
            }
        });
        LinearLayout wrap = new LinearLayout(ctx);
        wrap.setGravity(Gravity.END);
        wrap.addView(clear, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dpi(ctx, 4);
        lp.bottomMargin = Ui.dpi(ctx, 2);
        listBox.addView(wrap, lp);
    }

    private View makeRow(final NotifService.Item it) {
        Context ctx = getContext();
        Theme t = Theme.get();
        final int skew = Ui.dpi(ctx, 9);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClickable(true);
        row.setBackground(Ui.skewSelector(ctx, 0xE60A1424, t.accent, skew));
        row.setPadding(Ui.dpi(ctx, 16), Ui.dpi(ctx, 11),
                Ui.dpi(ctx, 16), Ui.dpi(ctx, 11));

        ImageView iv = new ImageView(ctx);
        Bitmap ic = host == null ? null : host.iconFor(it.pkg);
        if (ic != null) iv.setImageBitmap(ic);
        int is = Ui.dpi(ctx, 38);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(is, is);
        ilp.rightMargin = Ui.dpi(ctx, 14);
        row.addView(iv, ilp);

        LinearLayout texts = new LinearLayout(ctx);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView name = new TextView(ctx);
        name.setTypeface(Ui.display(ctx));
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        String label = host == null ? it.pkg : host.labelFor(it.pkg);
        name.setText(label == null ? it.pkg : label.toUpperCase(Locale.getDefault()));
        name.setTextColor(rowText(t.textLight, t));

        TextView preview = new TextView(ctx);
        preview.setTypeface(Ui.tfUpright(ctx));
        preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        preview.setSingleLine(true);
        preview.setEllipsize(TextUtils.TruncateAt.END);
        String prev = it.title != null && it.title.length() > 0 ? it.title : it.text;
        if (prev == null) prev = "";
        preview.setText(prev);
        preview.setTextColor(rowText(t.subtitle, t));

        texts.addView(name, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        texts.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // dismiss ✕ — clears this one (from the phone's shade too)
        final LinearLayout rowRef = row;
        TextView xb = new TextView(ctx);
        xb.setTypeface(Ui.tfUpright(ctx));
        xb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        xb.setText("✕");
        xb.setTextColor(rowText(t.subtitle, t));
        xb.setClickable(true);
        xb.setPadding(Ui.dpi(ctx, 12), Ui.dpi(ctx, 10),
                Ui.dpi(ctx, 6), Ui.dpi(ctx, 10));
        xb.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                NotifService.clearKey(it.key);
                listBox.removeView(rowRef);      // instant feedback
                if (banner != null) banner.bumpDown();
            }
        });
        row.addView(xb, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        row.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) { showDetail(it); }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dpi(ctx, 9);
        row.setLayoutParams(lp);
        return row;
    }

    // Rows flip to accentText when pressed (accent fill), else the given colour.
    private static ColorStateList rowText(int normal, Theme t) {
        return new ColorStateList(
                new int[][]{{android.R.attr.state_pressed}, {}},
                new int[]{t.accentText(), normal});
    }

    // ---------------------------------------------------------------- detail

    private void showDetail(final NotifService.Item it) {
        Context ctx = getContext();
        Theme t = Theme.get();
        detailPane.removeAllViews();

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);

        String label = host == null ? it.pkg : host.labelFor(it.pkg);
        if (label == null) label = it.pkg;
        Bitmap ic = host == null ? null : host.iconFor(it.pkg);

        Portrait head = new Portrait(ctx, ic,
                label.toUpperCase(Locale.getDefault()),
                it.title, it.when);
        col.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dpi(ctx, 188)));

        ScrollView bodyScroll = new ScrollView(ctx);
        bodyScroll.setVerticalScrollBarEnabled(false);
        TextView body = new TextView(ctx);
        body.setTypeface(Ui.tfUpright(ctx));
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        body.setTextColor(t.textLight);
        body.setLineSpacing(Ui.dp(ctx, 4), 1.05f);
        String txt = it.text != null && it.text.length() > 0
                ? it.text
                : (it.title != null ? it.title : "");
        body.setText(txt);
        body.setPadding(Ui.dpi(ctx, 24), Ui.dpi(ctx, 20),
                Ui.dpi(ctx, 24), Ui.dpi(ctx, 24));
        bodyScroll.addView(body, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        col.addView(bodyScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // footer: OPEN action (fires the notification's intent) + back hint
        TextView open = new TextView(ctx);
        open.setTypeface(Ui.display(ctx));
        open.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        open.setText("  OPEN IN APP  ▸");
        open.setTextColor(new ColorStateList(
                new int[][]{{android.R.attr.state_pressed}, {}},
                new int[]{t.accentText(), t.accentText()}));
        open.setGravity(Gravity.CENTER);
        open.setClickable(true);
        open.setBackground(Ui.skewSelector(ctx, t.accent, t.accentBright, Ui.dp(ctx, 10)));
        open.setPadding(Ui.dpi(ctx, 20), Ui.dpi(ctx, 14),
                Ui.dpi(ctx, 20), Ui.dpi(ctx, 14));
        open.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                if (host != null) host.openItem(it);
            }
        });
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        olp.setMargins(Ui.dpi(ctx, 20), Ui.dpi(ctx, 6),
                Ui.dpi(ctx, 20), Ui.dpi(ctx, 8));
        col.addView(open, olp);

        TextView back = new TextView(ctx);
        back.setTypeface(Ui.tfUpright(ctx));
        back.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        back.setText("◂ BACK TO MAIL");
        back.setTextColor(t.subtitle);
        back.setGravity(Gravity.CENTER);
        back.setClickable(true);
        back.setPadding(0, Ui.dpi(ctx, 4), 0, Ui.dpi(ctx, 18));
        back.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) { showList(); }
        });
        col.addView(back, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        detailPane.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        listPane.setVisibility(GONE);
        detailPane.setVisibility(VISIBLE);
        detailPane.setAlpha(0f);
        detailPane.setTranslationX(Ui.dp(ctx, 30));
        detailPane.animate().alpha(1f).translationX(0f).setDuration(150).start();
    }

    // ------------------------------------------------------------- back / close

    /** true if we handled it (went list<-detail, or closed the panel). */
    public boolean handleBack() {
        if (getVisibility() != VISIBLE) return false;
        if (detailPane.getVisibility() == VISIBLE) {
            showList();
            return true;
        }
        close();
        return true;
    }

    public void close() {
        animate().alpha(0f).translationY(-Ui.dp(getContext(), 20))
                .setDuration(140).withEndAction(new Runnable() {
            @Override public void run() { setVisibility(GONE); }
        }).start();
    }

    // ---- swipe up from the bottom edge to dismiss the whole panel ----

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

    private static int withA(int color, int a) {
        return (a << 24) | (color & 0x00FFFFFF);
    }

    // ================================================================ headers

    /** The list header: the big slashed MAIL wordmark + today banner + close. */
    private static final class Banner extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int count = 0;

        Banner(Context c) { super(c); }

        void setCount(int n) { count = n; invalidate(); }

        void bumpDown() { if (count > 0) count--; invalidate(); }

        @Override
        protected void onDraw(Canvas c) {
            Context ctx = getContext();
            Theme t = Theme.get();
            float w = getWidth(), h = getHeight();
            float pad = Ui.dp(ctx, 16);

            // slash underlay behind the wordmark
            p.setStyle(Paint.Style.FILL);
            p.setColor(withA(t.accent, 0x55));
            Path slash = new Path();
            float sy = h * 0.30f, sh = h * 0.44f, sk = Ui.dp(ctx, 22);
            slash.moveTo(pad + sk, sy);
            slash.lineTo(w, sy);
            slash.lineTo(w - sk, sy + sh);
            slash.lineTo(pad, sy + sh);
            slash.close();
            c.drawPath(slash, p);

            // MAIL wordmark
            p.setTypeface(Ui.display(ctx));
            p.setColor(t.textLight);
            p.setTextSize(Ui.dp(ctx, 62));
            p.setShadowLayer(Ui.dp(ctx, 4), 0, Ui.dp(ctx, 2), 0xCC000000);
            c.drawText("MAIL", pad, h * 0.62f, p);
            p.clearShadowLayer();

            // today banner (accent parallelogram) with count
            p.setColor(t.accent);
            float bw = Ui.dp(ctx, 176), bh = Ui.dp(ctx, 26), bk = Ui.dp(ctx, 10);
            float bx = w - pad - bw, by = h * 0.52f;
            Path ban = new Path();
            ban.moveTo(bx + bk, by);
            ban.lineTo(bx + bw, by);
            ban.lineTo(bx + bw - bk, by + bh);
            ban.lineTo(bx, by + bh);
            ban.close();
            c.drawPath(ban, p);
            p.setTypeface(Ui.tfUpright(ctx));
            p.setColor(t.accentText());
            p.setTextSize(Ui.dp(ctx, 13));
            String label = count == 1 ? "TODAY'S  ·  1 MESSAGE"
                    : "TODAY'S  ·  " + count + " MESSAGES";
            p.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fm = p.getFontMetrics();
            float ty = by + bh / 2f - (fm.ascent + fm.descent) / 2f;
            c.drawText(label, bx + bw / 2f, ty, p);
            p.setTextAlign(Paint.Align.LEFT);

            // close hint bottom-right
            p.setColor(t.subtitle);
            p.setTextSize(Ui.dp(ctx, 12));
            p.setTextAlign(Paint.Align.RIGHT);
            c.drawText("SWIPE ↑ TO CLOSE", w - pad, h - Ui.dp(ctx, 6), p);
            p.setTextAlign(Paint.Align.LEFT);

            // baseline rule
            p.setStyle(Paint.Style.FILL);
            p.setColor(withA(t.accent, 0xCC));
            c.drawRect(pad, h - Ui.dp(ctx, 2.5f), w - pad, h, p);
        }
    }

    /** The detail header: portrait + From line + subject bar. */
    private static final class Portrait extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Bitmap icon;
        private final String name;
        private final String subject;
        private final long when;

        Portrait(Context c, Bitmap icon, String name, String subject, long when) {
            super(c);
            this.icon = icon;
            this.name = name == null ? "" : name;
            this.subject = subject == null ? "" : subject;
            this.when = when;
        }

        @Override
        protected void onDraw(Canvas c) {
            Context ctx = getContext();
            Theme t = Theme.get();
            float w = getWidth(), h = getHeight();
            float pad = Ui.dp(ctx, 20);

            // big faint portrait icon on the left
            if (icon != null) {
                float ps = Ui.dp(ctx, 120);
                RectF dst = new RectF(pad, h * 0.10f, pad + ps, h * 0.10f + ps);
                p.setAlpha(60);
                c.drawBitmap(icon, null, dst, p);
                p.setAlpha(255);
            }

            // accent slash across the top
            p.setStyle(Paint.Style.FILL);
            p.setColor(withA(t.accent, 0x66));
            Path slash = new Path();
            float sk = Ui.dp(ctx, 20);
            slash.moveTo(w * 0.30f + sk, 0);
            slash.lineTo(w, 0);
            slash.lineTo(w, Ui.dp(ctx, 44));
            slash.lineTo(w * 0.30f, Ui.dp(ctx, 44));
            slash.close();
            c.drawPath(slash, p);

            // crisp small icon + FROM line
            float ix = pad, iy = Ui.dp(ctx, 26), isz = Ui.dp(ctx, 40);
            if (icon != null) {
                RectF r = new RectF(ix, iy, ix + isz, iy + isz);
                c.drawBitmap(icon, null, r, p);
            }
            float tx = ix + (icon != null ? isz + Ui.dp(ctx, 14) : 0);
            p.setTypeface(Ui.tfUpright(ctx));
            p.setColor(t.subtitle);
            p.setTextSize(Ui.dp(ctx, 13));
            c.drawText("FROM", tx, iy + Ui.dp(ctx, 12), p);
            p.setTypeface(Ui.display(ctx));
            p.setColor(t.textLight);
            p.setTextSize(Ui.dp(ctx, 26));
            String nm = Ui.ellipsize(new android.text.TextPaint(p), name, w - tx - pad);
            c.drawText(nm, tx, iy + Ui.dp(ctx, 36), p);

            // subject in a light bar
            float by = Ui.dp(ctx, 108), bh = Ui.dp(ctx, 40), bk = Ui.dp(ctx, 12);
            p.setStyle(Paint.Style.FILL);
            p.setColor(t.cardFace);
            Path bar = new Path();
            bar.moveTo(pad + bk, by);
            bar.lineTo(w - pad, by);
            bar.lineTo(w - pad - bk, by + bh);
            bar.lineTo(pad, by + bh);
            bar.close();
            c.drawPath(bar, p);
            p.setColor(t.dateInk);
            p.setTypeface(Ui.tfUpright(ctx));
            p.setTextSize(Ui.dp(ctx, 17));
            String subj = Ui.ellipsize(new android.text.TextPaint(p), subject,
                    w - pad * 2 - bk - Ui.dp(ctx, 8));
            Paint.FontMetrics fm = p.getFontMetrics();
            float sty = by + bh / 2f - (fm.ascent + fm.descent) / 2f;
            c.drawText(subj, pad + bk, sty, p);

            // time, small, under the bar
            if (when > 0) {
                p.setColor(t.subtitle);
                p.setTypeface(Ui.tfUpright(ctx));
                p.setTextSize(Ui.dp(ctx, 12));
                String ts = new SimpleDateFormat("EEE  h:mm a", Locale.getDefault())
                        .format(new java.util.Date(when));
                c.drawText(ts.toUpperCase(Locale.getDefault()), pad + bk, by + bh
                        + Ui.dp(ctx, 16), p);
            }
        }
    }
}
