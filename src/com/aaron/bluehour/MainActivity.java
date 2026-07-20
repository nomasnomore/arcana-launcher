package com.aaron.bluehour;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Dialog;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private FrameLayout root;
    private BackgroundView bg;
    private FrameLayout homeLayer;
    private LinearLayout drawerLayer;
    private AppsListView listView;
    private AppsAdapter adapter;
    private EditText searchEdit;
    private LinearLayout dockRow;   // v4: category word stack
    private LinearLayout quickRow;  // v4: glyph quick dock
    private ClockView timeText;
    private TextView dayText, dateText, phaseText, defaultHint,
            drawerHeader, battText;
    private LinearLayout battBox;
    private View ruleView;
    private TextView weatherText;
    private CRTView crtView;
    private HalftoneView halftoneView;
    private boolean wasStopped = false;
    private CardButton walletCard;
    private boolean pickWallet = false;
    private boolean lastDh = false;
    private PlayerView playerView;
    private android.media.session.MediaController mediaController;
    private android.media.session.MediaSessionManager msm;
    private View flash;

    private Prefs prefs;
    private List<AppEntry> apps = new ArrayList<AppEntry>();
    private final java.util.HashMap<String, Integer> usesMap =
            new java.util.HashMap<String, Integer>();

    private boolean drawerOpen = false;
    private float drawerPos = 0f;
    private ValueAnimator drawerAnim;
    private String currentCat = null; // null = ALL APPS
    private String addModeCat = null; // non-null = add/remove apps mode
    private int pickSlot = -1;        // >=0 = picking an app for a dock slot
    private FrameLayout searchRowV;

    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { updateClock(); }
    };
    private final BroadcastReceiver notifReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { updateDots(); }
    };
    private final BroadcastReceiver pkgReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            // scrub uninstalled apps from categories, dock, recents, hidden
            if (Intent.ACTION_PACKAGE_REMOVED.equals(i.getAction())
                    && !i.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                    && i.getData() != null) {
                prefs.removePkgEverywhere(i.getData().getSchemeSpecificPart());
                buildQuickRow();
            }
            loadApps();
        }
    };

    // ------------------------------------------------------------------ setup

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        Theme.set(prefs.intVal("theme")); // apply saved theme before building views
        setupWindow();

        root = new FrameLayout(this);
        bg = new BackgroundView(this);
        root.addView(bg, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        buildHomeLayer();
        buildDrawerLayer();
        // paint categories immediately from saved data — don't wait for the
        // app enumeration; dots fill in once apps finish loading
        rebuildDock();

        flash = new View(this);
        flash.setBackgroundColor(0xFFFFFFFF);
        flash.setAlpha(0f);
        flash.setClickable(false);
        root.addView(flash, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // halftone comic texture (Red Hour) — under the CRT, over the UI
        halftoneView = new HalftoneView(this);
        root.addView(halftoneView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        halftoneView.setOn(Theme.get().id == 2);

        // CRT overlay sits on top of everything; passes touches through
        crtView = new CRTView(this);
        root.addView(crtView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        applyCrt();

        setContentView(root);

        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                applyInsets(insets);
                return insets;
            }
        });

        // park the drawer off-screen once we know our height
        drawerLayer.setVisibility(View.INVISIBLE);
        root.post(new Runnable() {
            @Override
            public void run() {
                drawerLayer.setTranslationY(root.getHeight());
                drawerLayer.setVisibility(View.VISIBLE);
            }
        });

        loadApps();
    }

    private void setupWindow() {
        Window w = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
        } else {
            w.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            w.setAttributes(lp);
        }
    }

    private void applyInsets(WindowInsets insets) {
        int top, bottom;
        if (Build.VERSION.SDK_INT >= 30) {
            android.graphics.Insets sys = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            int ime = insets.getInsets(WindowInsets.Type.ime()).bottom;
            top = sys.top;
            bottom = Math.max(sys.bottom, ime);
        } else {
            top = insets.getSystemWindowInsetTop();
            bottom = insets.getSystemWindowInsetBottom();
        }
        homeLayer.setPadding(Ui.dpi(this, 22), top + Ui.dpi(this, 14),
                Ui.dpi(this, 22), bottom + Ui.dpi(this, 10));
        drawerLayer.setPadding(0, top + Ui.dpi(this, 10), 0, bottom);
    }

    // ------------------------------------------------------------- home layer

    private void buildHomeLayer() {
        homeLayer = new FrameLayout(this);
        // let children (esp. the big italic clock) draw outside these bounds
        homeLayer.setClipChildren(false);
        homeLayer.setClipToPadding(false);
        root.setClipChildren(false);
        root.addView(homeLayer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ---- clock block: straight & huge, title-screen style ----
        LinearLayout clockBox = new LinearLayout(this);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockBox.setClipChildren(false);
        clockBox.setClipToPadding(false);
        clockBox.setClickable(true);
        clockBox.setLongClickable(true);

        // custom-drawn clock: measures its own ink, cannot clip
        timeText = new ClockView(this);
        clockBox.addView(timeText);

        // day on a black slash ribbon
        dayText = new TextView(this);
        dayText.setTypeface(Ui.tf(this));
        dayText.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.dp(this, 30));
        dayText.setTextColor(0xFFF2F5FA);
        dayText.setBackground(Theme.get().shapeStyle == 2
                ? new JaggedDrawable(0xF0060A12, Ui.dp(this, 3.5f))
                : new SkewDrawable(0xF0060A12, Ui.dp(this, 9)));
        dayText.setPadding(Ui.dpi(this, 16), Ui.dpi(this, 2),
                Ui.dpi(this, 22), Ui.dpi(this, 4));
        LinearLayout.LayoutParams dayLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dayLp.topMargin = Ui.dpi(this, 0);
        clockBox.addView(dayText, dayLp);

        // date on a royal-blue slash ribbon
        dateText = new TextView(this);
        dateText.setTypeface(Ui.tf(this));
        dateText.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.dp(this, 22));
        dateText.setTextColor(Theme.get().dateInk);
        dateText.setBackground(Theme.get().shapeStyle == 2
                ? new JaggedDrawable(0xFFFFFFFF, Ui.dp(this, 3f))
                : new SkewDrawable(0xFFFFFFFF, Ui.dp(this, 7)));
        dateText.setPadding(Ui.dpi(this, 14), Ui.dpi(this, 2),
                Ui.dpi(this, 18), Ui.dpi(this, 3));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = Ui.dpi(this, 4);
        dlp.leftMargin = Ui.dpi(this, 6);
        clockBox.addView(dateText, dlp);

        phaseText = new TextView(this);
        phaseText.setTypeface(Ui.tf(this));
        phaseText.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.dp(this, 24));
        phaseText.setTextColor(0xFF6FDBFF);
        phaseText.setLetterSpacing(0.10f);
        phaseText.setShadowLayer(Ui.dp(this, 5), 0, Ui.dp(this, 2), 0x99000000);
        if (Theme.get().shapeStyle == 2) {
            // P5: phase sits in a jagged red speech-tag with white text
            phaseText.setBackground(new JaggedDrawable(Theme.get().accent,
                    Ui.dp(this, 3f)));
            phaseText.setTextColor(0xFFFFFFFF);
            phaseText.setPadding(Ui.dpi(this, 12), Ui.dpi(this, 2),
                    Ui.dpi(this, 14), Ui.dpi(this, 3));
        }
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.topMargin = Ui.dpi(this, 5);
        plp.leftMargin = Ui.dpi(this, 6);
        clockBox.addView(phaseText, plp);

        // thin underline under the phase, like the mock
        ruleView = new View(this);
        ruleView.setBackgroundColor(0xB3FFFFFF);
        LinearLayout.LayoutParams ruleLp = new LinearLayout.LayoutParams(
                Ui.dpi(this, 130), Ui.dpi(this, 2));
        ruleLp.topMargin = Ui.dpi(this, 5);
        ruleLp.leftMargin = Ui.dpi(this, 6);
        clockBox.addView(ruleView, ruleLp);

        // weather line — "☾ CLEAR NIGHT · 74°" like the original mock
        weatherText = new TextView(this);
        weatherText.setTypeface(Ui.tfUpright(this));
        weatherText.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.dp(this, 15));
        weatherText.setTextColor(0xFFF2F5FA);
        weatherText.setLetterSpacing(0.06f);
        weatherText.setShadowLayer(Ui.dp(this, 4), 0, Ui.dp(this, 1), 0x99000000);
        weatherText.setVisibility(View.GONE);
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wlp.topMargin = Ui.dpi(this, 6);
        wlp.leftMargin = Ui.dpi(this, 6);
        clockBox.addView(weatherText, wlp);

        defaultHint = new TextView(this);
        defaultHint.setTypeface(Ui.tf(this));
        defaultHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        defaultHint.setTextColor(Ui.TEXT_DIM);
        defaultHint.setText("NOT DEFAULT LAUNCHER — TAP TO FIX");
        defaultHint.setVisibility(View.GONE);
        defaultHint.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { requestHomeRole(true); }
        });
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = Ui.dpi(this, 10);
        hlp.leftMargin = Ui.dpi(this, 8);
        clockBox.addView(defaultHint, hlp);

        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        clp.topMargin = Ui.dpi(this, -14); // offset ClockView's symmetric pad
        clp.leftMargin = Ui.dpi(this, -26);
        homeLayer.addView(clockBox, clp);

        clockBox.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                showSettingsDialog();
                return true;
            }
        });

        // ---- battery "wallet box" top-right ----
        battBox = new LinearLayout(this);
        battBox.setOrientation(LinearLayout.VERTICAL);
        battBox.setBackground(new SkewDrawable(0xFFFFFFFF, 0,
                0xFF06080C, Ui.dp(this, 1.5f)));
        battBox.setPadding(Ui.dpi(this, 18), Ui.dpi(this, 8),
                Ui.dpi(this, 18), Ui.dpi(this, 8));
        battText = new TextView(this);
        battText.setTypeface(Ui.tfUpright(this));
        battText.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.dp(this, 21));
        battText.setTextColor(0xFF06080C);
        battText.setIncludeFontPadding(false);
        battBox.addView(battText);
        TextView battCap = new TextView(this);
        battCap.setTypeface(Ui.tfUpright(this));
        battCap.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.dp(this, 11));
        battCap.setTextColor(0xB306080C);
        battCap.setText("next alarm");
        battBox.addView(battCap);
        FrameLayout.LayoutParams bbLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        bbLp.topMargin = Ui.dpi(this, 46);
        bbLp.rightMargin = Ui.dpi(this, 14);
        homeLayer.addView(battBox, bbLp);

        // ---- Makoto's player: dangles top-right while music plays ----
        playerView = new PlayerView(this);
        playerView.setVisibility(View.GONE);
        playerView.setListener(new PlayerView.Listener() {
            @Override public void onPlayPause() {
                if (mediaController == null) return;
                android.media.session.PlaybackState st =
                        mediaController.getPlaybackState();
                if (st != null && st.getState()
                        == android.media.session.PlaybackState.STATE_PLAYING) {
                    mediaController.getTransportControls().pause();
                } else {
                    mediaController.getTransportControls().play();
                }
            }
            @Override public void onPrev() {
                if (mediaController != null) {
                    mediaController.getTransportControls().skipToPrevious();
                }
            }
            @Override public void onNext() {
                if (mediaController != null) {
                    mediaController.getTransportControls().skipToNext();
                }
            }
            @Override public void onOpenApp() {
                if (mediaController == null) return;
                try {
                    Intent i = getPackageManager().getLaunchIntentForPackage(
                            mediaController.getPackageName());
                    if (i != null) {
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        flashAccent();
                    }
                } catch (Exception ignored) {}
            }
        });
        FrameLayout.LayoutParams plp2 = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        plp2.topMargin = Ui.dpi(this, 24);   // the alarm box's slot
        plp2.rightMargin = Ui.dpi(this, 4);
        homeLayer.addView(playerView, plp2);

        // ---- category column, right side like the mock ----
        // CategoryColumn: drag your finger along it and the highlight follows
        dockRow = new CategoryColumn(this);
        FrameLayout.LayoutParams menuLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        menuLp.topMargin = Ui.dpi(this, 148); // starts beside the SATURDAY ribbon
        menuLp.rightMargin = Ui.dpi(this, 4);
        homeLayer.addView(dockRow, menuLp);

        // ---- bottom quick row: phone / messages / drawer / camera / browser ----
        quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        quickRow.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams qLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        qLp.bottomMargin = Ui.dpi(this, 18);
        homeLayer.addView(quickRow, qLp);
        buildQuickRow();

        // ---- the card: multi-gesture trigger (double/hold/swipe) ----
        walletCard = new CardButton(this);
        final android.view.GestureDetector cardGd = new android.view.GestureDetector(
                this, new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                cardWiggle(); // taps are inert: thumb-zone safety
                return true;
            }
            @Override public boolean onDoubleTap(MotionEvent e) {
                runCardAction(cardAction("card_double", "flashlight"));
                return true;
            }
            @Override public void onLongPress(MotionEvent e) {
                walletCard.performHapticFeedback(
                        android.view.HapticFeedbackConstants.LONG_PRESS);
                runCardAction(cardAction("card_hold", "wallet"));
            }
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2,
                                             float vx, float vy) {
                if (vy < -800) {
                    runCardAction(cardAction("card_swipe", "lastapp"));
                    return true;
                }
                return false;
            }
        });
        walletCard.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        break;
                }
                cardGd.onTouchEvent(e);
                return true;
            }
        });
        FrameLayout.LayoutParams wcLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.START);
        wcLp.leftMargin = Ui.dpi(this, 4);
        wcLp.bottomMargin = Ui.dpi(this, 118);
        homeLayer.addView(walletCard, wcLp);
        walletCard.setVisibility(
                prefs.flag("wallet_hidden") ? View.GONE : View.VISIBLE);
        loadWalletIcon();

        // ---- swipe up anywhere -> drawer ----
        final android.view.GestureDetector gd = new android.view.GestureDetector(this,
                new android.view.GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float vx, float vy) {
                        if (vy < -1600) {
                            openDrawer();
                            return true;
                        }
                        if (vy > 1600) {
                            openRecents();
                            return true;
                        }
                        return false;
                    }
                });
        homeLayer.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                gd.onTouchEvent(e);
                return true;
            }
        });

        updateClock();
    }

    // ----------------------------------------------------------- drawer layer

    private void buildDrawerLayer() {
        drawerLayer = new LinearLayout(this);
        drawerLayer.setOrientation(LinearLayout.VERTICAL);
        // consume touches so nothing falls through to the invisible home layer
        drawerLayer.setClickable(true);
        root.addView(drawerLayer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // giant cropped black header on the white card
        drawerHeader = new TextView(this);
        drawerHeader.setTypeface(Ui.tf(this));
        drawerHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 52);
        drawerHeader.setTextColor(0xFF0A0C10);
        drawerHeader.setLetterSpacing(-0.03f);
        drawerHeader.setText("APPS");
        drawerHeader.setIncludeFontPadding(false);
        drawerHeader.setSingleLine(true);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hLp.leftMargin = Ui.dpi(this, 24);
        hLp.topMargin = Ui.dpi(this, 8);
        drawerLayer.addView(drawerHeader, hLp);

        // search box, straight and quiet
        FrameLayout searchRow = new FrameLayout(this);
        searchRow.setBackground(new SkewDrawable(0xFFFFFFFF, 0,
                0xFF0A0C10, Ui.dp(this, 1.5f)));
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dpi(this, 46));
        srLp.leftMargin = Ui.dpi(this, 26);
        srLp.rightMargin = Ui.dpi(this, 26);
        srLp.topMargin = Ui.dpi(this, 6);
        drawerLayer.addView(searchRow, srLp);
        searchRowV = searchRow;

        searchEdit = new EditText(this);
        searchEdit.setBackground(null);
        searchEdit.setTypeface(Ui.tf(this));
        searchEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        searchEdit.setTextColor(0xFF06080C);
        searchEdit.setHintTextColor(0x6606080C);
        searchEdit.setHint("SEARCH APPS");
        searchEdit.setSingleLine(true);
        searchEdit.setImeOptions(EditorInfo.IME_ACTION_GO
                | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        searchEdit.setPadding(Ui.dpi(this, 24), 0, Ui.dpi(this, 24), 0);
        searchRow.addView(searchEdit, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                adapter.setFilter(s.toString());
                listView.resetFocus();
                listView.applyFx();
            }
        });
        searchEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                AppEntry e = adapter.first();
                if (e == null) return true;
                if (addModeCat != null) {
                    prefs.toggleCatApp(addModeCat, e.pkg);
                    adapter.setAddMembers(prefs.catApps(addModeCat));
                    return true;
                }
                if (pickCardKey != null) {
                    prefs.setStrVal(pickCardKey, "app:" + e.pkg);
                    pickCardKey = null;
                    closeDrawer();
                    toast("GESTURE → " + e.labelUp);
                    return true;
                }
                if (pickWallet) {
                    pickWallet = false;
                    prefs.setStrVal("wallet_pkg", e.pkg);
                    closeDrawer();
                    toast("WALLET → " + e.labelUp);
                    return true;
                }
                if (pickSlot >= 0) {
                    int slot = pickSlot;
                    pickSlot = -1;
                    prefs.setQuickSlot(slot, e.pkg);
                    buildQuickRow();
                    closeDrawer();
                    toast("DOCK SLOT " + (slot + 1) + " → " + e.labelUp);
                    return true;
                }
                launch(e, v);
                return true;
            }
        });

        // list + A-Z rail
        adapter = new AppsAdapter(this);
        adapter.setHidden(prefs.hidden());
        listView = new AppsListView(this);
        listView.setAdapter(adapter);
        FrameLayout listWrap = new FrameLayout(this);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        llp.topMargin = Ui.dpi(this, 8);
        drawerLayer.addView(listWrap, llp);
        listWrap.addView(listView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        AlphaRail rail = new AlphaRail(this);
        rail.setListener(new AlphaRail.Listener() {
            @Override public void onLetter(char letter) {
                int idx = adapter.firstIndexForLetter(letter);
                if (idx >= 0) listView.setSelection(idx);
            }
        });
        FrameLayout.LayoutParams railLp = new FrameLayout.LayoutParams(
                Ui.dpi(this, 26), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END);
        railLp.topMargin = Ui.dpi(this, 6);
        railLp.bottomMargin = Ui.dpi(this, 60);
        listWrap.addView(rail, railLp);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                AppEntry e = (AppEntry) adapter.getItem(pos);
                if (addModeCat != null) {
                    prefs.toggleCatApp(addModeCat, e.pkg);
                    adapter.setAddMembers(prefs.catApps(addModeCat));
                    view.performHapticFeedback(
                            android.view.HapticFeedbackConstants.CLOCK_TICK);
                    return;
                }
                if (pickCardKey != null) {
                    prefs.setStrVal(pickCardKey, "app:" + e.pkg);
                    pickCardKey = null;
                    closeDrawer();
                    toast("GESTURE → " + e.labelUp);
                    return;
                }
                if (pickWallet) {
                    pickWallet = false;
                    prefs.setStrVal("wallet_pkg", e.pkg);
                    closeDrawer();
                    toast("WALLET → " + e.labelUp);
                    return;
                }
                if (pickSlot >= 0) {
                    int slot = pickSlot;
                    pickSlot = -1;
                    prefs.setQuickSlot(slot, e.pkg);
                    buildQuickRow();
                    closeDrawer();
                    toast("DOCK SLOT " + (slot + 1) + " → " + e.labelUp);
                    return;
                }
                launch(e, view);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int pos, long id) {
                if (addModeCat != null || pickSlot >= 0 || pickWallet
                        || pickCardKey != null) return true;
                showAppDialog((AppEntry) adapter.getItem(pos));
                return true;
            }
        });
    }

    // ------------------------------------------------------------ drawer anim

    private void setDrawerPos(float v) {
        drawerPos = v;
        int h = root.getHeight();
        if (h == 0) return;
        drawerLayer.setTranslationY((1f - v) * h);
        homeLayer.setTranslationY(-v * Ui.dp(this, 56));
        homeLayer.setAlpha(1f - v);
        // header + search slide in from the right, slightly behind the card
        if (drawerHeader != null) {
            drawerHeader.setTranslationX((1f - v) * Ui.dp(this, 90));
            drawerHeader.setAlpha(v);
        }
        if (searchRowV != null) {
            searchRowV.setTranslationX((1f - v) * Ui.dp(this, 55));
            searchRowV.setAlpha(v);
        }
        // list lags slightly behind the card — parallax depth
        if (listView != null) {
            listView.setTranslationX((1f - v) * Ui.dp(this, 52));
        }
        bg.setDrawerProgress(v);
    }

    private void animateDrawer(final float target) {
        if (drawerAnim != null) drawerAnim.cancel();
        drawerAnim = ValueAnimator.ofFloat(drawerPos, target);
        drawerAnim.setDuration(380);
        drawerAnim.setInterpolator(new DecelerateInterpolator(1.9f));
        drawerAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                setDrawerPos(((Float) a.getAnimatedValue()).floatValue());
            }
        });
        drawerAnim.start();
    }

    private void openDrawer() {
        openDrawerFor(null);
    }

    private void openDrawerFor(String name) {
        if (drawerOpen) return;
        drawerOpen = true;
        currentCat = name;
        addModeCat = null;
        pickSlot = -1;
        pickWallet = false;
        pickCardKey = null;
        adapter.setAddMembers(null);
        adapter.setOrderedPkgs(null);
        adapter.setCatPkgs(name == null ? null : prefs.catApps(name));
        drawerHeader.setText(name == null ? "APPS" : name);
        listView.resetFocus();
        listView.setSelection(0);
        root.performHapticFeedback(
                android.view.HapticFeedbackConstants.CLOCK_TICK);
        animateDrawer(1f);
    }

    /** Swipe down: last-launched apps, most recent first. */
    private void openRecents() {
        if (drawerOpen) return;
        List<String> r = prefs.recents();
        if (r.isEmpty()) {
            toast("NOTHING RECENT YET");
            return;
        }
        drawerOpen = true;
        currentCat = null;
        addModeCat = null;
        pickSlot = -1;
        pickWallet = false;
        pickCardKey = null;
        adapter.setAddMembers(null);
        adapter.setCatPkgs(null);
        adapter.setOrderedPkgs(r);
        drawerHeader.setText("RECENT");
        listView.resetFocus();
        listView.setSelection(0);
        animateDrawer(1f);
    }

    private void enterAddMode(String cat) {
        addModeCat = cat;
        pickSlot = -1;
        pickWallet = false;
        pickCardKey = null;
        currentCat = cat;
        adapter.setOrderedPkgs(null);
        adapter.setCatPkgs(null);
        adapter.setAddMembers(prefs.catApps(cat));
        drawerHeader.setText("+ " + cat);
        listView.resetFocus();
        if (!drawerOpen) {
            drawerOpen = true;
            listView.setSelection(0);
            animateDrawer(1f);
        }
        toast("TAP APPS TO ADD OR REMOVE — BACK WHEN DONE");
    }

    private void exitAddMode() {
        String cat = addModeCat;
        addModeCat = null;
        adapter.setAddMembers(null);
        adapter.setCatPkgs(prefs.catApps(cat));
        drawerHeader.setText(cat);
        listView.resetFocus();
        listView.setSelection(0);
        rebuildDock();
    }

    private void enterPickMode(int slot) {
        pickSlot = slot;
        addModeCat = null;
        currentCat = null;
        pickWallet = false;
        pickCardKey = null;
        adapter.setAddMembers(null);
        adapter.setOrderedPkgs(null);
        adapter.setCatPkgs(null);
        drawerHeader.setText("PICK APP");
        listView.resetFocus();
        if (!drawerOpen) {
            drawerOpen = true;
            listView.setSelection(0);
            animateDrawer(1f);
        }
        toast("TAP AN APP FOR SLOT " + (slot + 1));
    }

    private void closeDrawer() {
        if (!drawerOpen) return;
        drawerOpen = false;
        addModeCat = null;
        pickSlot = -1;
        pickWallet = false;
        pickCardKey = null;
        adapter.setAddMembers(null);
        adapter.setOrderedPkgs(null);
        hideIme();
        searchEdit.setText("");
        searchEdit.clearFocus();
        root.performHapticFeedback(
                android.view.HapticFeedbackConstants.CLOCK_TICK);
        animateDrawer(0f);
    }

    private void hideIme() {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchEdit.getWindowToken(), 0);
    }

    @Override
    public void onBackPressed() {
        if (addModeCat != null) {
            exitAddMode();
            return;
        }
        if (drawerOpen) closeDrawer();
        // a launcher never finishes on back
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // home button pressed while we're already the home app
        closeDrawer();
    }

    // ------------------------------------------------------------------- data

    private void loadApps() {
        AppRepository.load(this, new AppRepository.Callback() {
            @Override
            public void onLoaded(List<AppEntry> loaded) {
                apps = loaded;
                // one-time: shorten the seeded 13-char COMMUNICATION
                if (!prefs.flag("comm_renamed")) {
                    prefs.setFlag("comm_renamed", true);
                    if (prefs.catNames().contains("COMMUNICATION")
                            && !prefs.catNames().contains("SOCIAL")) {
                        prefs.renameCat("COMMUNICATION", "SOCIAL");
                        prefs.setCatJp("SOCIAL", "ソーシャル");
                    }
                }
                // one-time: SYSTEM leaves the editable store, becomes pinned
                if (!prefs.flag("system_pinned")) {
                    prefs.setFlag("system_pinned", true);
                    if (prefs.catNames().contains("SYSTEM")) {
                        prefs.deleteCat("SYSTEM");
                    }
                }
                usesMap.clear();
                for (AppEntry e : loaded) {
                    int u = prefs.uses(e.pkg);
                    if (u > 0) usesMap.put(e.pkg, Integer.valueOf(u));
                }
                adapter.setUses(usesMap);
                adapter.setHidden(prefs.hidden());
                adapter.setData(loaded);
                rebuildDock();
                buildQuickRow();
            }

            @Override
            public void onIconsUpdated() {
                adapter.notifyDataSetChanged();
                // rebuild dock only while a custom slot still lacks its icon
                if (quickAwaitingIcons) buildQuickRow();
            }
        });
    }

    private AppEntry findApp(String id) {
        for (AppEntry e : apps) {
            if (e.id().equals(id)) return e;
        }
        return null;
    }

    // 8 user categories + the pinned SYSTEM row = 9 on screen
    private static final int HOME_CAT_MAX = 8;

    /** The home word stack: up to 6 categories, right-aligned. Swipe up = all apps. */
    private void rebuildDock() {
        dockRow.removeAllViews();
        dockRow.setGravity(Gravity.START); // shared left edge...
        dockRow.setClipChildren(false);    // ...with the column pinned right
        List<String> names = prefs.catNames();

        int wordColor = lastDh ? Theme.get().darkHourText : Theme.get().catWord;
        int shown = Math.min(names.size(), HOME_CAT_MAX);
        for (int i = 0; i < shown; i++) {
            String name = names.get(i);
            addCatWord(name, prefs.catJp(name), wordColor, name);
        }

        if (names.size() > HOME_CAT_MAX) {
            // overflow entry so extra categories stay reachable
            final List<String> extra = names.subList(HOME_CAT_MAX, names.size());
            final List<String> extraCopy = new ArrayList<String>(extra);
            MenuWordView more = new MenuWordView(this,
                    "MORE (" + extraCopy.size() + ")", "", 0xB3F2F5FA);
            more.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { showMoreDialog(extraCopy); }
            });
            dockRow.addView(more, wordLp());
        } else if (names.size() < HOME_CAT_MAX) {
            MenuWordView plus = new MenuWordView(this, "+ NEW", "", 0x80F2F5FA);
            plus.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { newCategory(); }
            });
            dockRow.addView(plus, wordLp());
        }
        // at 8 user categories "+ NEW" disappears; deleting one brings it back

        // ---- SYSTEM: permanent, uneditable, always last → phone Settings ----
        MenuWordView sys = new MenuWordView(this, "SYSTEM", "システム", wordColor);
        sys.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                v.postDelayed(new Runnable() {
                    @Override public void run() {
                        try {
                            Intent i = new Intent(Settings.ACTION_SETTINGS);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(i);
                            flashAccent();
                        } catch (Exception ignored) {}
                    }
                }, 90);
            }
        });
        dockRow.addView(sys, wordLp());
    }

    private void newCategory() {
        if (prefs.catNames().size() >= HOME_CAT_MAX) {
            toast("MAX " + HOME_CAT_MAX + " CATEGORIES — DELETE ONE FIRST");
            return;
        }
        promptDialog("NEW CATEGORY", "", "", new NameCallback() {
            @Override public void onName(String name, String kana) {
                prefs.addCat(name, kana);
                rebuildDock();
            }
        });
    }

    private void showMoreDialog(List<String> extra) {
        Dialog d = makeDialog("MORE CATEGORIES");
        for (final String name : extra) {
            addRow(d, name, 0xFF06080C, new Runnable() {
                @Override public void run() { openDrawerFor(name); }
            });
        }
        addRow(d, "+ NEW CATEGORY", 0xFF06080C, new Runnable() {
            @Override public void run() { newCategory(); }
        });
        d.show();
    }

    private void addCatWord(final String label, String jp, int color,
                            final String tag) {
        MenuWordView word = new MenuWordView(this, label, jp, color);
        word.setTag(tag);
        word.setDot(catHasDot(tag));
        word.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // brief beat so the slash press state reads
                v.postDelayed(new Runnable() {
                    @Override public void run() {
                        openDrawerFor(tag.length() == 0 ? null : tag);
                    }
                }, 90);
            }
        });
        if (tag.length() > 0) {
            word.setLongClickable(true);
            word.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    showCatManageDialog(tag);
                    return true;
                }
            });
        }
        dockRow.addView(word, wordLp());
    }

    private LinearLayout.LayoutParams wordLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dpi(this, -2);
        lp.gravity = Gravity.START; // all words share one left edge
        return lp;
    }

    private boolean catHasDot(String tag) {
        Set<String> hiddenPkgs = prefs.hidden();
        Set<String> members = tag.length() == 0 ? null : prefs.catApps(tag);
        for (AppEntry e : apps) {
            if (hiddenPkgs.contains(e.pkg)) continue;
            if ((members == null || members.contains(e.pkg))
                    && NotifService.has(e.pkg)) {
                return true;
            }
        }
        return false;
    }

    // ----- category management -----

    private interface NameCallback {
        void onName(String name, String kana);
    }

    private EditText promptField(String hint, String initial, int maxLen) {
        EditText input = new EditText(this);
        input.setTypeface(Ui.tf(this));
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        input.setTextColor(0xFF06080C);
        input.setHintTextColor(0x5906080C);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setFilters(new android.text.InputFilter[]{
                new android.text.InputFilter.LengthFilter(maxLen)});
        input.setText(initial);
        input.setBackground(new SkewDrawable(0xFFFFFFFF, 0,
                0xFF06080C, Ui.dp(this, 1.5f)));
        input.setPadding(Ui.dpi(this, 16), Ui.dpi(this, 10),
                Ui.dpi(this, 16), Ui.dpi(this, 10));
        return input;
    }

    private void promptDialog(String title, String initial, String initialKana,
                              final NameCallback cb) {
        final Dialog d = makeDialog(title);
        // 12-char cap keeps every name inside the column's width
        final EditText input = promptField("NAME", initial, 12);
        final EditText kanaInput = promptField(
                "カタカナ — AUTO IF EMPTY", initialKana, 16);
        LinearLayout box = (LinearLayout) d.getWindow().getDecorView()
                .findViewById(android.R.id.list);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dpi(this, 7);
        box.addView(input, lp);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp2.topMargin = Ui.dpi(this, 7);
        box.addView(kanaInput, lp2);
        // OK row added directly (not via addRow) so an invalid name keeps
        // the dialog open instead of eating the user's input
        TextView ok = dialogRow("OK", 0xFF06080C);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String name = input.getText().toString().trim()
                        .toUpperCase(Locale.getDefault()).replace("|", "");
                if (name.length() == 0 || name.equals("ALL APPS")
                        || name.equals("SYSTEM")
                        || prefs.catNames().contains(name)) {
                    toast("PICK A DIFFERENT NAME");
                    return;
                }
                String kana = kanaInput.getText().toString().trim();
                if (kana.length() == 0) kana = Kana.toKatakana(name);
                d.dismiss();
                cb.onName(name, kana);
            }
        });
        LinearLayout.LayoutParams okLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        okLp.topMargin = Ui.dpi(this, 7);
        box.addView(ok, okLp);
        d.show();
    }

    /** Notification peek: what's waiting inside these packages. */
    private void showPeekDialog(String header, List<String> pkgs) {
        Dialog d = makeDialog(header);
        boolean any = false;
        for (final String pkg : pkgs) {
            int n = NotifService.count(pkg);
            if (n == 0) continue;
            any = true;
            String label = pkg;
            for (AppEntry e : apps) {
                if (e.pkg.equals(pkg)) { label = e.labelUp; break; }
            }
            StringBuilder sb = new StringBuilder(label);
            if (n > 1) sb.append("  (").append(n).append(")");
            List<String> ts = NotifService.titles(pkg);
            if (!ts.isEmpty() && ts.get(0).length() > 0) {
                sb.append("\n").append(android.text.TextUtils.join(" · ", ts));
            }
            final String fp = pkg;
            addRow(d, sb.toString(), 0xFF06080C, new Runnable() {
                @Override public void run() {
                    AppEntry target = null;
                    for (AppEntry e : apps) {
                        if (e.pkg.equals(fp)) { target = e; break; }
                    }
                    if (target != null) launch(target, null);
                    else startQuick(null, fp);
                }
            });
        }
        if (!any) {
            addRow(d, "NOTHING WAITING", 0x8806080C, new Runnable() {
                @Override public void run() {}
            });
        }
        d.show();
    }

    private List<String> dottedIn(Set<String> members) {
        List<String> out = new ArrayList<String>();
        Set<String> hiddenPkgs = prefs.hidden();
        for (AppEntry e : apps) {
            if (hiddenPkgs.contains(e.pkg)) continue;
            if ((members == null || members.contains(e.pkg))
                    && NotifService.has(e.pkg)) {
                out.add(e.pkg);
            }
        }
        return out;
    }

    private void showCatManageDialog(final String name) {
        Dialog d = makeDialog(name);
        final List<String> dotted = dottedIn(prefs.catApps(name));
        if (!dotted.isEmpty()) {
            addRow(d, "◆ NOTIFICATIONS (" + dotted.size() + ")", 0xFFE60012,
                    new Runnable() {
                @Override public void run() { showPeekDialog(name, dotted); }
            });
        }
        addRow(d, "ADD / REMOVE APPS", 0xFF06080C, new Runnable() {
            @Override public void run() { enterAddMode(name); }
        });
        addRow(d, "RENAME", 0xFF06080C, new Runnable() {
            @Override public void run() {
                promptDialog("RENAME " + name, name, prefs.catJp(name),
                        new NameCallback() {
                    @Override public void onName(String newName, String kana) {
                        prefs.renameCat(name, newName);
                        prefs.setCatJp(newName, kana);
                        rebuildDock();
                    }
                });
            }
        });
        addRow(d, "MOVE UP", 0xFF06080C, new Runnable() {
            @Override public void run() {
                prefs.moveCat(name, -1);
                rebuildDock();
            }
        });
        addRow(d, "MOVE DOWN", 0xFF06080C, new Runnable() {
            @Override public void run() {
                prefs.moveCat(name, 1);
                rebuildDock();
            }
        });
        addRow(d, "DELETE CATEGORY", 0xFFE60012, new Runnable() {
            @Override public void run() {
                prefs.deleteCat(name);
                rebuildDock();
                toast("DELETED " + name + " (APPS ARE NOT UNINSTALLED)");
            }
        });
        d.show();
    }

    private boolean quickAwaitingIcons = false;

    private void buildQuickRow() {
        quickRow.removeAllViews();
        quickAwaitingIcons = false;
        int[] kinds = {GlyphView.PHONE, GlyphView.MSG, GlyphView.PLAY,
                GlyphView.WEB, GlyphView.CAM};
        String[] labels = {"PHONE", "MESSAGES", "YOUTUBE", "CHROME", "CAMERA"};
        String[] jps = {"電話", "メッセージ", "ユーチューブ", "クローム", "カメラ"};
        for (int i = 0; i < 5; i++) {
            final int slot = i;
            String custom = prefs.quickSlot(i);
            GlyphView g;
            final Runnable action;
            String dotPkg;
            if (custom.length() > 0) {
                dotPkg = custom;
            } else {
                dotPkg = defaultQuickPkg(i);
            }
            if (custom.length() > 0) {
                AppEntry e = null;
                for (AppEntry a : apps) {
                    if (a.pkg.equals(custom)) { e = a; break; }
                }
                final String pkg = custom;
                if (e != null && e.icon == null) quickAwaitingIcons = true;
                g = new GlyphView(this, e == null ? null : e.icon,
                        e == null ? "?" : e.labelUp);
                action = new Runnable() {
                    @Override public void run() { startQuick(null, pkg); }
                };
            } else {
                g = new GlyphView(this, kinds[i], labels[i], jps[i]);
                action = defaultQuickAction(i);
            }
            g.setTag(dotPkg);
            g.setDot(dotPkg != null && NotifService.has(dotPkg));
            g.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { action.run(); }
            });
            g.setLongClickable(true);
            g.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    showSlotDialog(slot);
                    return true;
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    Ui.dpi(this, 66), Ui.dpi(this, 94));
            lp.leftMargin = Ui.dpi(this, 3);
            lp.rightMargin = Ui.dpi(this, 3);
            quickRow.addView(g, lp);
        }
    }

    private String resolvePkg(Intent i) {
        try {
            android.content.pm.ResolveInfo ri = getPackageManager()
                    .resolveActivity(i,
                            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
            return ri == null || ri.activityInfo == null
                    ? null : ri.activityInfo.packageName;
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean installed(String pkg) {
        try {
            return getPackageManager().getLaunchIntentForPackage(pkg) != null;
        } catch (Exception ex) {
            return false;
        }
    }

    /** Which package a default dock slot represents (for notification dots). */
    private String defaultQuickPkg(int slot) {
        switch (slot) {
            case 0: return resolvePkg(new Intent(Intent.ACTION_DIAL));
            case 1: return installed("com.google.android.apps.messaging")
                    ? "com.google.android.apps.messaging"
                    : resolvePkg(Intent.makeMainSelectorActivity(
                            Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING));
            case 2: return installed("com.google.android.youtube")
                    ? "com.google.android.youtube" : null;
            case 3: return installed("com.android.chrome")
                    ? "com.android.chrome"
                    : resolvePkg(Intent.makeMainSelectorActivity(
                            Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER));
            default: return resolvePkg(new Intent(android.provider.MediaStore
                    .INTENT_ACTION_STILL_IMAGE_CAMERA));
        }
    }

    private Runnable defaultQuickAction(int slot) {
        switch (slot) {
            case 0: return new Runnable() {
                @Override public void run() {
                    startQuick(new Intent(Intent.ACTION_DIAL), null);
                }
            };
            case 1: return new Runnable() {
                @Override public void run() {
                    startQuick(Intent.makeMainSelectorActivity(
                            Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
                            "com.google.android.apps.messaging");
                }
            };
            case 2: return new Runnable() {
                @Override public void run() {
                    startQuick(null, "com.google.android.youtube");
                }
            };
            case 3: return new Runnable() {
                @Override public void run() {
                    startQuick(Intent.makeMainSelectorActivity(
                            Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER),
                            "com.android.chrome");
                }
            };
            default: return new Runnable() {
                @Override public void run() {
                    startQuick(new Intent(android.provider.MediaStore
                            .INTENT_ACTION_STILL_IMAGE_CAMERA), null);
                }
            };
        }
    }

    private void showSlotDialog(final int slot) {
        Dialog d = makeDialog("DOCK SLOT " + (slot + 1));
        String custom = prefs.quickSlot(slot);
        final String pkg = custom.length() > 0 ? custom : defaultQuickPkg(slot);
        if (pkg != null && NotifService.has(pkg)) {
            final List<String> one = new ArrayList<String>();
            one.add(pkg);
            addRow(d, "◆ NOTIFICATIONS (" + NotifService.count(pkg) + ")",
                    0xFFE60012, new Runnable() {
                @Override public void run() {
                    showPeekDialog("WAITING", one);
                }
            });
        }
        addRow(d, "CHOOSE APP", 0xFF06080C, new Runnable() {
            @Override public void run() { enterPickMode(slot); }
        });
        addRow(d, "RESET TO DEFAULT", 0xFF06080C, new Runnable() {
            @Override public void run() {
                prefs.setQuickSlot(slot, "");
                buildQuickRow();
            }
        });
        d.show();
    }

    /** Launch by package if given (with intent fallback), else by intent. */
    private void startQuick(Intent fallback, String pkg) {
        try {
            Intent i = null;
            if (pkg != null) {
                i = getPackageManager().getLaunchIntentForPackage(pkg);
            }
            if (i == null) i = fallback;
            if (i == null) {
                toast("APP NOT FOUND");
                return;
            }
            i = new Intent(i);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            flashAccent();
            if (pkg != null) prefs.pushRecent(pkg);
        } catch (Exception ex) {
            toast("CAN'T LAUNCH");
        }
    }

    private void updateDots() {
        adapter.notifyDataSetChanged();
        for (int i = 0; i < dockRow.getChildCount(); i++) {
            View v = dockRow.getChildAt(i);
            if (v instanceof MenuWordView && v.getTag() instanceof String) {
                ((MenuWordView) v).setDot(catHasDot((String) v.getTag()));
            }
        }
        for (int i = 0; i < quickRow.getChildCount(); i++) {
            View v = quickRow.getChildAt(i);
            if (v instanceof GlyphView) {
                String pkg = v.getTag() instanceof String ? (String) v.getTag() : null;
                ((GlyphView) v).setDot(pkg != null && NotifService.has(pkg));
            }
        }
    }

    // ------------------------------------------------------------------ clock

    private void updateClock() {
        Calendar cal = Calendar.getInstance();
        boolean h24 = android.text.format.DateFormat.is24HourFormat(this);
        int hour24 = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int shown = h24 ? hour24 : cal.get(Calendar.HOUR);
        if (!h24 && shown == 0) shown = 12;
        // no leading zero on the hour — cleaner, and thinner at 10/11/12
        timeText.setTime(String.format(Locale.US, "%d:%02d", shown, minute));

        SimpleDateFormat dayF = new SimpleDateFormat("EEEE", Locale.getDefault());
        dayText.setText(dayF.format(cal.getTime()).toUpperCase(Locale.getDefault()));
        SimpleDateFormat dateF = new SimpleDateFormat("M/d", Locale.getDefault());
        dateText.setText(dateF.format(cal.getTime()));

        // "wallet box" = next alarm (status bar already shows battery %)
        try {
            if (battText != null && battBox != null) {
                android.app.AlarmManager am = (android.app.AlarmManager)
                        getSystemService(Context.ALARM_SERVICE);
                android.app.AlarmManager.AlarmClockInfo info =
                        am == null ? null : am.getNextAlarmClock();
                boolean musicShowing = playerView != null
                        && playerView.getVisibility() == View.VISIBLE;
                if (info == null) {
                    battBox.setVisibility(View.GONE);
                } else {
                    Calendar al = Calendar.getInstance();
                    al.setTimeInMillis(info.getTriggerTime());
                    boolean today = al.get(Calendar.DAY_OF_YEAR)
                            == cal.get(Calendar.DAY_OF_YEAR);
                    SimpleDateFormat af = new SimpleDateFormat(
                            today ? (h24 ? "H:mm" : "h:mm")
                                  : (h24 ? "EEE H:mm" : "EEE h:mm"),
                            Locale.getDefault());
                    battText.setText(af.format(al.getTime())
                            .toUpperCase(Locale.getDefault()));
                    if (!musicShowing) battBox.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception ignored) {}

        boolean dh = isDarkHour(hour24);
        // Red Hour: white phase text on the red jagged tag; else themed subtitle
        int ph = Theme.get().shapeStyle == 2 ? 0xFFFFFFFF : Theme.get().subtitle;
        String phase;
        int color;
        if (dh) { phase = "DARK HOUR"; color = Theme.get().darkHourText; }
        else if (hour24 < 5) { phase = "LATE NIGHT"; color = ph; }
        else if (hour24 < 11) { phase = "MORNING"; color = ph; }
        else if (hour24 < 17) { phase = "DAYTIME"; color = ph; }
        else if (hour24 < 20) { phase = "EVENING"; color = ph; }
        else { phase = "LATE NIGHT"; color = ph; }
        phaseText.setText(phase);
        phaseText.setTextColor(color);

        // dark hour mode: the world tints sickly green
        if (dh != lastDh) {
            lastDh = dh;
            bg.setDarkHour(dh);
            ruleView.setBackgroundColor(dh ? 0xB39BFFC8 : 0xB3FFFFFF);
            rebuildDock();
        }
    }

    // ----------------------------------------------------------------- launch

    private void launch(AppEntry e, View from) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setClassName(e.pkg, e.cls);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            Bundle opts = null;
            if (from != null) {
                opts = ActivityOptions.makeScaleUpAnimation(
                        from, 0, 0, from.getWidth(), from.getHeight()).toBundle();
            }
            startActivity(i, opts);
            flashAccent();
            prefs.bumpUses(e.pkg);
            prefs.pushRecent(e.pkg);
            usesMap.put(e.pkg, Integer.valueOf(prefs.uses(e.pkg)));
        } catch (Exception ex) {
            toast("CAN'T LAUNCH " + e.labelUp);
        }
    }

    private void flashAccent() {
        flash.animate().cancel();
        flash.setAlpha(0.30f);
        flash.animate().alpha(0f).setDuration(330).start();
    }

    // ---------------------------------------------------------------- dialogs

    private TextView dialogRow(String text, int textColor) {
        TextView tv = new TextView(this);
        tv.setTypeface(Ui.tf(this));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setTextColor(new android.content.res.ColorStateList(
                new int[][]{{android.R.attr.state_pressed}, {}},
                new int[]{0xFFFFFFFF, textColor}));
        tv.setText(text);
        tv.setClickable(true);
        tv.setBackground(Ui.skewSelector(this, 0xF5FFFFFF, 0xFFE60012, Ui.dp(this, 10)));
        tv.setPadding(Ui.dpi(this, 20), Ui.dpi(this, 13),
                Ui.dpi(this, 20), Ui.dpi(this, 13));
        return tv;
    }

    private Dialog makeDialog(String title) {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = new LinearLayout(this);
        box.setId(android.R.id.list);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dpi(this, 10), Ui.dpi(this, 10),
                Ui.dpi(this, 10), Ui.dpi(this, 10));

        TextView head = new TextView(this);
        head.setTypeface(Ui.tf(this));
        head.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        head.setTextColor(Theme.get().accentText());
        head.setText(title);
        head.setBackground(new SkewDrawable(Theme.get().accent, Ui.dp(this, 10)));
        head.setPadding(Ui.dpi(this, 20), Ui.dpi(this, 10),
                Ui.dpi(this, 20), Ui.dpi(this, 10));
        box.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(box, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        d.setContentView(scroll);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0));
            w.setLayout(Ui.dpi(this, 310), ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setDimAmount(0.75f);
        }
        return d;
    }

    private void addRow(Dialog d, String text, int color, final Runnable action) {
        LinearLayout box = (LinearLayout) d.getWindow().getDecorView()
                .findViewById(android.R.id.list);
        TextView row = dialogRow(text, color);
        final Dialog fd = d;
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                fd.dismiss();
                action.run();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dpi(this, 7);
        box.addView(row, lp);
    }

    private void showAppDialog(final AppEntry e) {
        Dialog d = makeDialog(e.labelUp);
        if (currentCat != null && addModeCat == null) {
            final String cat = currentCat;
            addRow(d, "REMOVE FROM " + cat, 0xFF06080C, new Runnable() {
                @Override public void run() {
                    Set<String> s = prefs.catApps(cat);
                    s.remove(e.pkg);
                    prefs.setCatApps(cat, s);
                    adapter.setCatPkgs(s);
                    rebuildDock();
                }
            });
        }
        addRow(d, "ADD TO CATEGORY…", 0xFF06080C, new Runnable() {
            @Override public void run() { showAppCatsDialog(e); }
        });

        // ---- the app's own deep shortcuts (default-launcher privilege) ----
        try {
            final android.content.pm.LauncherApps la =
                    (android.content.pm.LauncherApps)
                            getSystemService(Context.LAUNCHER_APPS_SERVICE);
            if (la != null && la.hasShortcutHostPermission()) {
                android.content.pm.LauncherApps.ShortcutQuery q =
                        new android.content.pm.LauncherApps.ShortcutQuery();
                q.setPackage(e.pkg);
                q.setQueryFlags(
                        android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                        | android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST);
                List<android.content.pm.ShortcutInfo> shortcuts =
                        la.getShortcuts(q, android.os.Process.myUserHandle());
                if (shortcuts != null) {
                    int count = 0;
                    for (final android.content.pm.ShortcutInfo si : shortcuts) {
                        if (count >= 4) break;
                        CharSequence lbl = si.getShortLabel();
                        if (lbl == null || !si.isEnabled()) continue;
                        count++;
                        addRow(d, "▸ " + lbl.toString().toUpperCase(
                                Locale.getDefault()), 0xFF1E4FD8, new Runnable() {
                            @Override public void run() {
                                try {
                                    la.startShortcut(si.getPackage(), si.getId(),
                                            null, null,
                                            android.os.Process.myUserHandle());
                                    flashAccent();
                                    prefs.pushRecent(e.pkg);
                                } catch (Exception ex) {
                                    toast("SHORTCUT FAILED");
                                }
                            }
                        });
                    }
                }
            }
        } catch (Exception ignored) {
        }

        addRow(d, "HIDE APP", 0xFF06080C, new Runnable() {
            @Override public void run() {
                prefs.hide(e.pkg);
                adapter.setHidden(prefs.hidden());
                rebuildDock();
                toast("HIDDEN — LONG-PRESS THE CLOCK TO UNHIDE");
            }
        });
        addRow(d, "APP INFO", 0xFF06080C, new Runnable() {
            @Override public void run() {
                try {
                    startActivity(new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + e.pkg)));
                } catch (Exception ignored) {}
            }
        });
        addRow(d, "UNINSTALL", 0xFFE60012, new Runnable() {
            @Override public void run() {
                try {
                    startActivity(new Intent(Intent.ACTION_DELETE,
                            Uri.parse("package:" + e.pkg)));
                } catch (Exception ignored) {}
            }
        });
        d.show();
    }

    /** Toggle this app's membership per category (✓ = currently in it). */
    private void showAppCatsDialog(final AppEntry e) {
        Dialog d = makeDialog(e.labelUp);
        for (final String name : prefs.catNames()) {
            boolean in = prefs.catApps(name).contains(e.pkg);
            addRow(d, (in ? "✓  " : "    ") + name, 0xFF06080C, new Runnable() {
                @Override public void run() {
                    prefs.toggleCatApp(name, e.pkg);
                    if (currentCat != null) {
                        adapter.setCatPkgs(prefs.catApps(currentCat));
                    }
                    rebuildDock();
                    toast(prefs.catApps(name).contains(e.pkg)
                            ? "ADDED TO " + name : "REMOVED FROM " + name);
                }
            });
        }
        d.show();
    }

    private void showSettingsDialog() {
        String ver = "";
        try {
            ver = " v" + getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {}
        Dialog d = makeDialog("ARCANA LAUNCHER" + ver);
        addRow(d, "▸  THEME & DISPLAY", 0xFF06080C, new Runnable() {
            @Override public void run() { showDisplayMenu(); }
        });
        addRow(d, "▸  THE CARD", 0xFF06080C, new Runnable() {
            @Override public void run() { showCardMenu(); }
        });
        addRow(d, "▸  APPS", 0xFF06080C, new Runnable() {
            @Override public void run() { showAppsMenu(); }
        });
        addRow(d, "▸  BACKUP", 0xFF06080C, new Runnable() {
            @Override public void run() { showBackupMenu(); }
        });
        addRow(d, "▸  SETUP", 0xFF06080C, new Runnable() {
            @Override public void run() { showSetupMenu(); }
        });
        d.show();
    }

    private void showDisplayMenu() {
        Dialog d = makeDialog("THEME & DISPLAY");
        addRow(d, "THEME:  " + Theme.get().name, 0xFF06080C, new Runnable() {
            @Override public void run() { showThemeDialog(); }
        });
        addRow(d, prefs.flag("crt") ? "CRT SHADER: ON" : "CRT SHADER: OFF",
                0xFF06080C, new Runnable() {
            @Override public void run() {
                prefs.setFlag("crt", !prefs.flag("crt"));
                applyCrt();
                toast(prefs.flag("crt") ? "CRT ON" : "CRT OFF");
            }
        });
        addRow(d, prefs.flag("dh_force")
                        ? "DARK HOUR PREVIEW: ON" : "DARK HOUR PREVIEW: OFF",
                0xFF06080C, new Runnable() {
            @Override public void run() {
                prefs.setFlag("dh_force", !prefs.flag("dh_force"));
                updateClock();
                toast(prefs.flag("dh_force")
                        ? "THE DARK HOUR IS UPON US" : "BACK TO NORMAL TIME");
            }
        });
        d.show();
    }

    private void showCardMenu() {
        Dialog d = makeDialog("THE CARD");
        addRow(d, "GESTURE ACTIONS", 0xFF06080C, new Runnable() {
            @Override public void run() { showCardActionsDialog(); }
        });
        addRow(d, "PICK PAYMENT APP", 0xFF06080C, new Runnable() {
            @Override public void run() { enterPickWallet(); }
        });
        addRow(d, "CHOOSE CARD ICON", 0xFF06080C, new Runnable() {
            @Override public void run() {
                try {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("image/*");
                    startActivityForResult(i, 10);
                } catch (Exception ex) {
                    toast("NO IMAGE PICKER AVAILABLE");
                }
            }
        });
        addRow(d, "RESET CARD ICON", 0xFF06080C, new Runnable() {
            @Override public void run() {
                new java.io.File(getFilesDir(), "wallet_card.png").delete();
                loadWalletIcon();
            }
        });
        addRow(d, prefs.flag("wallet_hidden")
                ? "SHOW CARD" : "HIDE CARD", 0xFF06080C, new Runnable() {
            @Override public void run() {
                prefs.setFlag("wallet_hidden", !prefs.flag("wallet_hidden"));
                walletCard.setVisibility(
                        prefs.flag("wallet_hidden") ? View.GONE : View.VISIBLE);
            }
        });
        d.show();
    }

    private void showAppsMenu() {
        Dialog d = makeDialog("APPS");
        addRow(d, "HIDDEN APPS", 0xFF06080C, new Runnable() {
            @Override public void run() { showHiddenDialog(); }
        });
        addRow(d, "RELOAD APPS", 0xFF06080C, new Runnable() {
            @Override public void run() {
                loadApps();
                toast("RELOADING…");
            }
        });
        d.show();
    }

    private void showBackupMenu() {
        Dialog d = makeDialog("BACKUP");
        addRow(d, "BACK UP TO DOWNLOADS", 0xFF06080C, new Runnable() {
            @Override public void run() { backupToDownloads(); }
        });
        addRow(d, "RESTORE FROM FILE", 0xFF06080C, new Runnable() {
            @Override public void run() {
                try {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, 8);
                } catch (Exception ex) {
                    toast("NO FILE PICKER AVAILABLE");
                }
            }
        });
        d.show();
    }

    private void showSetupMenu() {
        Dialog d = makeDialog("SETUP");
        addRow(d, "SET AS DEFAULT LAUNCHER", 0xFF06080C, new Runnable() {
            @Override public void run() { requestHomeRole(true); }
        });
        addRow(d, NotifService.isEnabled(this)
                ? "NOTIFICATION ACCESS: ON" : "NOTIFICATION ACCESS: ENABLE",
                0xFF06080C, new Runnable() {
            @Override public void run() {
                try {
                    startActivity(new Intent(
                            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                } catch (Exception ignored) {}
            }
        });
        d.show();
    }

    private boolean isDarkHour(int hour24) {
        return prefs.flag("dh_force") || hour24 == 0;
    }

    // ----------------------------------------------------- card gesture hub

    private boolean torchOn = false;
    private String pickCardKey = null; // gesture being bound to an app

    private String cardAction(String key, String def) {
        String v = prefs.strVal(key);
        return v.length() == 0 ? def : v;
    }

    private void runCardAction(String a) {
        if (a.startsWith("app:")) {
            startQuick(null, a.substring(4));
        } else if (a.equals("wallet")) {
            launchWallet();
        } else if (a.equals("flashlight")) {
            toggleTorch();
        } else if (a.equals("lastapp")) {
            shuffleTime();
        } else if (a.equals("search")) {
            openDrawer();
            searchEdit.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(searchEdit, 0);
        } else if (a.equals("recents")) {
            openRecents();
        }
        // "none": do nothing
    }

    private void toggleTorch() {
        try {
            android.hardware.camera2.CameraManager cm =
                    (android.hardware.camera2.CameraManager)
                            getSystemService(Context.CAMERA_SERVICE);
            for (String id : cm.getCameraIdList()) {
                Boolean flash = cm.getCameraCharacteristics(id).get(
                        android.hardware.camera2.CameraCharacteristics
                                .FLASH_INFO_AVAILABLE);
                if (flash != null && flash.booleanValue()) {
                    torchOn = !torchOn;
                    cm.setTorchMode(id, torchOn);
                    toast(torchOn ? "TORCH ON" : "TORCH OFF");
                    return;
                }
            }
            toast("NO FLASH ON THIS DEVICE");
        } catch (Exception ex) {
            toast("TORCH UNAVAILABLE");
        }
    }

    /** "Shuffle Time": draw the card — returns you to the last app used. */
    private void shuffleTime() {
        List<String> r = prefs.recents();
        if (r.isEmpty()) {
            toast("NO CARD TO DRAW YET");
            return;
        }
        final String pkg = r.get(0);
        walletCard.animate().rotationY(88f).setDuration(130)
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        walletCard.setRotationY(0f);
                        startQuick(null, pkg);
                    }
                }).start();
    }

    private void cardWiggle() {
        walletCard.animate().rotationBy(5f).setDuration(70)
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        walletCard.animate().rotationBy(-5f).setDuration(140)
                                .setInterpolator(
                                        new android.view.animation
                                                .OvershootInterpolator(2f))
                                .start();
                    }
                }).start();
    }

    private String cardActionLabel(String a) {
        if (a.startsWith("app:")) {
            for (AppEntry e : apps) {
                if (e.pkg.equals(a.substring(4))) return e.labelUp;
            }
            return "APP";
        }
        if (a.equals("wallet")) return "WALLET";
        if (a.equals("flashlight")) return "FLASHLIGHT";
        if (a.equals("lastapp")) return "SHUFFLE TIME";
        if (a.equals("search")) return "SEARCH";
        if (a.equals("recents")) return "RECENTS";
        return "NONE";
    }

    private void showCardActionsDialog() {
        Dialog d = makeDialog("CARD ACTIONS");
        String[][] gestures = {
                {"card_double", "DOUBLE TAP", "flashlight"},
                {"card_hold", "HOLD", "wallet"},
                {"card_swipe", "SWIPE UP", "lastapp"}};
        for (final String[] g : gestures) {
            addRow(d, g[1] + ":  " + cardActionLabel(cardAction(g[0], g[2])),
                    0xFF06080C, new Runnable() {
                @Override public void run() { showCardActionPicker(g[0], g[1]); }
            });
        }
        d.show();
    }

    private void showCardActionPicker(final String key, String gestureName) {
        Dialog d = makeDialog(gestureName);
        String[][] opts = {{"wallet", "WALLET"}, {"flashlight", "FLASHLIGHT"},
                {"lastapp", "SHUFFLE TIME (LAST APP)"}, {"search", "SEARCH"},
                {"recents", "RECENTS"}, {"none", "NONE"}};
        for (final String[] o : opts) {
            addRow(d, o[1], 0xFF06080C, new Runnable() {
                @Override public void run() {
                    prefs.setStrVal(key, o[0]);
                    toast("SET");
                }
            });
        }
        addRow(d, "CHOOSE APP…", 0xFF06080C, new Runnable() {
            @Override public void run() {
                pickCardKey = key;
                enterPickCardAction();
            }
        });
        d.show();
    }

    private void enterPickCardAction() {
        pickWallet = false;
        addModeCat = null;
        pickSlot = -1;
        currentCat = null;
        adapter.setAddMembers(null);
        adapter.setOrderedPkgs(null);
        adapter.setCatPkgs(null);
        drawerHeader.setText("PICK APP");
        listView.resetFocus();
        if (!drawerOpen) {
            drawerOpen = true;
            listView.setSelection(0);
            animateDrawer(1f);
        }
        toast("TAP AN APP FOR THE GESTURE");
    }

    // ----------------------------------------------------------- wallet card

    private void launchWallet() {
        String pkg = prefs.strVal("wallet_pkg");
        if (pkg.length() == 0) pkg = "com.samsung.android.spay";

        // Samsung Wallet: try to land on the quick-access card screen.
        // Nothing here is documented, so fire a chain of known handles and
        // fall back to the plain launch if the version has sealed them all.
        if ("com.samsung.android.spay".equals(pkg)) {
            // 1) samsungpay:// deep-link URIs (used in Samsung's own promos)
            String[] uris = {
                    "samsungpay://launch?action=quick_access",
                    "samsungpay://quickaccess",
                    "samsungpay://payment",
                    "samsungpay://launch"
            };
            for (String u : uris) {
                try {
                    Intent vi = new Intent(Intent.ACTION_VIEW, Uri.parse(u));
                    vi.setPackage(pkg);
                    vi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(vi);
                    flashAccent();
                    return;
                } catch (Exception ignored) {}
            }
            // 2) known internal pay activities (older Wallet builds)
            String[] candidates = {
                    "com.samsung.android.spay.ui.list.SpayPayMainActivity",
                    "com.samsung.android.spay.ui.SpayMainActivity"
            };
            for (String cls : candidates) {
                try {
                    Intent qi = new Intent(Intent.ACTION_MAIN);
                    qi.setClassName(pkg, cls);
                    qi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(qi);
                    flashAccent();
                    return;
                } catch (Exception ignored) {}
            }
            try {
                Intent ai = new Intent("com.samsung.android.spay.action.QUICK_PAY");
                ai.setPackage(pkg);
                ai.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(ai);
                flashAccent();
                return;
            } catch (Exception ignored) {}
        }

        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) {
                toast("WALLET APP NOT FOUND — SET IT VIA CLOCK MENU");
                return;
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            flashAccent();
        } catch (Exception ex) {
            toast("CAN'T OPEN WALLET");
        }
    }

    private void loadWalletIcon() {
        try {
            java.io.File f = new java.io.File(getFilesDir(), "wallet_card.png");
            if (f.exists()) {
                walletCard.setCustomIcon(decodeScaled(f.getAbsolutePath(), 512));
            } else {
                walletCard.setCustomIcon(null);
            }
        } catch (Throwable ignored) {
            // OOM or decode failure: fall back to the drawn face, don't crash
            try { walletCard.setCustomIcon(null); } catch (Throwable ignore2) {}
        }
    }

    /** Decode a bitmap downscaled so its largest side is ~maxPx (avoids OOM). */
    private android.graphics.Bitmap decodeScaled(String path, int maxPx) {
        android.graphics.BitmapFactory.Options o =
                new android.graphics.BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeFile(path, o);
        int big = Math.max(o.outWidth, o.outHeight);
        int sample = 1;
        while (big / sample > maxPx) sample *= 2;
        android.graphics.BitmapFactory.Options d =
                new android.graphics.BitmapFactory.Options();
        d.inSampleSize = sample;
        return android.graphics.BitmapFactory.decodeFile(path, d);
    }

    private void enterPickWallet() {
        pickWallet = true;
        addModeCat = null;
        pickSlot = -1;
        currentCat = null;
        adapter.setAddMembers(null);
        adapter.setOrderedPkgs(null);
        adapter.setCatPkgs(null);
        drawerHeader.setText("PICK WALLET APP");
        listView.resetFocus();
        if (!drawerOpen) {
            drawerOpen = true;
            listView.setSelection(0);
            animateDrawer(1f);
        }
        toast("TAP YOUR PAYMENT APP");
    }

    // ---------------------------------------------------------------- weather

    private void maybeFetchWeather() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            if (!prefs.flag("asked_loc")) {
                prefs.setFlag("asked_loc", true);
                requestPermissions(new String[]{
                        android.Manifest.permission.ACCESS_COARSE_LOCATION}, 9);
            }
            return;
        }
        // show cached immediately; refresh if older than 30 minutes
        showWeather(prefs.strVal("wx_text"));
        if (System.currentTimeMillis() - prefs.longVal("wx_time") < 30 * 60 * 1000L) {
            return;
        }
        Thread t = new Thread(new Runnable() {
            @Override public void run() { fetchWeather(); }
        }, "bluehour-wx");
        t.start();
    }

    private void fetchWeather() {
        try {
            android.location.LocationManager lm = (android.location.LocationManager)
                    getSystemService(Context.LOCATION_SERVICE);
            android.location.Location loc = null;
            String[] providers = {android.location.LocationManager.NETWORK_PROVIDER,
                    android.location.LocationManager.PASSIVE_PROVIDER,
                    android.location.LocationManager.GPS_PROVIDER};
            for (String pr : providers) {
                try {
                    loc = lm.getLastKnownLocation(pr);
                } catch (Exception ignored) {}
                if (loc != null) break;
            }
            if (loc == null) return;

            boolean useF = "US".equals(Locale.getDefault().getCountry());
            String url = "https://api.open-meteo.com/v1/forecast?latitude="
                    + loc.getLatitude() + "&longitude=" + loc.getLongitude()
                    + "&current=temperature_2m,weather_code,is_day"
                    + (useF ? "&temperature_unit=fahrenheit" : "");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL(url).openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            java.io.InputStream is = conn.getInputStream();
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            is.close();
            conn.disconnect();

            org.json.JSONObject o = new org.json.JSONObject(bos.toString("UTF-8"))
                    .getJSONObject("current");
            int temp = (int) Math.round(o.getDouble("temperature_2m"));
            int code = o.getInt("weather_code");
            boolean day = o.optInt("is_day", 1) == 1;

            String desc;
            if (code == 0) desc = day ? "CLEAR SKY" : "CLEAR NIGHT";
            else if (code <= 2) desc = "PARTLY CLOUDY";
            else if (code == 3) desc = "OVERCAST";
            else if (code <= 48) desc = "FOGGY";
            else if (code <= 57) desc = "DRIZZLE";
            else if (code <= 67) desc = "RAIN";
            else if (code <= 77) desc = "SNOW";
            else if (code <= 82) desc = "SHOWERS";
            else if (code <= 86) desc = "SNOW SHOWERS";
            else desc = "THUNDERSTORM";

            final String text = (day ? "☀  " : "☾  ") + desc + " · " + temp + "°";
            prefs.setStrVal("wx_text", text);
            prefs.setLongVal("wx_time", System.currentTimeMillis());
            runOnUiThread(new Runnable() {
                @Override public void run() { showWeather(text); }
            });
        } catch (Exception ignored) {
            // offline or API hiccup: keep whatever we had
        }
    }

    private void showWeather(String text) {
        if (weatherText == null) return;
        if (text == null || text.length() == 0) {
            weatherText.setVisibility(View.GONE);
        } else {
            weatherText.setText(text);
            weatherText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 9 && grantResults.length > 0
                && grantResults[0]
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            maybeFetchWeather();
        }
    }

    private void applyCrt() {
        if (crtView == null) return;
        boolean on = prefs.flag("crt");
        // amber tint in Yellow, faint red in Red, neutral in Blue
        int id = Theme.get().id;
        int tint = id == 1 ? 0x14C89000 : (id == 2 ? 0x16C80014 : 0x0C000000);
        crtView.setEnabled2(on, on ? tint : 0);
    }

    private void showThemeDialog() {
        Dialog d = makeDialog("CHOOSE HOUR");
        for (final Theme t : Theme.all()) {
            String mark = t.id == Theme.get().id ? "▶ " : "";
            addRow(d, mark + t.name, 0xFF06080C, new Runnable() {
                @Override public void run() {
                    prefs.setIntVal("theme", t.id);
                    Theme.set(t.id);
                    recreate(); // rebuild all views with the new palette
                }
            });
        }
        d.show();
    }

    private void backupToDownloads() {
        try {
            String json = prefs.exportJson();
            String name = "bluehour_backup_" + new SimpleDateFormat(
                    "yyyyMMdd_HHmm", Locale.US).format(
                    Calendar.getInstance().getTime()) + ".json";
            if (Build.VERSION.SDK_INT >= 29) {
                android.content.ContentValues cv = new android.content.ContentValues();
                cv.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name);
                cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,
                        "application/json");
                cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS);
                Uri uri = getContentResolver().insert(
                        android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri == null) {
                    toast("BACKUP FAILED");
                    return;
                }
                java.io.OutputStream os = getContentResolver().openOutputStream(uri);
                os.write(json.getBytes("UTF-8"));
                os.close();
            } else {
                java.io.File dir = getExternalFilesDir(null);
                java.io.File f = new java.io.File(dir, name);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
                fos.write(json.getBytes("UTF-8"));
                fos.close();
            }
            toast("SAVED TO DOWNLOADS: " + name);
        } catch (Exception ex) {
            toast("BACKUP FAILED");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null || data.getData() == null) return;
        if (requestCode == 10) {
            // custom wallet-card face
            try {
                java.io.InputStream is = getContentResolver()
                        .openInputStream(data.getData());
                java.io.File f = new java.io.File(getFilesDir(), "wallet_card.png");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
                fos.close();
                is.close();
                loadWalletIcon();
                toast("CARD FACE SET");
            } catch (Exception ex) {
                toast("COULDN'T LOAD IMAGE");
            }
            return;
        }
        if (requestCode != 8) return;
        try {
            java.io.InputStream is = getContentResolver()
                    .openInputStream(data.getData());
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            is.close();
            prefs.importJson(bos.toString("UTF-8"));
            adapter.setHidden(prefs.hidden());
            loadApps();
            toast("SETUP RESTORED");
        } catch (Exception ex) {
            toast("RESTORE FAILED — NOT A VALID BACKUP?");
        }
    }

    private void showHiddenDialog() {
        Dialog d = makeDialog("HIDDEN APPS");
        Set<String> hidden = prefs.hidden();
        if (hidden.isEmpty()) {
            addRow(d, "NOTHING HIDDEN", 0x8806080C, new Runnable() {
                @Override public void run() {}
            });
        } else {
            for (final String pkg : hidden) {
                String label = pkg;
                for (AppEntry e : apps) {
                    if (e.pkg.equals(pkg)) { label = e.labelUp; break; }
                }
                addRow(d, "UNHIDE: " + label, 0xFF06080C, new Runnable() {
                    @Override public void run() {
                        prefs.unhide(pkg);
                        adapter.setHidden(prefs.hidden());
                        rebuildDock();
                        toast("RESTORED");
                    }
                });
            }
        }
        d.show();
    }

    // ------------------------------------------------------------------- role

    private void requestHomeRole(boolean force) {
        if (Build.VERSION.SDK_INT < 29) {
            try {
                startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            } catch (Exception ignored) {}
            return;
        }
        RoleManager rm = (RoleManager) getSystemService(Context.ROLE_SERVICE);
        if (rm == null || !rm.isRoleAvailable(RoleManager.ROLE_HOME)) return;
        if (rm.isRoleHeld(RoleManager.ROLE_HOME)) return;
        if (!force && prefs.flag("askedRole")) return;
        prefs.setFlag("askedRole", true);
        try {
            startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), 7);
        } catch (Exception ex) {
            try {
                startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            } catch (Exception ignored) {}
        }
    }

    private void updateDefaultHint() {
        boolean held = true;
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager rm = (RoleManager) getSystemService(Context.ROLE_SERVICE);
            held = rm == null || rm.isRoleHeld(RoleManager.ROLE_HOME);
        }
        defaultHint.setVisibility(held ? View.GONE : View.VISIBLE);
    }

    // -------------------------------------------------------------- lifecycle

    // ------------------------------------------------------------- media

    private final android.media.session.MediaController.Callback mediaCb =
            new android.media.session.MediaController.Callback() {
        @Override
        public void onMetadataChanged(android.media.MediaMetadata metadata) {
            updatePlayerWidget();
        }
        @Override
        public void onPlaybackStateChanged(
                android.media.session.PlaybackState state) {
            updatePlayerWidget();
        }
    };

    private final android.media.session.MediaSessionManager
            .OnActiveSessionsChangedListener sessionsListener =
            new android.media.session.MediaSessionManager
                    .OnActiveSessionsChangedListener() {
        @Override
        public void onActiveSessionsChanged(
                List<android.media.session.MediaController> controllers) {
            pickController(controllers);
        }
    };

    private void pickController(
            List<android.media.session.MediaController> list) {
        android.media.session.MediaController best = null;
        if (list != null) {
            for (android.media.session.MediaController mc : list) {
                if (mc.getMetadata() == null) continue;
                android.media.session.PlaybackState st = mc.getPlaybackState();
                boolean isPlaying = st != null && st.getState()
                        == android.media.session.PlaybackState.STATE_PLAYING;
                if (best == null || isPlaying) best = mc;
                if (isPlaying) break;
            }
        }
        if (mediaController != null) {
            try { mediaController.unregisterCallback(mediaCb); } catch (Exception ignored) {}
        }
        mediaController = best;
        if (best != null) best.registerCallback(mediaCb);
        updatePlayerWidget();
    }

    private void updatePlayerWidget() {
        if (playerView == null) return;
        android.media.MediaMetadata md =
                mediaController == null ? null : mediaController.getMetadata();
        if (mediaController == null || md == null) {
            playerView.hideAnimated();
            updateClock(); // alarm box decides its own visibility
            return;
        }
        String t = md.getString(android.media.MediaMetadata.METADATA_KEY_TITLE);
        String a = md.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST);
        long dur = md.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION);
        android.graphics.Bitmap art = md.getBitmap(
                android.media.MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (art == null) art = md.getBitmap(
                android.media.MediaMetadata.METADATA_KEY_ART);
        playerView.setAlbumArt(art);
        android.media.session.PlaybackState st = mediaController.getPlaybackState();
        boolean isPlaying = st != null && st.getState()
                == android.media.session.PlaybackState.STATE_PLAYING;
        long pos = st == null ? 0 : st.getPosition();
        long posTime = st == null ? android.os.SystemClock.elapsedRealtime()
                : st.getLastPositionUpdateTime();
        float spd = st == null ? 1f : st.getPlaybackSpeed();
        battBox.setVisibility(View.GONE);
        playerView.showAnimated();
        playerView.update(t == null ? "—" : t, a == null ? "" : a,
                isPlaying, dur, pos, posTime, spd);
    }

    private void startMediaWatch() {
        try {
            msm = (android.media.session.MediaSessionManager)
                    getSystemService(Context.MEDIA_SESSION_SERVICE);
            android.content.ComponentName cn =
                    new android.content.ComponentName(this, NotifService.class);
            msm.addOnActiveSessionsChangedListener(sessionsListener, cn);
            pickController(msm.getActiveSessions(cn));
        } catch (Exception ex) {
            // notification access not granted — widget stays hidden
            if (playerView != null) playerView.setVisibility(View.GONE);
        }
    }

    private void stopMediaWatch() {
        try {
            if (msm != null) {
                msm.removeOnActiveSessionsChangedListener(sessionsListener);
            }
        } catch (Exception ignored) {}
        if (mediaController != null) {
            try { mediaController.unregisterCallback(mediaCb); } catch (Exception ignored) {}
            mediaController = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startMediaWatch();
        maybeFetchWeather();
        IntentFilter tf = new IntentFilter();
        tf.addAction(Intent.ACTION_TIME_TICK);
        tf.addAction(Intent.ACTION_TIME_CHANGED);
        tf.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        regReceiver(timeReceiver, tf);

        regReceiver(notifReceiver, new IntentFilter(NotifService.ACTION_CHANGED));

        IntentFilter pf = new IntentFilter();
        pf.addAction(Intent.ACTION_PACKAGE_ADDED);
        pf.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pf.addAction(Intent.ACTION_PACKAGE_CHANGED);
        pf.addDataScheme("package");
        regReceiver(pkgReceiver, pf);
    }

    private void regReceiver(BroadcastReceiver r, IntentFilter f) {
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(r, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(r, f);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateClock();
        updateDots();
        updateDefaultHint();
        requestHomeRole(false);
        if (crtView != null) crtView.hostResumed();
        // kinetic menu entrance when returning to home from another app
        if (wasStopped) {
            wasStopped = false;
            animateMenuIn();
        }
    }

    /** P3R-style staggered slide-in of the category column. */
    private void animateMenuIn() {
        if (dockRow == null || drawerOpen) return;
        for (int i = 0; i < dockRow.getChildCount(); i++) {
            View v = dockRow.getChildAt(i);
            v.animate().cancel();
            v.setTranslationX(Ui.dp(this, 64));
            v.setAlpha(0f);
            v.animate().translationX(0f).alpha(1f)
                    .setStartDelay(i * 26L).setDuration(300)
                    .setInterpolator(new android.view.animation
                            .OvershootInterpolator(1.5f)).start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        wasStopped = true;
        stopMediaWatch();
        if (crtView != null) crtView.hostStopped();
        if (playerView != null) playerView.hostStopped();
        try { unregisterReceiver(timeReceiver); } catch (Exception ignored) {}
        try { unregisterReceiver(notifReceiver); } catch (Exception ignored) {}
        try { unregisterReceiver(pkgReceiver); } catch (Exception ignored) {}
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
