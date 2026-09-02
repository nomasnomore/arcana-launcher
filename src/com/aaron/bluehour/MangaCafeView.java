package com.aaron.bluehour;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOverlay;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;

/* loaded from: classes.dex */
public class MangaCafeView extends FrameLayout {
    private final Paint amb;
    private boolean edgeClose;
    private View flash;
    private LinearLayout grid;
    private Host host;
    private float swDownX;
    private float swDownY;

    private final class CoverBg extends Drawable {
        private final Paint p;

        private CoverBg() {
            this.p = new Paint(1);
        }

        @Override // android.graphics.drawable.Drawable
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float dp = Ui.dp(MangaCafeView.this.getContext(), 10.0f);
            this.p.setStyle(Paint.Style.FILL);
            this.p.setColor(-434893818);
            canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, dp, dp, this.p);
            this.p.setStyle(Paint.Style.STROKE);
            this.p.setStrokeWidth(Ui.dp(MangaCafeView.this.getContext(), 1.5f));
            this.p.setColor(MangaCafeView.withA(Theme.get().accent, 102));
            float strokeWidth = this.p.getStrokeWidth();
            canvas.drawRoundRect(bounds.left + strokeWidth, bounds.top + strokeWidth, bounds.right - strokeWidth, bounds.bottom - strokeWidth, dp, dp, this.p);
        }

        @Override // android.graphics.drawable.Drawable
        public int getOpacity() {
            return -3;
        }

        @Override // android.graphics.drawable.Drawable
        public void setAlpha(int i) {
        }

        @Override // android.graphics.drawable.Drawable
        public void setColorFilter(ColorFilter colorFilter) {
        }
    }

    private static final class HeaderView extends View {
        private final Paint p;

        HeaderView(Context context) {
            super(context);
            this.p = new Paint(1);
        }

        private static int withAlpha(int i, int i2) {
            return (i2 << 24) | (16777215 & i);
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            Context context = getContext();
            Theme theme = Theme.get();
            float width = getWidth();
            float height = getHeight();
            float dp = Ui.dp(context, 24.0f);
            this.p.setStyle(Paint.Style.FILL);
            this.p.setColor(withAlpha(theme.accent, 85));
            Path path = new Path();
            float f = 0.32f * height;
            float f2 = 0.44f * height;
            float dp2 = Ui.dp(context, 22.0f);
            path.moveTo(dp + dp2, f);
            path.lineTo(width, f);
            path.lineTo(width - dp2, f + f2);
            path.lineTo(dp, f + f2);
            path.close();
            canvas.drawPath(path, this.p);
            this.p.setTypeface(Ui.display(context));
            this.p.setColor(theme.textLight);
            this.p.setTextSize(Ui.dp(context, 40.0f));
            this.p.setShadowLayer(Ui.dp(context, 4.0f), 0.0f, Ui.dp(context, 2.0f), -872415232);
            canvas.drawText("MANGA CAFÉ", dp, 0.6f * height, this.p);
            this.p.clearShadowLayer();
            this.p.setTypeface(Ui.tfUpright(context));
            this.p.setColor(theme.subtitle);
            this.p.setTextSize(Ui.dp(context, 12.0f));
            canvas.drawText("まんが喫茶  ·  OPEN A VOLUME", dp, height - Ui.dp(context, 12.0f), this.p);
            float dp3 = height - Ui.dp(context, 3.0f);
            this.p.setColor(-12966122);
            canvas.drawRect(dp, dp3, width - dp, height, this.p);
            this.p.setColor(withAlpha(theme.accent, 176));
            canvas.drawRect(dp, dp3 - Ui.dp(context, 1.5f), width - dp, dp3, this.p);
        }
    }

    public interface Host {
        void addFlow();

        Bitmap iconFor(String str);

        String labelFor(String str);

        void launch(String str);

        List<String> readerPkgs();

        void remove(String str);
    }

    public MangaCafeView(Context context) {
        super(context);
        this.amb = new Paint();
        setVisibility(8);
        setClickable(true);
        setWillNotDraw(false);
        build();
    }

    private View addTile() {
        Context context = getContext();
        Theme theme = Theme.get();
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(17);
        linearLayout.setClickable(true);
        linearLayout.setBackground(new CoverBg());
        TextView textView = new TextView(context);
        textView.setTypeface(Ui.display(context));
        textView.setText("+");
        textView.setTextSize(2, 44.0f);
        textView.setTextColor(theme.accentBright);
        textView.setGravity(17);
        linearLayout.addView(textView, new LinearLayout.LayoutParams(-2, -2));
        TextView textView2 = new TextView(context);
        textView2.setTypeface(Ui.tfUpright(context));
        textView2.setText("ADD READER");
        textView2.setTextSize(2, 12.0f);
        textView2.setTextColor(theme.subtitle);
        textView2.setGravity(17);
        linearLayout.addView(textView2, new LinearLayout.LayoutParams(-1, -2));
        linearLayout.setOnClickListener(new View.OnClickListener() { // from class: com.aaron.bluehour.MangaCafeView.4
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (MangaCafeView.this.host != null) {
                    MangaCafeView.this.host.addFlow();
                }
            }
        });
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.addView(linearLayout, new FrameLayout.LayoutParams(-1, Ui.dpi(context, 150.0f)));
        return frameLayout;
    }

    private void build() {
        setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{-199487482, -99612155}));
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(1);
        linearLayout.addView(new HeaderView(getContext()), new LinearLayout.LayoutParams(-1, Ui.dpi(getContext(), 104.0f)));
        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setVerticalScrollBarEnabled(false);
        this.grid = new LinearLayout(getContext());
        this.grid.setOrientation(1);
        this.grid.setPadding(Ui.dpi(getContext(), 22.0f), Ui.dpi(getContext(), 6.0f), Ui.dpi(getContext(), 22.0f), Ui.dpi(getContext(), 40.0f));
        scrollView.addView(this.grid, new ViewGroup.LayoutParams(-1, -2));
        linearLayout.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        addView(linearLayout, new FrameLayout.LayoutParams(-1, -1));
        this.flash = new View(getContext());
        this.flash.setBackgroundColor(-3104);
        this.flash.setAlpha(0.0f);
        addView(this.flash, new FrameLayout.LayoutParams(-1, -1));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void populate() {
        this.grid.removeAllViews();
        List<String> readerPkgs = this.host == null ? null : this.host.readerPkgs();
        int size = readerPkgs == null ? 0 : readerPkgs.size();
        int i = getResources().getConfiguration().orientation == 2 ? 4 : 3;
        int i2 = (((size + 1) + i) - 1) / i;
        int i3 = 0;
        for (int i4 = 0; i4 < i2; i4++) {
            LinearLayout linearLayout = new LinearLayout(getContext());
            linearLayout.setOrientation(0);
            for (int i5 = 0; i5 < i; i5++) {
                View readerTile = i3 < size ? readerTile(readerPkgs.get(i3)) : i3 == size ? addTile() : new View(getContext());
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
                layoutParams.setMargins(Ui.dpi(getContext(), 6.0f), Ui.dpi(getContext(), 6.0f), Ui.dpi(getContext(), 6.0f), Ui.dpi(getContext(), 6.0f));
                linearLayout.addView(readerTile, layoutParams);
                i3++;
            }
            this.grid.addView(linearLayout, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private View readerTile(final String str) {
        Context context = getContext();
        Theme theme = Theme.get();
        FrameLayout frameLayout = new FrameLayout(context);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(17);
        linearLayout.setClickable(true);
        linearLayout.setBackground(new CoverBg());
        linearLayout.setPadding(0, Ui.dpi(context, 16.0f), 0, Ui.dpi(context, 12.0f));
        ImageView imageView = new ImageView(context);
        Bitmap iconFor = this.host == null ? null : this.host.iconFor(str);
        if (iconFor != null) {
            imageView.setImageBitmap(iconFor);
        }
        int dpi = Ui.dpi(context, 62.0f);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dpi, dpi);
        layoutParams.bottomMargin = Ui.dpi(context, 10.0f);
        linearLayout.addView(imageView, layoutParams);
        TextView textView = new TextView(context);
        textView.setTypeface(Ui.display(context));
        textView.setTextSize(2, 14.0f);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setGravity(17);
        String labelFor = this.host == null ? str : this.host.labelFor(str);
        textView.setText(labelFor == null ? str : labelFor.toUpperCase(Locale.getDefault()));
        textView.setTextColor(theme.textLight);
        textView.setPadding(Ui.dpi(context, 6.0f), 0, Ui.dpi(context, 6.0f), 0);
        linearLayout.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        linearLayout.setOnClickListener(new View.OnClickListener() { // from class: com.aaron.bluehour.MangaCafeView.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (MangaCafeView.this.host != null) {
                    MangaCafeView.this.host.launch(str);
                }
                MangaCafeView.this.close();
            }
        });
        linearLayout.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.aaron.bluehour.MangaCafeView.3
            @Override // android.view.View.OnLongClickListener
            public boolean onLongClick(View view) {
                if (MangaCafeView.this.host != null) {
                    MangaCafeView.this.host.remove(str);
                }
                MangaCafeView.this.populate();
                return true;
            }
        });
        frameLayout.addView(linearLayout, new FrameLayout.LayoutParams(-1, Ui.dpi(context, 150.0f)));
        return frameLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int withA(int i, int i2) {
        return (i2 << 24) | (16777215 & i);
    }

    public void close() {
        setPivotX(getWidth() / 2.0f);
        setPivotY(getHeight() / 2.0f);
        this.flash.animate().cancel();
        this.flash.setAlpha(0.5f);
        animate().scaleY(0.02f).setDuration(150L).setInterpolator(new AccelerateInterpolator(1.4f)).withEndAction(new Runnable() { // from class: com.aaron.bluehour.MangaCafeView.1
            @Override // java.lang.Runnable
            public void run() {
                MangaCafeView.this.flash.animate().alpha(0.0f).setDuration(170L).start();
                MangaCafeView.this.animate().scaleX(0.03f).alpha(0.0f).setDuration(190L).setInterpolator(new AccelerateInterpolator(1.7f)).withEndAction(new Runnable() { // from class: com.aaron.bluehour.MangaCafeView.1.1
                    @Override // java.lang.Runnable
                    public void run() {
                        MangaCafeView.this.setVisibility(8);
                        MangaCafeView.this.setScaleX(1.0f);
                        MangaCafeView.this.setScaleY(1.0f);
                        MangaCafeView.this.setAlpha(1.0f);
                        MangaCafeView.this.flash.setAlpha(0.0f);
                    }
                }).start();
            }
        }).start();
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }
        this.amb.setStyle(Paint.Style.FILL);
        this.amb.setShader(new RadialGradient(width / 2.0f, (-height) * 0.1f, height * 0.85f, 872404890, 0, Shader.TileMode.CLAMP));
        canvas.drawRect(0.0f, 0.0f, width, height, this.amb);
        this.amb.setShader(null);
        this.amb.setShader(new RadialGradient(width / 2.0f, height * 0.42f, Math.max(width, height) * 0.78f, 0, -1291845632, Shader.TileMode.CLAMP));
        canvas.drawRect(0.0f, 0.0f, width, height, this.amb);
        this.amb.setShader(null);
    }

    public boolean handleBack() {
        if (getVisibility() != 0) {
            return false;
        }
        close();
        return true;
    }

    @Override // android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case 0:
                this.swDownY = motionEvent.getY();
                this.swDownX = motionEvent.getX();
                this.edgeClose = this.swDownY > ((float) getHeight()) - Ui.dp(getContext(), 96.0f);
                break;
            case 2:
                if (this.edgeClose) {
                    float y = this.swDownY - motionEvent.getY();
                    float abs = Math.abs(motionEvent.getX() - this.swDownX);
                    if (y > Ui.dp(getContext(), 22.0f) && y > abs) {
                        return true;
                    }
                }
                break;
        }
        return false;
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.edgeClose) {
            switch (motionEvent.getActionMasked()) {
                case 1:
                case 3:
                    float y = this.swDownY - motionEvent.getY();
                    this.edgeClose = false;
                    if (y <= Ui.dp(getContext(), 70.0f)) {
                        animate().translationY(0.0f).setDuration(150L).start();
                        return true;
                    }
                    setTranslationY(0.0f);
                    close();
                    return true;
                case 2:
                    float y2 = this.swDownY - motionEvent.getY();
                    if (y2 <= 0.0f) {
                        return true;
                    }
                    setTranslationY(-Math.min(y2 * 0.4f, Ui.dp(getContext(), 70.0f)));
                    return true;
            }
        }
        return super.onTouchEvent(motionEvent);
    }

    public void open() {
        populate();
        setVisibility(0);
        bringToFront();
        setTranslationY(0.0f);
        setPivotY(getHeight() > 0 ? getHeight() / 2.0f : 400.0f);
        setScaleY(0.02f);
        setScaleX(1.0f);
        animate().scaleY(1.0f).setDuration(220L).setInterpolator(new DecelerateInterpolator(1.6f)).start();
        this.flash.setAlpha(0.55f);
        this.flash.animate().alpha(0.0f).setDuration(280L).start();
    }

    public void refresh() {
        if (getVisibility() == 0) {
            populate();
        }
    }

    public void setHost(Host host) {
        this.host = host;
    }
}
