package com.aaron.bluehour;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.service.notification.NotificationListenerService;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.aaron.bluehour.AlphaRail;
import com.aaron.bluehour.AppRepository;
import com.aaron.bluehour.MailView;
import com.aaron.bluehour.MediaHubView;
import com.aaron.bluehour.NotifService;
import com.aaron.bluehour.PlayerView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class MainActivity extends Activity {
    private static final int HOME_CAT_MAX = 8;
    private AppsAdapter adapter;
    private LinearLayout battBox;
    private TextView battText;
    private BackgroundView bg;
    private BulletinView bulletinView;
    private LinearLayout clockBox;
    private CRTView crtView;
    private TextView dateText;
    private TextView dayText;
    private TextView defaultHint;
    private LinearLayout dockRow;
    private ValueAnimator drawerAnim;
    private TextView drawerHeader;
    private LinearLayout drawerLayer;
    private FiligreeView filigreeView;
    private View flash;
    private HalftoneView halftoneView;
    private GestureDetector homeGd;
    private FrameLayout homeLayer;
    private LauncherApps launcherApps;
    private AppsListView listView;
    private MailButton mailButton;
    private MailView mailView;
    private MediaController mediaController;
    private MediaHubView mediaHub;
    private MediaSessionManager msm;
    private TextView phaseText;
    private PlayerView playerView;
    private Prefs prefs;
    private LinearLayout quickRow;
    private FrameLayout root;
    private View ruleView;
    private EditText searchEdit;
    private FrameLayout searchRowV;
    private long swDownT;
    private float swDownX;
    private float swDownY;
    private ClockView timeText;
    private CardButton walletCard;
    private TextView weatherText;
    private boolean wasStopped = false;
    private boolean pickWallet = false;
    private boolean lastDh = false;
    private List<AppEntry> apps = new ArrayList();
    private final HashMap<String, Integer> usesMap = new HashMap<>();
    private boolean drawerOpen = false;
    private float drawerPos = 0.0f;
    private String currentCat = null;
    private String addModeCat = null;
    private int pickSlot = -1;
    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() { // from class: com.aaron.bluehour.MainActivity.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            MainActivity.this.updateClock();
        }
    };
    private final BroadcastReceiver notifReceiver = new BroadcastReceiver() { // from class: com.aaron.bluehour.MainActivity.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            MainActivity.this.updateDots();
            MainActivity.this.refreshMediaWatch();
        }
    };
    private final BroadcastReceiver pkgReceiver = new BroadcastReceiver() { // from class: com.aaron.bluehour.MainActivity.3
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.PACKAGE_REMOVED".equals(intent.getAction()) && !intent.getBooleanExtra("android.intent.extra.REPLACING", false) && intent.getData() != null) {
                MainActivity.this.prefs.removePkgEverywhere(intent.getData().getSchemeSpecificPart());
                MainActivity.this.buildQuickRow();
            }
            MainActivity.this.scheduleAppReload();
        }
    };
    private final Handler appReloadHandler = new Handler(Looper.getMainLooper());
    private final Runnable appReloadRun = new Runnable() { // from class: com.aaron.bluehour.MainActivity.4
        @Override // java.lang.Runnable
        public void run() {
            MainActivity.this.loadApps();
        }
    };
    private boolean firstStart = true;
    private final LauncherApps.Callback launcherCb = new LauncherApps.Callback() { // from class: com.aaron.bluehour.MainActivity.5
        @Override // android.content.pm.LauncherApps.Callback
        public void onPackageAdded(String str, UserHandle userHandle) {
            MainActivity.this.scheduleAppReload();
        }

        @Override // android.content.pm.LauncherApps.Callback
        public void onPackageChanged(String str, UserHandle userHandle) {
            MainActivity.this.scheduleAppReload();
        }

        @Override // android.content.pm.LauncherApps.Callback
        public void onPackageRemoved(String str, UserHandle userHandle) {
            MainActivity.this.scheduleAppReload();
        }

        @Override // android.content.pm.LauncherApps.Callback
        public void onPackagesAvailable(String[] strArr, UserHandle userHandle, boolean z) {
            MainActivity.this.scheduleAppReload();
        }

        @Override // android.content.pm.LauncherApps.Callback
        public void onPackagesUnavailable(String[] strArr, UserHandle userHandle, boolean z) {
            MainActivity.this.scheduleAppReload();
        }
    };
    private boolean quickAwaitingIcons = false;
    private boolean torchOn = false;
    private String pickCardKey = null;
    private final MediaController.Callback mediaCb = new MediaController.Callback() { // from class: com.aaron.bluehour.MainActivity.107
        @Override // android.media.session.MediaController.Callback
        public void onMetadataChanged(MediaMetadata mediaMetadata) {
            MainActivity.this.updatePlayerWidget();
        }

        @Override // android.media.session.MediaController.Callback
        public void onPlaybackStateChanged(PlaybackState playbackState) {
            MainActivity.this.updatePlayerWidget();
        }
    };
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsListener = new MediaSessionManager.OnActiveSessionsChangedListener() { // from class: com.aaron.bluehour.MainActivity.108
        @Override // android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
        public void onActiveSessionsChanged(List<MediaController> list) {
            MainActivity.this.pickController(list);
        }
    };

    private interface NameCallback {
        void onName(String str, String str2);
    }

    private void addCatWord(String str, String str2, int i, final String str3) {
        MenuWordView menuWordView = new MenuWordView(this, str, str2, i);
        menuWordView.setTag(str3);
        menuWordView.setDot(catHasDot(str3));
        menuWordView.setOnClickListener(new View.OnClickListener() { // from class: com.aaron.bluehour.MainActivity.37
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                view.postDelayed(new Runnable() { // from class: com.aaron.bluehour.MainActivity.37.1
                    @Override // java.lang.Runnable
                    public void run() {
                        MainActivity.this.openDrawerFor(str3.length() == 0 ? null : str3);
                    }
                }, 90L);
            }
        });
        if (str3.length() > 0) {
            menuWordView.setLongClickable(true);
            menuWordView.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.aaron.bluehour.MainActivity.38
                @Override // android.view.View.OnLongClickListener
                public boolean onLongClick(View view) {
                    MainActivity.this.showCatManageDialog(str3);
                    return true;
                }
            });
        }
        this.dockRow.addView(menuWordView, wordLp());
    }

    private void addRow(final Dialog dialog, String str, int i, final Runnable runnable) {
        LinearLayout linearLayout = (LinearLayout) dialog.getWindow().getDecorView().findViewById(android.R.id.list);
        TextView dialogRow = dialogRow(str, i);
        dialogRow.setOnClickListener(new View.OnClickListener() { // from class: com.aaron.bluehour.MainActivity.60
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                dialog.dismiss();
                runnable.run();
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = Ui.dpi(this, 7.0f);
        linearLayout.addView(dialogRow, layoutParams);
    }

    private void animateDrawer(float f) {
        if (this.drawerAnim != null) {
            this.drawerAnim.cancel();
        }
        this.drawerAnim = ValueAnimator.ofFloat(this.drawerPos, f);
        this.drawerAnim.setDuration(380L);
        this.drawerAnim.setInterpolator(new DecelerateInterpolator(1.9f));
        this.drawerAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.aaron.bluehour.MainActivity.29
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                MainActivity.this.setDrawerPos(((Float) valueAnimator.getAnimatedValue()).floatValue());
            }
        });
        this.drawerAnim.start();
    }

    private void animateMenuIn() {
        if (this.dockRow == null || this.drawerOpen) {
            return;
        }
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= this.dockRow.getChildCount()) {
                return;
            }
            View childAt = this.dockRow.getChildAt(i2);
            float restingTx = childAt instanceof MenuWordView ? ((MenuWordView) childAt).restingTx() : 0.0f;
            childAt.animate().cancel();
            childAt.setTranslationX(Ui.dp(this, 64.0f) + restingTx);
            childAt.setAlpha(0.0f);
            childAt.animate().translationX(restingTx).alpha(1.0f).setStartDelay(i2 * 26).setDuration(300L).setInterpolator(new OvershootInterpolator(1.5f)).start();
            i = i2 + 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public AppEntry appForPkg(String str) {
        if (str == null) {
            return null;
        }
        for (AppEntry appEntry : this.apps) {
            if (appEntry.pkg.equals(str)) {
                return appEntry;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyCrt() {
        if (this.crtView == null) {
            return;
        }
        boolean flag = this.prefs.flag("crt");
        int i = Theme.get().id;
        int i2 = i == 1 ? 348688384 : i == 2 ? 382205972 : i == 3 ? 381192491 : 201326592;
        CRTView cRTView = this.crtView;
        if (!flag) {
            i2 = 0;
        }
        cRTView.setEnabled2(flag, i2);
    }

    private void applyFoldLayout() {
        if (this.homeLayer == null) {
            return;
        }
        final boolean isUnfolded = isUnfolded();
        int homeHPad = homeHPad();
        this.homeLayer.setPadding(homeHPad, this.homeLayer.getPaddingTop(), homeHPad, this.homeLayer.getPaddingBottom());
        if (this.clockBox != null) {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2, 8388659);
            layoutParams.topMargin = Ui.dpi(this, -14.0f);
            layoutParams.leftMargin = Ui.dpi(this, -26.0f);
            this.clockBox.setLayoutParams(layoutParams);
        }
        if (this.dockRow != null) {
            FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(-2, -2, isUnfolded ? 8388629 : 8388661);
            layoutParams2.rightMargin = Ui.dpi(this, 4.0f);
            layoutParams2.topMargin = isUnfolded ? 0 : Ui.dpi(this, 140.0f);
            this.dockRow.setLayoutParams(layoutParams2);
            final float f = isUnfolded ? 1.0f : 0.78f;
            final LinearLayout linearLayout = this.dockRow;
            linearLayout.setScaleX(f);
            linearLayout.setScaleY(f);
            linearLayout.post(new Runnable() { // from class: com.aaron.bluehour.MainActivity.8
                @Override // java.lang.Runnable
                public void run() {
                    linearLayout.setPivotX(isUnfolded ? linearLayout.getWidth() / 2.0f : linearLayout.getWidth());
                    linearLayout.setPivotY(isUnfolded ? linearLayout.getHeight() / 2.0f : 0.0f);
                    linearLayout.setScaleX(f);
                    linearLayout.setScaleY(f);
                }
            });
        }
        int i = isUnfolded ? 49 : 8388661;
        if (this.playerView != null) {
            FrameLayout.LayoutParams layoutParams3 = new FrameLayout.LayoutParams(-2, -2, i);
            layoutParams3.topMargin = Ui.dpi(this, isUnfolded ? 14.0f : 28.0f);
            layoutParams3.rightMargin = isUnfolded ? 0 : Ui.dpi(this, 4.0f);
            this.playerView.setLayoutParams(layoutParams3);
        }
        if (this.battBox != null) {
            FrameLayout.LayoutParams layoutParams4 = new FrameLayout.LayoutParams(-2, -2, i);
            layoutParams4.topMargin = Ui.dpi(this, isUnfolded ? 14.0f : 46.0f);
            layoutParams4.rightMargin = isUnfolded ? 0 : Ui.dpi(this, 14.0f);
            this.battBox.setLayoutParams(layoutParams4);
        }
        if (this.mailButton != null) {
            FrameLayout.LayoutParams layoutParams5 = new FrameLayout.LayoutParams(-2, -2, 8388661);
            layoutParams5.topMargin = Ui.dpi(this, 2.0f);
            layoutParams5.rightMargin = Ui.dpi(this, 6.0f);
            this.mailButton.setLayoutParams(layoutParams5);
        }
        if (this.quickRow != null) {
            FrameLayout.LayoutParams layoutParams6 = new FrameLayout.LayoutParams(-2, -2, 81);
            layoutParams6.bottomMargin = Ui.dpi(this, 18.0f);
            this.quickRow.setLayoutParams(layoutParams6);
        }
        if (this.walletCard != null) {
            FrameLayout.LayoutParams layoutParams7 = new FrameLayout.LayoutParams(-2, -2, 8388691);
            layoutParams7.leftMargin = Ui.dpi(this, 4.0f);
            layoutParams7.bottomMargin = Ui.dpi(this, isUnfolded ? 20.0f : 96.0f);
            this.walletCard.setLayoutParams(layoutParams7);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyInsets(WindowInsets windowInsets) {
        int systemWindowInsetTop;
        int systemWindowInsetBottom;
        int i;
        int i2;
        if (Build.VERSION.SDK_INT >= 30) {
            Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            int i3 = windowInsets.getInsets(WindowInsets.Type.ime()).bottom;
            systemWindowInsetTop = insets.top;
            systemWindowInsetBottom = Math.max(insets.bottom, i3);
        } else {
            systemWindowInsetTop = windowInsets.getSystemWindowInsetTop();
            systemWindowInsetBottom = windowInsets.getSystemWindowInsetBottom();
        }
        int homeHPad = homeHPad();
        this.homeLayer.setPadding(homeHPad, Ui.dpi(this, 14.0f) + systemWindowInsetTop, homeHPad, Ui.dpi(this, 10.0f) + systemWindowInsetBottom);
        this.drawerLayer.setPadding(0, Ui.dpi(this, 10.0f) + systemWindowInsetTop, 0, systemWindowInsetBottom);
        if (this.filigreeView != null) {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets insets2 = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                int i4 = insets2.left;
                i = insets2.right;
                i2 = i4;
            } else {
                i = 0;
                i2 = 0;
            }
            this.filigreeView.setInsets(systemWindowInsetTop, systemWindowInsetBottom, i2, i);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void backupToDownloads() {
        try {
            String exportJson = this.prefs.exportJson();
            String str = "bluehour_backup_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Calendar.getInstance().getTime()) + ".json";
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("_display_name", str);
                contentValues.put("mime_type", "application/json");
                contentValues.put("relative_path", Environment.DIRECTORY_DOWNLOADS);
                Uri insert = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (insert == null) {
                    toast("BACKUP FAILED");
                    return;
                } else {
                    OutputStream openOutputStream = getContentResolver().openOutputStream(insert);
                    openOutputStream.write(exportJson.getBytes("UTF-8"));
                    openOutputStream.close();
                }
            } else {
                FileOutputStream fileOutputStream = new FileOutputStream(new File(getExternalFilesDir(null), str));
                fileOutputStream.write(exportJson.getBytes("UTF-8"));
                fileOutputStream.close();
            }
            toast("SAVED TO DOWNLOADS: " + str);
        } catch (Exception e) {
            toast("BACKUP FAILED");
        }
    }

    private void buildDrawerLayer() {
        this.drawerLayer = new LinearLayout(this);
        this.drawerLayer.setOrientation(1);
        this.drawerLayer.setClickable(true);
        this.root.addView(this.drawerLayer, new FrameLayout.LayoutParams(-1, -1));
        this.drawerHeader = new TextView(this);
        this.drawerHeader.setTypeface(Ui.tf(this));
        this.drawerHeader.setTextSize(2, 52.0f);
        this.drawerHeader.setTextColor(-16118768);
        this.drawerHeader.setLetterSpacing(-0.03f);
        this.drawerHeader.setText("APPS");
        this.drawerHeader.setIncludeFontPadding(false);
        this.drawerHeader.setSingleLine(true);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.leftMargin = Ui.dpi(this, 24.0f);
        layoutParams.topMargin = Ui.dpi(this, 8.0f);
        this.drawerLayer.addView(this.drawerHeader, layoutParams);
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setBackground(new SkewDrawable(-1, 0.0f, -16118768, Ui.dp(this, 1.5f)));
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, Ui.dpi(this, 46.0f));
        layoutParams2.leftMargin = Ui.dpi(this, 26.0f);
        layoutParams2.rightMargin = Ui.dpi(this, 26.0f);
        layoutParams2.topMargin = Ui.dpi(this, 6.0f);
        this.drawerLayer.addView(frameLayout, layoutParams2);
        this.searchRowV = frameLayout;
        this.searchEdit = new EditText(this);
        this.searchEdit.setBackground(null);
        this.searchEdit.setTypeface(Ui.tf(this));
        this.searchEdit.setTextSize(2, 17.0f);
        this.searchEdit.setTextColor(-16381940);
        this.searchEdit.setHintTextColor(1711671308);
        this.searchEdit.setHint("SEARCH APPS");
        this.searchEdit.setSingleLine(true);
        this.searchEdit.setImeOptions(268435458);
        this.searchEdit.setPadding(Ui.dpi(this, 24.0f), 0, Ui.dpi(this, 24.0f), 0);
        frameLayout.addView(this.searchEdit, new FrameLayout.LayoutParams(-1, -1));
        this.searchEdit.addTextChangedListener(new TextWatcher() { // from class: com.aaron.bluehour.MainActivity.24
            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable editable) {
                MainActivity.this.adapter.setFilter(editable.toString());
                MainActivity.this.listView.resetFocus();
                MainActivity.this.listView.applyFx();
            }

            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }
        });
        this.searchEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: com.aaron.bluehour.MainActivity.25
            @Override // android.widget.TextView.OnEditorActionListener
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                AppEntry first = MainActivity.this.adapter.first();
                if (first != null) {
                    if (MainActivity.this.addModeCat != null) {
                        MainActivity.this.prefs.toggleCatApp(MainActivity.this.addModeCat, first.pkg);
                        MainActivity.this.adapter.setAddMembers(MainActivity.this.prefs.catApps(MainActivity.this.addModeCat));
                    } else if (MainActivity.this.pickCardKey != null) {
                        MainActivity.this.prefs.setStrVal(MainActivity.this.pickCardKey, "app:" + first.pkg);
                        MainActivity.this.pickCardKey = null;
                        MainActivity.this.closeDrawer();
                        MainActivity.this.toast("GESTURE → " + first.labelUp);
                    } else if (MainActivity.this.pickWallet) {
                        MainActivity.this.pickWallet = false;
                        MainActivity.this.prefs.setStrVal("wallet_pkg", first.pkg);
                        MainActivity.this.closeDrawer();
                        MainActivity.this.toast("WALLET → " + first.labelUp);
                    } else if (MainActivity.this.pickSlot >= 0) {
                        int i2 = MainActivity.this.pickSlot;
                        MainActivity.this.pickSlot = -1;
                        MainActivity.this.prefs.setQuickSlot(i2, first.pkg);
                        MainActivity.this.buildQuickRow();
                        MainActivity.this.closeDrawer();
                        MainActivity.this.toast("DOCK SLOT " + (i2 + 1) + " → " + first.labelUp);
                    } else {
                        MainActivity.this.launch(first, textView);
                    }
                }
                return true;
            }
        });
        this.adapter = new AppsAdapter(this);
        this.adapter.setHidden(this.prefs.hidden());
        this.listView = new AppsListView(this);
        this.listView.setAdapter((ListAdapter) this.adapter);
        FrameLayout frameLayout2 = new FrameLayout(this);
        frameLayout2.setClipChildren(false);
        frameLayout2.setClipToPadding(false);
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-1, 0, 1.0f);
        layoutParams3.topMargin = Ui.dpi(this, 8.0f);
        this.drawerLayer.addView(frameLayout2, layoutParams3);
        frameLayout2.addView(this.listView, new FrameLayout.LayoutParams(-1, -1));
        AlphaRail alphaRail = new AlphaRail(this);
        alphaRail.setListener(new AlphaRail.Listener() { // from class: com.aaron.bluehour.MainActivity.26
            @Override // com.aaron.bluehour.AlphaRail.Listener
            public void onLetter(char c) {
                int firstIndexForLetter = MainActivity.this.adapter.firstIndexForLetter(c);
                if (firstIndexForLetter >= 0) {
                    MainActivity.this.listView.setSelection(firstIndexForLetter);
                }
            }
        });
        FrameLayout.LayoutParams layoutParams4 = new FrameLayout.LayoutParams(Ui.dpi(this, 26.0f), -1, 8388613);
        layoutParams4.topMargin = Ui.dpi(this, 6.0f);
        layoutParams4.bottomMargin = Ui.dpi(this, 60.0f);
        frameLayout2.addView(alphaRail, layoutParams4);
        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: com.aaron.bluehour.MainActivity.27
            @Override // android.widget.AdapterView.OnItemClickListener
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
                AppEntry appEntry = (AppEntry) MainActivity.this.adapter.getItem(i);
                if (MainActivity.this.addModeCat != null) {
                    MainActivity.this.prefs.toggleCatApp(MainActivity.this.addModeCat, appEntry.pkg);
                    MainActivity.this.adapter.setAddMembers(MainActivity.this.prefs.catApps(MainActivity.this.addModeCat));
                    view.performHapticFeedback(4);
                    return;
                }
                if (MainActivity.this.pickCardKey != null) {
                    MainActivity.this.prefs.setStrVal(MainActivity.this.pickCardKey, "app:" + appEntry.pkg);
                    MainActivity.this.pickCardKey = null;
                    MainActivity.this.closeDrawer();
                    MainActivity.this.toast("GESTURE → " + appEntry.labelUp);
                    return;
                }
                if (MainActivity.this.pickWallet) {
                    MainActivity.this.pickWallet = false;
                    MainActivity.this.prefs.setStrVal("wallet_pkg", appEntry.pkg);
                    MainActivity.this.closeDrawer();
                    MainActivity.this.toast("WALLET → " + appEntry.labelUp);
                    return;
                }
                if (MainActivity.this.pickSlot < 0) {
                    MainActivity.this.launch(appEntry, view);
                    return;
                }
                int i2 = MainActivity.this.pickSlot;
                MainActivity.this.pickSlot = -1;
                MainActivity.this.prefs.setQuickSlot(i2, appEntry.pkg);
                MainActivity.this.buildQuickRow();
                MainActivity.this.closeDrawer();
                MainActivity.this.toast("DOCK SLOT " + (i2 + 1) + " → " + appEntry.labelUp);
            }
        });
        this.listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() { // from class: com.aaron.bluehour.MainActivity.28
            @Override // android.widget.AdapterView.OnItemLongClickListener
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long j) {
                if (MainActivity.this.addModeCat == null && MainActivity.this.pickSlot < 0 && !MainActivity.this.pickWallet && MainActivity.this.pickCardKey == null) {
                    MainActivity.this.showAppDialog((AppEntry) MainActivity.this.adapter.getItem(i));
                }
                return true;
            }
        });
    }

    private void buildHomeLayer() {
        this.homeLayer = new FrameLayout(this);
        this.homeLayer.setClipChildren(false);
        this.homeLayer.setClipToPadding(false);
        this.root.setClipChildren(false);
        this.root.addView(this.homeLayer, new FrameLayout.LayoutParams(-1, -1));
        this.clockBox = new LinearLayout(this);
        this.clockBox.setOrientation(1);
        this.clockBox.setClipChildren(false);
        this.clockBox.setClipToPadding(false);
        this.clockBox.setClickable(true);
        this.clockBox.setLongClickable(true);
        this.timeText = new ClockView(this);
        this.clockBox.addView(this.timeText);
        this.dayText = new TextView(this);
        this.dayText.setTypeface(Ui.tf(this));
        this.dayText.setTextSize(0, Ui.dp(this, 30.0f));
        this.dayText.setTextColor(-854534);
        this.dayText.setBackground(Theme.get().shapeStyle == 2 ? new JaggedDrawable(-268039662, Ui.dp(this, 3.5f)) : new SkewDrawable(-268039662, Ui.dp(this, 9.0f)));
        this.dayText.setPadding(Ui.dpi(this, 16.0f), Ui.dpi(this, 2.0f), Ui.dpi(this, 22.0f), Ui.dpi(this, 4.0f));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.topMargin = Ui.dpi(this, 0.0f);
        this.clockBox.addView(this.dayText, layoutParams);
        this.dateText = new TextView(this);
        this.dateText.setTypeface(Ui.tf(this));
        this.dateText.setTextSize(0, Ui.dp(this, 22.0f));
        this.dateText.setTextColor(Theme.get().dateInk);
        this.dateText.setBackground(Theme.get().shapeStyle == 2 ? new JaggedDrawable(-1, Ui.dp(this, 3.0f)) : new SkewDrawable(-1, Ui.dp(this, 7.0f)));
        this.dateText.setPadding(Ui.dpi(this, 14.0f), Ui.dpi(this, 2.0f), Ui.dpi(this, 18.0f), Ui.dpi(this, 3.0f));
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams2.topMargin = Ui.dpi(this, 4.0f);
        layoutParams2.leftMargin = Ui.dpi(this, 6.0f);
        this.clockBox.addView(this.dateText, layoutParams2);
        this.phaseText = new TextView(this);
        this.phaseText.setTypeface(Ui.tf(this));
        this.phaseText.setTextSize(0, Ui.dp(this, 24.0f));
        this.phaseText.setTextColor(-9446401);
        this.phaseText.setLetterSpacing(0.1f);
        this.phaseText.setShadowLayer(Ui.dp(this, 5.0f), 0.0f, Ui.dp(this, 2.0f), -1728053248);
        if (Theme.get().shapeStyle == 2) {
            this.phaseText.setBackground(new JaggedDrawable(Theme.get().accent, Ui.dp(this, 3.0f)));
            this.phaseText.setTextColor(-1);
            this.phaseText.setPadding(Ui.dpi(this, 12.0f), Ui.dpi(this, 2.0f), Ui.dpi(this, 14.0f), Ui.dpi(this, 3.0f));
        }
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams3.topMargin = Ui.dpi(this, 5.0f);
        layoutParams3.leftMargin = Ui.dpi(this, 6.0f);
        this.clockBox.addView(this.phaseText, layoutParams3);
        this.ruleView = new View(this);
        this.ruleView.setBackgroundColor(-1275068417);
        LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(Ui.dpi(this, 130.0f), Ui.dpi(this, 2.0f));
        layoutParams4.topMargin = Ui.dpi(this, 5.0f);
        layoutParams4.leftMargin = Ui.dpi(this, 6.0f);
        this.clockBox.addView(this.ruleView, layoutParams4);
        this.weatherText = new TextView(this);
        this.weatherText.setTypeface(Ui.tfUpright(this));
        this.weatherText.setTextSize(0, Ui.dp(this, 15.0f));
        this.weatherText.setTextColor(-854534);
        this.weatherText.setLetterSpacing(0.06f);
        this.weatherText.setShadowLayer(Ui.dp(this, 4.0f), 0.0f, Ui.dp(this, 1.0f), -1728053248);
        this.weatherText.setVisibility(HOME_CAT_MAX);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setClipChildren(false);
        LinearLayout.LayoutParams layoutParams5 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams5.topMargin = Ui.dpi(this, 6.0f);
        layoutParams5.leftMargin = Ui.dpi(this, 6.0f);
        this.clockBox.addView(linearLayout, layoutParams5);
        linearLayout.addView(this.weatherText, new LinearLayout.LayoutParams(-2, -2));
        this.bulletinView = new BulletinView(this);
        this.bulletinView.setMirrored(true);
        LinearLayout.LayoutParams layoutParams6 = new LinearLayout.LayoutParams(Ui.dpi(this, 36.0f), Ui.dpi(this, 42.0f));
        layoutParams6.leftMargin = Ui.dpi(this, 10.0f);
        layoutParams6.gravity = 16;
        this.bulletinView.setOnClickListener(new View.OnClickListener() { // from class: com.aaron.bluehour.MainActivity.9
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.openFeed();
            }
        });
        this.bulletinView.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.aaron.bluehour.MainActivity.10
            @Override // android.view.View.OnLongClickListener
            public boolean onLongClick(View view) {
                MainActivity.this.pickCardKey = "feed_pkg";
                MainActivity.this.enterPickCardAction();
                MainActivity.this.toast("PICK YOUR FEED / NEWS APP");
                return true;
            }
        });
        this.bulletinView.setVisibility(this.prefs.flag("feed_shown") ? 0 : HOME_CAT_MAX);
        linearLayout.addView(this.bulletinView, layoutParams6);
        this.defaultHint = new TextView(this);
        this.defaultHint.setTypeface(Ui.tf(this));
        this.defaultHint.setTextSize(2, 12.0f);
        this.defaultHint.setTextColor(Ui.TEXT_DIM);
        this.defaultHint.setText("NOT DEFAULT LAUNCHER — TAP TO FIX");
        this.defaultHint.setVisibility(HOME_CAT_MAX);
        this.defaultHint.setOnClickListener(new View.OnClickListener() { // from class: com.aaron.bluehour.MainActivity.11
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.requestHomeRole(true);
            }
        });
        LinearLayout.LayoutParams layoutParams7 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams7.topMargin = Ui.dpi(this, 10.0f);
        layoutParams7.leftMargin = Ui.dpi(this, 8.0f);
        this.clockBox.addView(this.defaultHint, layoutParams7);
        FrameLayout.LayoutParams layoutParams8 = new FrameLayout.LayoutParams(-2, -2, 8388659);
        layoutParams8.topMargin = Ui.dpi(this, -14.0f);
        layoutParams8.leftMargin = Ui.dpi(this, -26.0f);
        this.homeLayer.addView(this.clockBox, layoutParams8);
        this.clockBox.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.aaron.bluehour.MainActivity.12
            @Override // android.view.View.OnLongClickListener
            public boolean onLongClick(View view) {
                MainActivity.this.showSettingsDialog();
                return true;
            }
        });
        this.battBox = new LinearLayout(this);
        this.battBox.setOrientation(1);
        this.battBox.setBackground(new SkewDrawable(-1, 0.0f, -16381940, Ui.dp(this, 1.5f)));
        this.battBox.setPadding(Ui.dpi(this, 18.0f), Ui.dpi(this, 8.0f), Ui.dpi(this, 18.0f), Ui.dpi(this, 8.0f));
        this.battText = new TextView(this);
        this.battText.setTypeface(Ui.tfUpright(this));
        this.battText.setTextSize(0, Ui.dp(this, 21.0f));
        this.battText.setTextColor(-16381940);
        this.battText.setIncludeFontPadding(false);
        this.battBox.addView(this.battText);
        TextView textView = new TextView(this);
        textView.setTypeface(Ui.tfUpright(this));
        textView.setTextSize(0, Ui.dp(this, 11.0f));
        textView.setTextColor(-1291450356);
        textView.setText("next alarm");
        this.battBox.addView(textView);
        FrameLayout.LayoutParams layoutParams9 = new FrameLayout.LayoutParams(-2, -2, 8388661);
        layoutParams9.topMargin = Ui.dpi(this, 46.0f);
        layoutParams9.rightMargin = Ui.dpi(this, 14.0f);
        this.homeLayer.addView(this.battBox, layoutParams9);
        this.playerView = new PlayerView(this);
        this.playerView.setVisibility(HOME_CAT_MAX);
        this.playerView.setListener(new PlayerView.Listener() { // from class: com.aaron.bluehour.MainActivity.13
            @Override // com.aaron.bluehour.PlayerView.Listener
            public void onNext() {
                if (MainActivity.this.mediaController != null) {
                    MainActivity.this.mediaController.getTransportControls().skipToNext();
                }
            }

            @Override // com.aaron.bluehour.PlayerView.Listener
            public void onOpenApp() {
                if (MainActivity.this.mediaController == null) {
                    return;
                }
                try {
                    Intent launchIntentForPackage = MainActivity.this.getPackageManager().getLaunchIntentForPackage(MainActivity.this.mediaController.getPackageName());
                    if (launchIntentForPackage != null) {
                        launchIntentForPackage.addFlags(268435456);
                        MainActivity.this.startActivity(launchIntentForPackage);
                        MainActivity.this.flashAccent();
                    }
                } catch (Exception e) {
                }
            }

            @Override // com.aaron.bluehour.PlayerView.Listener
            public void onPlayPause() {
                if (MainActivity.this.mediaController == null) {
                    return;
                }
                PlaybackState playbackState = MainActivity.this.mediaController.getPlaybackState();
                if (playbackState == null || playbackState.getState() != 3) {
                    MainActivity.this.mediaController.getTransportControls().play();
                } else {
                    MainActivity.this.mediaController.getTransportControls().pause();
                }
            }

            @Override // com.aaron.bluehour.PlayerView.Listener
            public void onPrev() {
                if (MainActivity.this.mediaController != null) {
                    MainActivity.this.mediaController.getTransportControls().skipToPrevious();
                }
            }
        });
        FrameLayout.LayoutParams layoutParams10 = new FrameLayout.LayoutParams(-2, -2, 8388661);
        layoutParams10.topMargin = Ui.dpi(this, 28.0f);
        layoutParams10.rightMargin = Ui.dpi(this, 4.0f);
        this.homeLayer.addView(this.playerView, layoutParams10);
        this.dockRow = new CategoryColumn(this);
        FrameLayout.LayoutParams layoutParams11 = new FrameLayout.LayoutParams(-2, -2, 8388661);
        layoutParams11.topMargin = Ui.dpi(this, 148.0f);
        layoutParams11.rightMargin = Ui.dpi(this, 4.0f);
        this.homeLayer.addView(this.dockRow, layoutParams11);
        this.quickRow = new LinearLayout(this);
        this.quickRow.setOrientation(0);
        this.quickRow.setGravity(17);
        FrameLayout.LayoutParams layoutParams12 = new FrameLayout.LayoutParams(-2, -2, 81);
        layoutParams12.bottomMargin = Ui.dpi(this, 18.0f);
        this.homeLayer.addView(this.quickRow, layoutParams12);
        buildQuickRow();
        this.walletCard = new CardButton(this);
        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() { // from class: com.aaron.bluehour.MainActivity.14
            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnDoubleTapListener
            public boolean onDoubleTap(MotionEvent motionEvent) {
                MainActivity.this.runCardAction(MainActivity.this.cardAction("card_double", "flashlight"));
                return true;
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
            public boolean onDown(MotionEvent motionEvent) {
                return true;
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
            public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
                if (f2 >= -800.0f) {
                    return false;
                }
                MainActivity.this.runCardAction(MainActivity.this.cardAction("card_swipe", "lastapp"));
                return true;
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
            public void onLongPress(MotionEvent motionEvent) {
                MainActivity.this.walletCard.performHapticFeedback(0);
                MainActivity.this.runCardAction(MainActivity.this.cardAction("card_hold", "wallet"));
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnDoubleTapListener
            public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                MainActivity.this.cardWiggle();
                return true;
            }
        });
        this.walletCard.setOnTouchListener(new View.OnTouchListener() { // from class: com.aaron.bluehour.MainActivity.15
            @Override // android.view.View.OnTouchListener
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getActionMasked()) {
                    case 0:
                        view.setPressed(true);
                        break;
                    case 1:
                    case 3:
                        view.setPressed(false);
                        break;
                }
                gestureDetector.onTouchEvent(motionEvent);
                return true;
            }
        });
        FrameLayout.LayoutParams layoutParams13 = new FrameLayout.LayoutParams(-2, -2, 8388691);
        layoutParams13.leftMargin = Ui.dpi(this, 4.0f);
        layoutParams13.bottomMargin = Ui.dpi(this, 118.0f);
        this.homeLayer.addView(this.walletCard, layoutParams13);
        this.walletCard.setVisibility(this.prefs.flag("wallet_hidden") ? HOME_CAT_MAX : 0);
        loadWalletIcon();
        this.homeGd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() { // from class: com.aaron.bluehour.MainActivity.16
            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
            public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
                if (f2 < -1600.0f) {
                    MainActivity.this.openDrawer();
                    return true;
                }
                if (f2 <= 1600.0f) {
                    return false;
                }
                MainActivity.this.openRecents();
                return true;
            }
        });
        this.homeLayer.setOnTouchListener(new View.OnTouchListener() { // from class: com.aaron.bluehour.MainActivity.17
            @Override // android.view.View.OnTouchListener
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
        this.mailButton = new MailButton(this);
        FrameLayout.LayoutParams layoutParams14 = new FrameLayout.LayoutParams(-2, -2, 8388661);
        layoutParams14.topMargin = Ui.dpi(this, 2.0f);
        layoutParams14.rightMargin = Ui.dpi(this, 6.0f);
        this.homeLayer.addView(this.mailButton, layoutParams14);
        this.mailButton.setOnClickListener(new View.OnClickListener() { // from class: com.aaron.bluehour.MainActivity.18
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.openMail();
            }
        });
        this.mailButton.setCount(NotifService.feedCount());
        updateClock();
        applyFoldLayout();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void buildQuickRow() {
        GlyphView glyphView;
        final Runnable defaultQuickAction;
        AppEntry appEntry;
        this.quickRow.removeAllViews();
        this.quickAwaitingIcons = false;
        int[] iArr = {0, 1, 2, 3, 4};
        String[] strArr = {"PHONE", "MESSAGES", "YOUTUBE", "CHROME", "CAMERA"};
        String[] strArr2 = {"電話", "メッセージ", "ユーチューブ", "クローム", "カメラ"};
        for (int i0 = 0; i0 < 5; i0++) {
            final int i = i0;
            final String quickSlot = this.prefs.quickSlot(i);
            String defaultQuickPkg = quickSlot.length() > 0 ? quickSlot : defaultQuickPkg(i);
            if (quickSlot.length() > 0) {
                Iterator<AppEntry> it = this.apps.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        appEntry = null;
                        break;
                    }
                    AppEntry next = it.next();
                    if (next.pkg.equals(quickSlot)) {
                        appEntry = next;
                        break;
                    }
                }
                if (appEntry != null && appEntry.icon == null) {
                    this.quickAwaitingIcons = true;
                }
                GlyphView glyphView2 = new GlyphView(this, appEntry == null ? null : appEntry.icon, appEntry == null ? "?" : appEntry.labelUp);
                defaultQuickAction = new Runnable() { // from class: com.aaron.bluehour.MainActivity.48
                    @Override // java.lang.Runnable
                    public void run() {
                        MainActivity.this.startQuick(null, quickSlot);
                    }
                };
                glyphView = glyphView2;
            } else if (i == 2) {
                glyphView = new GlyphView(this, 5, "CHANNEL", "チャンネル");
                defaultQuickAction = new Runnable() { // from class: com.aaron.bluehour.MainActivity.49
                    @Override // java.lang.Runnable
                    public void run() {
                        MainActivity.this.openMediaHub();
                    }
                };
                defaultQuickPkg = null;
            } else {
                glyphView = new GlyphView(this, iArr[i], strArr[i], strArr2[i]);
                defaultQuickAction = defaultQuickAction(i);
            }
            glyphView.setTag(defaultQuickPkg);
            glyphView.setDot(defaultQuickPkg != null && NotifService.has(defaultQuickPkg));
            glyphView.setOnClickListener(new View.OnClickListener() { // from class: com.aaron.bluehour.MainActivity.50
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    defaultQuickAction.run();
                }
            });
            glyphView.setLongClickable(true);
            glyphView.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.aaron.bluehour.MainActivity.51
                @Override // android.view.View.OnLongClickListener
                public boolean onLongClick(View view) {
                    MainActivity.this.showSlotDialog(i);
                    return true;
                }
            });
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Ui.dpi(this, 66.0f), Ui.dpi(this, 94.0f));
            layoutParams.leftMargin = Ui.dpi(this, 3.0f);
            layoutParams.rightMargin = Ui.dpi(this, 3.0f);
            this.quickRow.addView(glyphView, layoutParams);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String cardAction(String str, String str2) {
        String strVal = this.prefs.strVal(str);
        return strVal.length() == 0 ? str2 : strVal;
    }

    private String cardActionLabel(String str) {
        if (!str.startsWith("app:")) {
            return str.equals("wallet") ? "WALLET" : str.equals("flashlight") ? "FLASHLIGHT" : str.equals("lastapp") ? "SHUFFLE TIME" : str.equals("search") ? "SEARCH" : str.equals("recents") ? "RECENTS" : "NONE";
        }
        for (AppEntry appEntry : this.apps) {
            if (appEntry.pkg.equals(str.substring(4))) {
                return appEntry.labelUp;
            }
        }
        return "APP";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cardWiggle() {
        this.walletCard.animate().rotationBy(5.0f).setDuration(70L).withEndAction(new Runnable() { // from class: com.aaron.bluehour.MainActivity.95
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.walletCard.animate().rotationBy(-5.0f).setDuration(140L).setInterpolator(new OvershootInterpolator(2.0f)).start();
            }
        }).start();
    }

    private boolean catHasDot(String str) {
        Set<String> hidden = this.prefs.hidden();
        Set<String> catApps = str.length() == 0 ? null : this.prefs.catApps(str);
        for (AppEntry appEntry : this.apps) {
            if (!hidden.contains(appEntry.pkg) && (catApps == null || catApps.contains(appEntry.pkg))) {
                if (NotifService.has(appEntry.pkg)) {
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void closeDrawer() {
        if (this.drawerOpen) {
            this.drawerOpen = false;
            this.addModeCat = null;
            this.pickSlot = -1;
            this.pickWallet = false;
            this.pickCardKey = null;
            this.adapter.setAddMembers(null);
            this.adapter.setOrderedPkgs(null);
            hideIme();
            this.searchEdit.setText("");
            this.searchEdit.clearFocus();
            this.root.performHapticFeedback(4);
            animateDrawer(0.0f);
        }
    }

    private Bitmap decodeScaled(String str, int i) {
        int i2 = 1;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(str, options);
        while (Math.max(options.outWidth, options.outHeight) / i2 > i) {
            i2 *= 2;
        }
        BitmapFactory.Options options2 = new BitmapFactory.Options();
        options2.inSampleSize = i2;
        return BitmapFactory.decodeFile(str, options2);
    }

    private Runnable defaultQuickAction(int i) {
        switch (i) {
            case 0:
                return new Runnable() { // from class: com.aaron.bluehour.MainActivity.52
                    @Override // java.lang.Runnable
                    public void run() {
                        MainActivity.this.startQuick(new Intent("android.intent.action.DIAL"), null);
                    }
                };
            case 1:
                return new Runnable() { // from class: com.aaron.bluehour.MainActivity.53
                    @Override // java.lang.Runnable
                    public void run() {
                        MainActivity.this.startQuick(Intent.makeMainSelectorActivity("android.intent.action.MAIN", "android.intent.category.APP_MESSAGING"), "com.google.android.apps.messaging");
                    }
                };
            case 2:
                return new Runnable() { // from class: com.aaron.bluehour.MainActivity.54
                    @Override // java.lang.Runnable
                    public void run() {
                        MainActivity.this.startQuick(null, "com.google.android.youtube");
                    }
                };
            case 3:
                return new Runnable() { // from class: com.aaron.bluehour.MainActivity.55
                    @Override // java.lang.Runnable
                    public void run() {
                        MainActivity.this.startQuick(Intent.makeMainSelectorActivity("android.intent.action.MAIN", "android.intent.category.APP_BROWSER"), "com.android.chrome");
                    }
                };
            default:
                return new Runnable() { // from class: com.aaron.bluehour.MainActivity.56
                    @Override // java.lang.Runnable
                    public void run() {
                        MainActivity.this.startQuick(new Intent("android.media.action.STILL_IMAGE_CAMERA"), null);
                    }
                };
        }
    }

    private String defaultQuickPkg(int i) {
        switch (i) {
            case 0:
                return resolvePkg(new Intent("android.intent.action.DIAL"));
            case 1:
                return installed("com.google.android.apps.messaging") ? "com.google.android.apps.messaging" : resolvePkg(Intent.makeMainSelectorActivity("android.intent.action.MAIN", "android.intent.category.APP_MESSAGING"));
            case 2:
                if (installed("com.google.android.youtube")) {
                    return "com.google.android.youtube";
                }
                return null;
            case 3:
                return installed("com.android.chrome") ? "com.android.chrome" : resolvePkg(Intent.makeMainSelectorActivity("android.intent.action.MAIN", "android.intent.category.APP_BROWSER"));
            default:
                return resolvePkg(new Intent("android.media.action.STILL_IMAGE_CAMERA"));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public TextView dialogRow(String str, int i) {
        TextView textView = new TextView(this);
        textView.setTypeface(Ui.tf(this));
        textView.setTextSize(2, 16.0f);
        textView.setTextColor(new ColorStateList(new int[][]{new int[]{android.R.attr.state_pressed}, new int[0]}, new int[]{-1, i}));
        textView.setText(str);
        textView.setClickable(true);
        textView.setBackground(Ui.skewSelector(this, -167772161, -1703918, Ui.dp(this, 10.0f)));
        textView.setPadding(Ui.dpi(this, 20.0f), Ui.dpi(this, 13.0f), Ui.dpi(this, 20.0f), Ui.dpi(this, 13.0f));
        return textView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doWeatherFetch(Location location) {
        String str;
        try {
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL("https://api.open-meteo.com/v1/forecast?latitude=" + location.getLatitude() + "&longitude=" + location.getLongitude() + "&current=temperature_2m,weather_code,is_day" + ("US".equals(Locale.getDefault().getCountry()) ? "&temperature_unit=fahrenheit" : "")).openConnection();
            httpURLConnection.setConnectTimeout(6000);
            httpURLConnection.setReadTimeout(6000);
            InputStream inputStream = httpURLConnection.getInputStream();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] bArr = new byte[4096];
            while (true) {
                int read = inputStream.read(bArr);
                if (read <= 0) {
                    break;
                } else {
                    byteArrayOutputStream.write(bArr, 0, read);
                }
            }
            inputStream.close();
            httpURLConnection.disconnect();
            JSONObject jSONObject = new JSONObject(byteArrayOutputStream.toString("UTF-8")).getJSONObject("current");
            int round = (int) Math.round(jSONObject.getDouble("temperature_2m"));
            int i = jSONObject.getInt("weather_code");
            boolean z = jSONObject.optInt("is_day", 1) == 1;
            if (i == 0) {
                str = z ? "CLEAR SKY" : "CLEAR NIGHT";
            } else {
                str = i <= 2 ? "PARTLY CLOUDY" : i == 3 ? "OVERCAST" : i <= 48 ? "FOGGY" : i <= 57 ? "DRIZZLE" : i <= 67 ? "RAIN" : i <= 77 ? "SNOW" : i <= 82 ? "SHOWERS" : i <= 86 ? "SNOW SHOWERS" : "THUNDERSTORM";
            }
            final String str2 = (z ? "☀  " : "☾  ") + str + " · " + round + "°";
            this.prefs.setStrVal("wx_text", str2);
            this.prefs.setLongVal("wx_time", System.currentTimeMillis());
            runOnUiThread(new Runnable() { // from class: com.aaron.bluehour.MainActivity.103
                @Override // java.lang.Runnable
                public void run() {
                    MainActivity.this.showWeather(str2);
                }
            });
        } catch (Exception e) {
        }
    }

    private List<String> dottedIn(Set<String> set) {
        ArrayList arrayList = new ArrayList();
        Set<String> hidden = this.prefs.hidden();
        for (AppEntry appEntry : this.apps) {
            if (!hidden.contains(appEntry.pkg) && (set == null || set.contains(appEntry.pkg))) {
                if (NotifService.has(appEntry.pkg)) {
                    arrayList.add(appEntry.pkg);
                }
            }
        }
        return arrayList;
    }

    private void ensureNotifBound() {
        try {
            if (Build.VERSION.SDK_INT < 24 || !NotifService.isEnabled(this)) {
                return;
            }
            NotificationListenerService.requestRebind(new ComponentName(this, (Class<?>) NotifService.class));
        } catch (Exception e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enterAddMode(String str) {
        this.addModeCat = str;
        this.pickSlot = -1;
        this.pickWallet = false;
        this.pickCardKey = null;
        this.currentCat = str;
        this.adapter.setOrderedPkgs(null);
        this.adapter.setCatPkgs(null);
        this.adapter.setAddMembers(this.prefs.catApps(str));
        this.drawerHeader.setText("+ " + str);
        this.listView.resetFocus();
        if (!this.drawerOpen) {
            this.drawerOpen = true;
            this.listView.setSelection(0);
            animateDrawer(1.0f);
        }
        toast("TAP APPS TO ADD OR REMOVE — BACK WHEN DONE");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enterPickCardAction() {
        this.pickWallet = false;
        this.addModeCat = null;
        this.pickSlot = -1;
        this.currentCat = null;
        this.adapter.setAddMembers(null);
        this.adapter.setOrderedPkgs(null);
        this.adapter.setCatPkgs(null);
        this.drawerHeader.setText("PICK APP");
        this.listView.resetFocus();
        if (!this.drawerOpen) {
            this.drawerOpen = true;
            this.listView.setSelection(0);
            animateDrawer(1.0f);
        }
        toast("TAP AN APP FOR THE GESTURE");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enterPickMode(int i) {
        this.pickSlot = i;
        this.addModeCat = null;
        this.currentCat = null;
        this.pickWallet = false;
        this.pickCardKey = null;
        this.adapter.setAddMembers(null);
        this.adapter.setOrderedPkgs(null);
        this.adapter.setCatPkgs(null);
        this.drawerHeader.setText("PICK APP");
        this.listView.resetFocus();
        if (!this.drawerOpen) {
            this.drawerOpen = true;
            this.listView.setSelection(0);
            animateDrawer(1.0f);
        }
        toast("TAP AN APP FOR SLOT " + (i + 1));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enterPickWallet() {
        this.pickWallet = true;
        this.addModeCat = null;
        this.pickSlot = -1;
        this.currentCat = null;
        this.adapter.setAddMembers(null);
        this.adapter.setOrderedPkgs(null);
        this.adapter.setCatPkgs(null);
        this.drawerHeader.setText("PICK WALLET APP");
        this.listView.resetFocus();
        if (!this.drawerOpen) {
            this.drawerOpen = true;
            this.listView.setSelection(0);
            animateDrawer(1.0f);
        }
        toast("TAP YOUR PAYMENT APP");
    }

    private void exitAddMode() {
        String str = this.addModeCat;
        this.addModeCat = null;
        this.adapter.setAddMembers(null);
        this.adapter.setCatPkgs(this.prefs.catApps(str));
        this.drawerHeader.setText(str);
        this.listView.resetFocus();
        this.listView.setSelection(0);
        rebuildDock();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void fetchWeather() {
        Location location;
        int i = 0;
        LocationManager locationManager = (LocationManager) getSystemService("location");
        Location location2 = null;
        String[] strArr = {"network", "passive", "gps"};
        int length = strArr.length;
        while (true) {
            if (i >= length) {
                location = location2;
                break;
            }
            try {
                location2 = locationManager.getLastKnownLocation(strArr[i]);
            } catch (Exception e) {
            }
            if (location2 != null) {
                location = location2;
                break;
            }
            i++;
        }
        if (location == null) {
            runOnUiThread(new Runnable() { // from class: com.aaron.bluehour.MainActivity.100
                @Override // java.lang.Runnable
                public void run() {
                    MainActivity.this.requestFreshLocation();
                }
            });
        } else {
            doWeatherFetch(location);
        }
    }

    private AppEntry findApp(String str) {
        for (AppEntry appEntry : this.apps) {
            if (appEntry.id().equals(str)) {
                return appEntry;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void fireNotif(NotifService.Item item) {
        boolean z;
        boolean z2 = true;
        Bundle bundle = null;
        try {
            if (item.intent != null) {
                if (Build.VERSION.SDK_INT >= 34) {
                    ActivityOptions makeBasic = ActivityOptions.makeBasic();
                    makeBasic.setPendingIntentBackgroundActivityStartMode(1);
                    bundle = makeBasic.toBundle();
                }
                item.intent.send(this, 0, null, null, null, null, bundle);
                z = true;
            } else {
                z = false;
            }
        } catch (Exception e) {
            z = false;
        }
        if (!z) {
            try {
                Intent launchIntentForPackage = getPackageManager().getLaunchIntentForPackage(item.pkg);
                if (launchIntentForPackage != null) {
                    launchIntentForPackage.addFlags(268435456);
                    startActivity(launchIntentForPackage);
                } else {
                    z2 = z;
                }
                z = z2;
            } catch (Exception e2) {
            }
        }
        if (!z) {
            toast("CAN'T OPEN");
            return;
        }
        flashAccent();
        if (this.mailView != null) {
            this.mailView.close();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void flashAccent() {
        this.flash.animate().cancel();
        this.flash.setAlpha(0.3f);
        this.flash.animate().alpha(0.0f).setDuration(330L).start();
    }

    private void hideIme() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService("input_method");
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(this.searchEdit.getWindowToken(), 0);
        }
    }

    private int homeHPad() {
        return Ui.dpi(this, isUnfolded() ? 56.0f : 22.0f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean installed(String str) {
        try {
            return getPackageManager().getLaunchIntentForPackage(str) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isDarkHour(int i) {
        return this.prefs.flag("dh_force") || i == 0;
    }

    private boolean isUnfolded() {
        return getResources().getConfiguration().smallestScreenWidthDp >= 480;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void launch(AppEntry appEntry, View view) {
        try {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.LAUNCHER");
            intent.setClassName(appEntry.pkg, appEntry.cls);
            intent.setFlags(270532608);
            startActivity(intent, view != null ? ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight()).toBundle() : null);
            flashAccent();
            this.prefs.bumpUses(appEntry.pkg);
            this.prefs.pushRecent(appEntry.pkg);
            this.usesMap.put(appEntry.pkg, Integer.valueOf(this.prefs.uses(appEntry.pkg)));
        } catch (Exception e) {
            toast("CAN'T LAUNCH " + appEntry.labelUp);
        }
    }

    private void launchWallet() {
        String strVal = this.prefs.strVal("wallet_pkg");
        if (strVal.length() == 0) {
            strVal = "com.samsung.android.spay";
        }
        try {
            Intent launchIntentForPackage = getPackageManager().getLaunchIntentForPackage(strVal);
            if (launchIntentForPackage == null) {
                toast("NO WALLET APP SET — SETTINGS ▸ THE CARD ▸ PICK PAYMENT APP");
                return;
            }
            launchIntentForPackage.addFlags(268435456);
            startActivity(launchIntentForPackage);
            flashAccent();
        } catch (Exception e) {
            toast("CAN'T OPEN WALLET");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadApps() {
        AppRepository.load(this, new AppRepository.Callback() { // from class: com.aaron.bluehour.MainActivity.30
            @Override // com.aaron.bluehour.AppRepository.Callback
            public void onIconsUpdated() {
                MainActivity.this.adapter.notifyDataSetChanged();
                if (MainActivity.this.quickAwaitingIcons) {
                    MainActivity.this.buildQuickRow();
                }
            }

            @Override // com.aaron.bluehour.AppRepository.Callback
            public void onLoaded(List<AppEntry> list) {
                MainActivity.this.apps = list;
                if (!MainActivity.this.prefs.flag("comm_renamed")) {
                    MainActivity.this.prefs.setFlag("comm_renamed", true);
                    if (MainActivity.this.prefs.catNames().contains("COMMUNICATION") && !MainActivity.this.prefs.catNames().contains("SOCIAL")) {
                        MainActivity.this.prefs.renameCat("COMMUNICATION", "SOCIAL");
                        MainActivity.this.prefs.setCatJp("SOCIAL", "ソーシャル");
                    }
                }
                if (!MainActivity.this.prefs.flag("system_pinned")) {
                    MainActivity.this.prefs.setFlag("system_pinned", true);
                    if (MainActivity.this.prefs.catNames().contains("SYSTEM")) {
                        MainActivity.this.prefs.deleteCat("SYSTEM");
                    }
                }
                if (!MainActivity.this.prefs.flag("media_seeded")) {
                    MainActivity.this.prefs.setFlag("media_seeded", true);
                    if (MainActivity.this.prefs.mediaApps().isEmpty() && MainActivity.this.installed("com.google.android.youtube")) {
                        MainActivity.this.prefs.addMediaApp("com.google.android.youtube");
                    }
                }
                MainActivity.this.usesMap.clear();
                for (AppEntry appEntry : list) {
                    int uses = MainActivity.this.prefs.uses(appEntry.pkg);
                    if (uses > 0) {
                        MainActivity.this.usesMap.put(appEntry.pkg, Integer.valueOf(uses));
                    }
                }
                MainActivity.this.adapter.setUses(MainActivity.this.usesMap);
                MainActivity.this.adapter.setHidden(MainActivity.this.prefs.hidden());
                MainActivity.this.adapter.setData(list);
                MainActivity.this.rebuildDock();
                MainActivity.this.buildQuickRow();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadWalletIcon() {
        try {
            File file = new File(getFilesDir(), "wallet_card.png");
            if (file.exists()) {
                this.walletCard.setCustomIcon(decodeScaled(file.getAbsolutePath(), 512));
            } else {
                this.walletCard.setCustomIcon(null);
            }
        } catch (Throwable th) {
            try {
                this.walletCard.setCustomIcon(null);
            } catch (Throwable th2) {
            }
        }
    }

    private Dialog makeDialog(String str) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(1);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setId(android.R.id.list);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(Ui.dpi(this, 10.0f), Ui.dpi(this, 10.0f), Ui.dpi(this, 10.0f), Ui.dpi(this, 10.0f));
        TextView textView = new TextView(this);
        textView.setTypeface(Ui.tf(this));
        textView.setTextSize(2, 19.0f);
        textView.setTextColor(Theme.get().accentText());
        textView.setText(str);
        textView.setBackground(new SkewDrawable(Theme.get().accent, Ui.dp(this, 10.0f)));
        textView.setPadding(Ui.dpi(this, 20.0f), Ui.dpi(this, 10.0f), Ui.dpi(this, 20.0f), Ui.dpi(this, 10.0f));
        linearLayout.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        ScrollView scrollView = new ScrollView(this);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.addView(linearLayout, new ViewGroup.LayoutParams(-1, -2));
        dialog.setContentView(scrollView);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
            window.setLayout(Ui.dpi(this, 310.0f), -2);
            window.setDimAmount(0.75f);
        }
        return dialog;
    }

    private void maybeFetchWeather() {
        if (checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") != 0) {
            if (this.prefs.flag("asked_loc")) {
                return;
            }
            this.prefs.setFlag("asked_loc", true);
            requestPermissions(new String[]{"android.permission.ACCESS_COARSE_LOCATION"}, 9);
            return;
        }
        showWeather(this.prefs.strVal("wx_text"));
        if (System.currentTimeMillis() - this.prefs.longVal("wx_time") >= 1800000) {
            new Thread(new Runnable() { // from class: com.aaron.bluehour.MainActivity.99
                @Override // java.lang.Runnable
                public void run() {
                    MainActivity.this.fetchWeather();
                }
            }, "bluehour-wx").start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void newCategory() {
        if (this.prefs.catNames().size() >= HOME_CAT_MAX) {
            toast("MAX 8 CATEGORIES — DELETE ONE FIRST");
        } else {
            promptDialog("NEW CATEGORY", "", "", new NameCallback() { // from class: com.aaron.bluehour.MainActivity.34
                @Override // com.aaron.bluehour.MainActivity.NameCallback
                public void onName(String str, String str2) {
                    MainActivity.this.prefs.addCat(str, str2);
                    MainActivity.this.rebuildDock();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openDrawer() {
        openDrawerFor(null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openDrawerFor(String str) {
        if (this.drawerOpen) {
            return;
        }
        this.drawerOpen = true;
        this.currentCat = str;
        this.addModeCat = null;
        this.pickSlot = -1;
        this.pickWallet = false;
        this.pickCardKey = null;
        this.adapter.setAddMembers(null);
        this.adapter.setOrderedPkgs(null);
        this.adapter.setCatPkgs(str != null ? this.prefs.catApps(str) : null);
        TextView textView = this.drawerHeader;
        if (str == null) {
            str = "APPS";
        }
        textView.setText(str);
        this.listView.resetFocus();
        this.listView.setSelection(0);
        this.root.performHapticFeedback(4);
        animateDrawer(1.0f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openFeed() {
        String strVal = this.prefs.strVal("feed_pkg");
        startQuick(new Intent("android.intent.action.VIEW", Uri.parse("https://news.google.com")), (strVal == null || !strVal.startsWith("app:")) ? "com.google.android.googlequicksearchbox" : strVal.substring(4));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openMail() {
        if (this.mailView == null) {
            this.mailView = new MailView(this);
            this.mailView.setHost(new MailView.Host() { // from class: com.aaron.bluehour.MainActivity.19
                @Override // com.aaron.bluehour.MailView.Host
                public Bitmap iconFor(String str) {
                    AppEntry appForPkg = MainActivity.this.appForPkg(str);
                    if (appForPkg == null) {
                        return null;
                    }
                    return appForPkg.icon;
                }

                @Override // com.aaron.bluehour.MailView.Host
                public String labelFor(String str) {
                    AppEntry appForPkg = MainActivity.this.appForPkg(str);
                    return appForPkg == null ? str : appForPkg.labelUp;
                }

                @Override // com.aaron.bluehour.MailView.Host
                public void openItem(NotifService.Item item) {
                    MainActivity.this.fireNotif(item);
                }
            });
            this.root.addView(this.mailView, new FrameLayout.LayoutParams(-1, -1));
        }
        this.mailView.open();
        if (this.crtView != null) {
            this.crtView.bringToFront();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openMediaHub() {
        if (this.mediaHub == null) {
            this.mediaHub = new MediaHubView(this);
            this.mediaHub.setHost(new MediaHubView.Host() { // from class: com.aaron.bluehour.MainActivity.20
                @Override // com.aaron.bluehour.MediaHubView.Host
                public void addFlow() {
                    MainActivity.this.showAddMediaDialog();
                }

                @Override // com.aaron.bluehour.MediaHubView.Host
                public Bitmap iconFor(String str) {
                    AppEntry appForPkg = MainActivity.this.appForPkg(str);
                    if (appForPkg == null) {
                        return null;
                    }
                    return appForPkg.icon;
                }

                @Override // com.aaron.bluehour.MediaHubView.Host
                public String labelFor(String str) {
                    AppEntry appForPkg = MainActivity.this.appForPkg(str);
                    return appForPkg == null ? str : appForPkg.labelUp;
                }

                @Override // com.aaron.bluehour.MediaHubView.Host
                public void launch(String str) {
                    MainActivity.this.startQuick(null, str);
                }

                @Override // com.aaron.bluehour.MediaHubView.Host
                public List<String> mediaPkgs() {
                    return MainActivity.this.prefs.mediaApps();
                }

                @Override // com.aaron.bluehour.MediaHubView.Host
                public void remove(String str) {
                    MainActivity.this.prefs.removeMediaApp(str);
                    AppEntry appForPkg = MainActivity.this.appForPkg(str);
                    MainActivity.this.toast("REMOVED " + (appForPkg == null ? "" : appForPkg.labelUp));
                }
            });
            this.root.addView(this.mediaHub, new FrameLayout.LayoutParams(-1, -1));
        }
        this.mediaHub.open();
        if (this.crtView != null) {
            this.crtView.bringToFront();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openRecents() {
        if (this.drawerOpen) {
            return;
        }
        List<String> recents = this.prefs.recents();
        if (recents.isEmpty()) {
            toast("NOTHING RECENT YET");
            return;
        }
        this.drawerOpen = true;
        this.currentCat = null;
        this.addModeCat = null;
        this.pickSlot = -1;
        this.pickWallet = false;
        this.pickCardKey = null;
        this.adapter.setAddMembers(null);
        this.adapter.setCatPkgs(null);
        this.adapter.setOrderedPkgs(recents);
        this.drawerHeader.setText("RECENT");
        this.listView.resetFocus();
        this.listView.setSelection(0);
        animateDrawer(1.0f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pickController(List<MediaController> list) {
        MediaController mediaController = null;
        if (list != null) {
            Iterator<MediaController> it = list.iterator();
            MediaController mediaController2 = null;
            while (true) {
                if (!it.hasNext()) {
                    mediaController = mediaController2;
                    break;
                }
                mediaController = it.next();
                if (mediaController.getMetadata() != null) {
                    PlaybackState playbackState = mediaController.getPlaybackState();
                    boolean z = playbackState != null && playbackState.getState() == 3;
                    if (mediaController2 != null && !z) {
                        mediaController = mediaController2;
                    }
                    if (z) {
                        break;
                    } else {
                        mediaController2 = mediaController;
                    }
                }
            }
        }
        if (this.mediaController != null) {
            try {
                this.mediaController.unregisterCallback(this.mediaCb);
            } catch (Exception e) {
            }
        }
        this.mediaController = mediaController;
        if (mediaController != null) {
            mediaController.registerCallback(this.mediaCb);
        }
        updatePlayerWidget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pickWallpaper(int i) {
        try {
            Intent intent = new Intent("android.intent.action.GET_CONTENT");
            intent.setType("image/*");
            intent.addCategory("android.intent.category.OPENABLE");
            startActivityForResult(intent, i);
        } catch (Exception e) {
            toast("NO IMAGE PICKER FOUND");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void promptDialog(String str, String str2, String str3, final NameCallback nameCallback) {
        final Dialog makeDialog = makeDialog(str);
        final EditText promptField = promptField("NAME", str2, 12);
        final EditText promptField2 = promptField("カタカナ — AUTO IF EMPTY", str3, 16);
        LinearLayout linearLayout = (LinearLayout) makeDialog.getWindow().getDecorView().findViewById(android.R.id.list);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = Ui.dpi(this, 7.0f);
        linearLayout.addView(promptField, layoutParams);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = Ui.dpi(this, 7.0f);
        linearLayout.addView(promptField2, layoutParams2);
        TextView dialogRow = dialogRow("OK", -16381940);
        dialogRow.setOnClickListener(new View.OnClickListener() { // from class: com.aaron.bluehour.MainActivity.39
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                String replace = promptField.getText().toString().trim().toUpperCase(Locale.getDefault()).replace("|", "");
                if (replace.length() == 0 || replace.equals("ALL APPS") || replace.equals("SYSTEM") || MainActivity.this.prefs.catNames().contains(replace)) {
                    MainActivity.this.toast("PICK A DIFFERENT NAME");
                    return;
                }
                String trim = promptField2.getText().toString().trim();
                if (trim.length() == 0) {
                    trim = Kana.toKatakana(replace);
                }
                makeDialog.dismiss();
                nameCallback.onName(replace, trim);
            }
        });
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams3.topMargin = Ui.dpi(this, 7.0f);
        linearLayout.addView(dialogRow, layoutParams3);
        makeDialog.show();
    }

    private EditText promptField(String str, String str2, int i) {
        EditText editText = new EditText(this);
        editText.setTypeface(Ui.tf(this));
        editText.setTextSize(2, 17.0f);
        editText.setTextColor(-16381940);
        editText.setHintTextColor(1493567500);
        editText.setHint(str);
        editText.setSingleLine(true);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(i)});
        editText.setText(str2);
        editText.setBackground(new SkewDrawable(-1, 0.0f, -16381940, Ui.dp(this, 1.5f)));
        editText.setPadding(Ui.dpi(this, 16.0f), Ui.dpi(this, 10.0f), Ui.dpi(this, 16.0f), Ui.dpi(this, 10.0f));
        return editText;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void rebuildDock() {
        int i;
        this.dockRow.removeAllViews();
        this.dockRow.setGravity(8388611);
        this.dockRow.setClipChildren(false);
        List<String> catNames = this.prefs.catNames();
        if (this.lastDh) {
            Objects.requireNonNull(Theme.get());
            i = -6553656;
        } else {
            i = Theme.get().catWord;
        }
        int min = Math.min(catNames.size(), HOME_CAT_MAX);
        for (int i2 = 0; i2 < min; i2++) {
            String str = catNames.get(i2);
            addCatWord(str, this.prefs.catJp(str), i, str);
        }
        if (catNames.size() > HOME_CAT_MAX) {
            final ArrayList arrayList = new ArrayList(catNames.subList(HOME_CAT_MAX, catNames.size()));
            MenuWordView menuWordView = new MenuWordView(this, "MORE (" + arrayList.size() + ")", "", -1275922950);
            menuWordView.setOnClickListener(new View.OnClickListener() { // from class: com.aaron.bluehour.MainActivity.31
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    MainActivity.this.showMoreDialog(arrayList);
                }
            });
            this.dockRow.addView(menuWordView, wordLp());
        } else if (catNames.size() < HOME_CAT_MAX) {
            MenuWordView menuWordView2 = new MenuWordView(this, "+ NEW", "", -2131560966);
            menuWordView2.setOnClickListener(new View.OnClickListener() { // from class: com.aaron.bluehour.MainActivity.32
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    MainActivity.this.newCategory();
                }
            });
            this.dockRow.addView(menuWordView2, wordLp());
        }
        MenuWordView menuWordView3 = new MenuWordView(this, "SYSTEM", "システム", i);
        menuWordView3.setOnClickListener(new View.OnClickListener() { // from class: com.aaron.bluehour.MainActivity.33
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                view.postDelayed(new Runnable() { // from class: com.aaron.bluehour.MainActivity.33.1
                    @Override // java.lang.Runnable
                    public void run() {
                        try {
                            Intent intent = new Intent("android.settings.SETTINGS");
                            intent.addFlags(268435456);
                            MainActivity.this.startActivity(intent);
                            MainActivity.this.flashAccent();
                        } catch (Exception e) {
                        }
                    }
                }, 90L);
            }
        });
        this.dockRow.addView(menuWordView3, wordLp());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshMediaWatch() {
        if (this.mediaController != null) {
            return;
        }
        stopMediaWatch();
        startMediaWatch();
    }

    private void regReceiver(BroadcastReceiver broadcastReceiver, IntentFilter intentFilter) {
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(broadcastReceiver, intentFilter, 4);
        } else {
            registerReceiver(broadcastReceiver, intentFilter);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void regenerateSubtitles() {
        for (String str : this.prefs.catNames()) {
            this.prefs.setCatJp(str, Kana.toKatakana(str));
        }
        rebuildDock();
        toast("SUBTITLES REFRESHED");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestFreshLocation() {
        if (checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") != 0) {
            return;
        }
        final LocationManager locationManager = (LocationManager) getSystemService("location");
        LocationListener locationListener = new LocationListener() { // from class: com.aaron.bluehour.MainActivity.101
            @Override // android.location.LocationListener
            public void onLocationChanged(final Location location) {
                try {
                    locationManager.removeUpdates(this);
                } catch (Exception e) {
                }
                new Thread(new Runnable() { // from class: com.aaron.bluehour.MainActivity.101.1
                    @Override // java.lang.Runnable
                    public void run() {
                        MainActivity.this.doWeatherFetch(location);
                    }
                }, "bluehour-wx2").start();
            }

            @Override // android.location.LocationListener
            public void onProviderDisabled(String str) {
            }

            @Override // android.location.LocationListener
            public void onProviderEnabled(String str) {
            }

            @Override // android.location.LocationListener
            public void onStatusChanged(String str, int i, Bundle bundle) {
            }
        };
        final LocationListener[] locationListenerArr = {locationListener};
        String str = null;
        try {
            if (locationManager.isProviderEnabled("network")) {
                str = "network";
            } else if (locationManager.isProviderEnabled("gps")) {
                str = "gps";
            }
        } catch (Exception e) {
        }
        if (str != null) {
            try {
                locationManager.requestSingleUpdate(str, locationListener, Looper.getMainLooper());
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() { // from class: com.aaron.bluehour.MainActivity.102
                    @Override // java.lang.Runnable
                    public void run() {
                        try {
                            locationManager.removeUpdates(locationListenerArr[0]);
                        } catch (Exception e2) {
                        }
                    }
                }, 12000L);
            } catch (Exception e2) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestHomeRole(boolean z) {
        if (Build.VERSION.SDK_INT < 29) {
            try {
                startActivity(new Intent("android.settings.HOME_SETTINGS"));
                return;
            } catch (Exception e) {
                return;
            }
        }
        RoleManager roleManager = (RoleManager) getSystemService("role");
        if (roleManager == null || !roleManager.isRoleAvailable("android.app.role.HOME") || roleManager.isRoleHeld("android.app.role.HOME")) {
            return;
        }
        if (z || !this.prefs.flag("askedRole")) {
            this.prefs.setFlag("askedRole", true);
            try {
                startActivityForResult(roleManager.createRequestRoleIntent("android.app.role.HOME"), 7);
            } catch (Exception e2) {
                try {
                    startActivity(new Intent("android.settings.HOME_SETTINGS"));
                } catch (Exception e3) {
                }
            }
        }
    }

    private String resolvePkg(Intent intent) {
        try {
            ResolveInfo resolveActivity = getPackageManager().resolveActivity(intent, 65536);
            if (resolveActivity == null || resolveActivity.activityInfo == null) {
                return null;
            }
            return resolveActivity.activityInfo.packageName;
        } catch (Exception e) {
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void runCardAction(String str) {
        if (str.startsWith("app:")) {
            startQuick(null, str.substring(4));
            return;
        }
        if (str.equals("wallet")) {
            launchWallet();
            return;
        }
        if (str.equals("flashlight")) {
            toggleTorch();
            return;
        }
        if (str.equals("lastapp")) {
            shuffleTime();
            return;
        }
        if (!str.equals("search")) {
            if (str.equals("recents")) {
                openRecents();
            }
        } else {
            openDrawer();
            this.searchEdit.requestFocus();
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService("input_method");
            if (inputMethodManager != null) {
                inputMethodManager.showSoftInput(this.searchEdit, 0);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleAppReload() {
        this.appReloadHandler.removeCallbacks(this.appReloadRun);
        this.appReloadHandler.postDelayed(this.appReloadRun, 250L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDrawerPos(float f) {
        this.drawerPos = f;
        int height = this.root.getHeight();
        if (height == 0) {
            return;
        }
        this.drawerLayer.setTranslationY(height * (1.0f - f));
        this.homeLayer.setTranslationY((-f) * Ui.dp(this, 56.0f));
        this.homeLayer.setAlpha(1.0f - f);
        if (this.drawerHeader != null) {
            this.drawerHeader.setTranslationX((1.0f - f) * Ui.dp(this, 90.0f));
            this.drawerHeader.setAlpha(f);
        }
        if (this.searchRowV != null) {
            this.searchRowV.setTranslationX((1.0f - f) * Ui.dp(this, 55.0f));
            this.searchRowV.setAlpha(f);
        }
        if (this.listView != null) {
            this.listView.setTranslationX((1.0f - f) * Ui.dp(this, 52.0f));
        }
        this.bg.setDrawerProgress(f);
    }

    private void setupWindow() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
        } else {
            window.getDecorView().setSystemUiVisibility(1792);
        }
        window.setStatusBarColor(0);
        window.setNavigationBarColor(0);
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.layoutInDisplayCutoutMode = 1;
            window.setAttributes(attributes);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showAddMediaDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(1);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(Ui.dpi(this, 10.0f), Ui.dpi(this, 10.0f), Ui.dpi(this, 10.0f), Ui.dpi(this, 10.0f));
        TextView textView = new TextView(this);
        textView.setTypeface(Ui.tf(this));
        textView.setTextSize(2, 19.0f);
        textView.setTextColor(Theme.get().accentText());
        textView.setText("ADD CHANNEL");
        textView.setBackground(new SkewDrawable(Theme.get().accent, Ui.dp(this, 10.0f)));
        textView.setPadding(Ui.dpi(this, 20.0f), Ui.dpi(this, 10.0f), Ui.dpi(this, 20.0f), Ui.dpi(this, 10.0f));
        linearLayout.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        final EditText editText = new EditText(this);
        editText.setTypeface(Ui.tfUpright(this));
        editText.setTextSize(2, 16.0f);
        editText.setSingleLine(true);
        editText.setHint("SEARCH APPS…");
        editText.setTextColor(-15723750);
        editText.setHintTextColor(-2146430182);
        editText.setBackgroundColor(-218103809);
        editText.setPadding(Ui.dpi(this, 14.0f), Ui.dpi(this, 10.0f), Ui.dpi(this, 14.0f), Ui.dpi(this, 10.0f));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = Ui.dpi(this, 8.0f);
        linearLayout.addView(editText, layoutParams);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setVerticalScrollBarEnabled(false);
        final LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        scrollView.addView(linearLayout2, new ViewGroup.LayoutParams(-1, -2));
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, Ui.dpi(this, 360.0f));
        layoutParams2.topMargin = Ui.dpi(this, 8.0f);
        linearLayout.addView(scrollView, layoutParams2);
        final ArrayList<AppEntry> arrayList = new ArrayList<AppEntry>(this.apps);
        Collections.sort(arrayList, new Comparator<AppEntry>() { // from class: com.aaron.bluehour.MainActivity.21
            @Override // java.util.Comparator
            public int compare(AppEntry appEntry, AppEntry appEntry2) {
                return appEntry.labelUp.compareTo(appEntry2.labelUp);
            }
        });
        final Runnable[] runnableArr = new Runnable[1];
        runnableArr[0] = new Runnable() { // from class: com.aaron.bluehour.MainActivity.22
            @Override // java.lang.Runnable
            public void run() {
                linearLayout2.removeAllViews();
                String lowerCase = editText.getText().toString().trim().toLowerCase(Locale.getDefault());
                List<String> mediaApps = MainActivity.this.prefs.mediaApps();
                int i = 0;
                for (final AppEntry appEntry : arrayList) {
                    if (!mediaApps.contains(appEntry.pkg) && (lowerCase.length() <= 0 || appEntry.labelUp.toLowerCase(Locale.getDefault()).contains(lowerCase))) {
                        TextView dialogRow = MainActivity.this.dialogRow(appEntry.labelUp, -16381940);
                        dialogRow.setOnClickListener(new View.OnClickListener() { // from class: com.aaron.bluehour.MainActivity.22.1
                            @Override // android.view.View.OnClickListener
                            public void onClick(View view) {
                                MainActivity.this.prefs.addMediaApp(appEntry.pkg);
                                if (MainActivity.this.mediaHub != null) {
                                    MainActivity.this.mediaHub.refresh();
                                }
                                runnableArr[0].run();
                            }
                        });
                        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-1, -2);
                        layoutParams3.topMargin = Ui.dpi(MainActivity.this, 7.0f);
                        linearLayout2.addView(dialogRow, layoutParams3);
                        i++;
                    }
                }
                if (i == 0) {
                    TextView textView2 = new TextView(MainActivity.this);
                    textView2.setTypeface(Ui.tfUpright(MainActivity.this));
                    textView2.setTextSize(2, 14.0f);
                    textView2.setTextColor(-1711276033);
                    textView2.setText(lowerCase.length() > 0 ? "NO MATCHES" : "ALL APPS ADDED");
                    textView2.setPadding(Ui.dpi(MainActivity.this, 8.0f), Ui.dpi(MainActivity.this, 16.0f), 0, 0);
                    linearLayout2.addView(textView2);
                }
            }
        };
        runnableArr[0].run();
        editText.addTextChangedListener(new TextWatcher() { // from class: com.aaron.bluehour.MainActivity.23
            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable editable) {
            }

            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                runnableArr[0].run();
            }
        });
        dialog.setContentView(linearLayout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
            window.setLayout(Ui.dpi(this, 330.0f), -2);
            window.setDimAmount(0.75f);
        }
        dialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showAppCatsDialog(final AppEntry appEntry) {
        Dialog makeDialog = makeDialog(appEntry.labelUp);
        for (final String str : this.prefs.catNames()) {
            addRow(makeDialog, (this.prefs.catApps(str).contains(appEntry.pkg) ? "✓  " : "    ") + str, -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.67
                @Override // java.lang.Runnable
                public void run() {
                    MainActivity.this.prefs.toggleCatApp(str, appEntry.pkg);
                    if (MainActivity.this.currentCat != null) {
                        MainActivity.this.adapter.setCatPkgs(MainActivity.this.prefs.catApps(MainActivity.this.currentCat));
                    }
                    MainActivity.this.rebuildDock();
                    MainActivity.this.toast(MainActivity.this.prefs.catApps(str).contains(appEntry.pkg) ? "ADDED TO " + str : "REMOVED FROM " + str);
                }
            });
        }
        makeDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showAppDialog(final AppEntry appEntry) {
        Dialog makeDialog = makeDialog(appEntry.labelUp);
        if (this.currentCat != null && this.addModeCat == null) {
            final String str = this.currentCat;
            addRow(makeDialog, "REMOVE FROM " + str, -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.61
                @Override // java.lang.Runnable
                public void run() {
                    Set<String> catApps = MainActivity.this.prefs.catApps(str);
                    catApps.remove(appEntry.pkg);
                    MainActivity.this.prefs.setCatApps(str, catApps);
                    MainActivity.this.adapter.setCatPkgs(catApps);
                    MainActivity.this.rebuildDock();
                }
            });
        }
        addRow(makeDialog, "ADD TO CATEGORY…", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.62
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.showAppCatsDialog(appEntry);
            }
        });
        try {
            final LauncherApps launcherApps = (LauncherApps) getSystemService("launcherapps");
            if (launcherApps != null && launcherApps.hasShortcutHostPermission()) {
                LauncherApps.ShortcutQuery shortcutQuery = new LauncherApps.ShortcutQuery();
                shortcutQuery.setPackage(appEntry.pkg);
                shortcutQuery.setQueryFlags(9);
                List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(shortcutQuery, Process.myUserHandle());
                if (shortcuts != null) {
                    int i = 0;
                    for (final ShortcutInfo shortcutInfo : shortcuts) {
                        if (i >= 4) {
                            break;
                        }
                        CharSequence shortLabel = shortcutInfo.getShortLabel();
                        if (shortLabel != null && shortcutInfo.isEnabled()) {
                            i++;
                            addRow(makeDialog, "▸ " + shortLabel.toString().toUpperCase(Locale.getDefault()), -14790696, new Runnable() { // from class: com.aaron.bluehour.MainActivity.63
                                @Override // java.lang.Runnable
                                public void run() {
                                    try {
                                        launcherApps.startShortcut(shortcutInfo.getPackage(), shortcutInfo.getId(), null, null, Process.myUserHandle());
                                        MainActivity.this.flashAccent();
                                        MainActivity.this.prefs.pushRecent(appEntry.pkg);
                                    } catch (Exception e) {
                                        MainActivity.this.toast("SHORTCUT FAILED");
                                    }
                                }
                            });
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        addRow(makeDialog, "HIDE APP", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.64
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.prefs.hide(appEntry.pkg);
                MainActivity.this.adapter.setHidden(MainActivity.this.prefs.hidden());
                MainActivity.this.rebuildDock();
                MainActivity.this.toast("HIDDEN — LONG-PRESS THE CLOCK TO UNHIDE");
            }
        });
        addRow(makeDialog, "APP INFO", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.65
            @Override // java.lang.Runnable
            public void run() {
                try {
                    MainActivity.this.startActivity(new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.parse("package:" + appEntry.pkg)));
                } catch (Exception e2) {
                }
            }
        });
        addRow(makeDialog, "UNINSTALL", -1703918, new Runnable() { // from class: com.aaron.bluehour.MainActivity.66
            @Override // java.lang.Runnable
            public void run() {
                try {
                    MainActivity.this.startActivity(new Intent("android.intent.action.DELETE", Uri.parse("package:" + appEntry.pkg)));
                } catch (Exception e2) {
                }
            }
        });
        makeDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showAppsMenu() {
        Dialog makeDialog = makeDialog("APPS");
        addRow(makeDialog, "HIDDEN APPS", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.85
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.showHiddenDialog();
            }
        });
        addRow(makeDialog, "RELOAD APPS", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.86
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.loadApps();
                MainActivity.this.toast("RELOADING…");
            }
        });
        addRow(makeDialog, "REFRESH JP SUBTITLES", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.87
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.regenerateSubtitles();
            }
        });
        addRow(makeDialog, "FEED BUTTON: CHOOSE APP", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.88
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.pickCardKey = "feed_pkg";
                MainActivity.this.enterPickCardAction();
                MainActivity.this.toast("PICK YOUR FEED / NEWS APP");
            }
        });
        final boolean flag = this.prefs.flag("feed_shown");
        addRow(makeDialog, flag ? "HIDE FEED BUTTON" : "SHOW FEED BUTTON", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.89
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.prefs.setFlag("feed_shown", !flag);
                if (MainActivity.this.bulletinView != null) {
                    MainActivity.this.bulletinView.setVisibility(flag ? MainActivity.HOME_CAT_MAX : 0);
                }
                MainActivity.this.toast(flag ? "FEED BUTTON HIDDEN" : "FEED BUTTON SHOWN");
            }
        });
        makeDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showBackupMenu() {
        Dialog makeDialog = makeDialog("BACKUP");
        addRow(makeDialog, "BACK UP TO DOWNLOADS", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.90
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.backupToDownloads();
            }
        });
        addRow(makeDialog, "RESTORE FROM FILE", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.91
            @Override // java.lang.Runnable
            public void run() {
                try {
                    Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
                    intent.addCategory("android.intent.category.OPENABLE");
                    intent.setType("*/*");
                    MainActivity.this.startActivityForResult(intent, MainActivity.HOME_CAT_MAX);
                } catch (Exception e) {
                    MainActivity.this.toast("NO FILE PICKER AVAILABLE");
                }
            }
        });
        makeDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showCardActionPicker(final String str, String str2) {
        Dialog makeDialog = makeDialog(str2);
        for (final String[] strArr : new String[][]{new String[]{"wallet", "WALLET"}, new String[]{"flashlight", "FLASHLIGHT"}, new String[]{"lastapp", "SHUFFLE TIME (LAST APP)"}, new String[]{"search", "SEARCH"}, new String[]{"recents", "RECENTS"}, new String[]{"none", "NONE"}}) {
            addRow(makeDialog, strArr[1], -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.97
                @Override // java.lang.Runnable
                public void run() {
                    MainActivity.this.prefs.setStrVal(str, strArr[0]);
                    MainActivity.this.toast("SET");
                }
            });
        }
        addRow(makeDialog, "CHOOSE APP…", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.98
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.pickCardKey = str;
                MainActivity.this.enterPickCardAction();
            }
        });
        makeDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showCardActionsDialog() {
        Dialog makeDialog = makeDialog("CARD ACTIONS");
        for (final String[] strArr : new String[][]{new String[]{"card_double", "DOUBLE TAP", "flashlight"}, new String[]{"card_hold", "HOLD", "wallet"}, new String[]{"card_swipe", "SWIPE UP", "lastapp"}}) {
            addRow(makeDialog, strArr[1] + ":  " + cardActionLabel(cardAction(strArr[0], strArr[2])), -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.96
                @Override // java.lang.Runnable
                public void run() {
                    MainActivity.this.showCardActionPicker(strArr[0], strArr[1]);
                }
            });
        }
        makeDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showCardMenu() {
        Dialog makeDialog = makeDialog("THE CARD");
        addRow(makeDialog, "GESTURE ACTIONS", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.80
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.showCardActionsDialog();
            }
        });
        addRow(makeDialog, "PICK PAYMENT APP", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.81
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.enterPickWallet();
            }
        });
        addRow(makeDialog, "CHOOSE CARD ICON", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.82
            @Override // java.lang.Runnable
            public void run() {
                try {
                    Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
                    intent.addCategory("android.intent.category.OPENABLE");
                    intent.setType("image/*");
                    MainActivity.this.startActivityForResult(intent, 10);
                } catch (Exception e) {
                    MainActivity.this.toast("NO IMAGE PICKER AVAILABLE");
                }
            }
        });
        addRow(makeDialog, "RESET CARD ICON", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.83
            @Override // java.lang.Runnable
            public void run() {
                new File(MainActivity.this.getFilesDir(), "wallet_card.png").delete();
                MainActivity.this.loadWalletIcon();
            }
        });
        addRow(makeDialog, this.prefs.flag("wallet_hidden") ? "SHOW CARD" : "HIDE CARD", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.84
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.prefs.setFlag("wallet_hidden", !MainActivity.this.prefs.flag("wallet_hidden"));
                MainActivity.this.walletCard.setVisibility(MainActivity.this.prefs.flag("wallet_hidden") ? MainActivity.HOME_CAT_MAX : 0);
            }
        });
        makeDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showCatManageDialog(final String str) {
        Dialog makeDialog = makeDialog(str);
        final List<String> dottedIn = dottedIn(this.prefs.catApps(str));
        if (!dottedIn.isEmpty()) {
            addRow(makeDialog, "◆ NOTIFICATIONS (" + dottedIn.size() + ")", -1703918, new Runnable() { // from class: com.aaron.bluehour.MainActivity.42
                @Override // java.lang.Runnable
                public void run() {
                    MainActivity.this.showPeekDialog(str, dottedIn);
                }
            });
        }
        addRow(makeDialog, "ADD / REMOVE APPS", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.43
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.enterAddMode(str);
            }
        });
        addRow(makeDialog, "RENAME", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.44
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.promptDialog("RENAME " + str, str, MainActivity.this.prefs.catJp(str), new NameCallback() { // from class: com.aaron.bluehour.MainActivity.44.1
                    @Override // com.aaron.bluehour.MainActivity.NameCallback
                    public void onName(String str2, String str3) {
                        if (str3.equals(Kana.toKatakana(str))) {
                            str3 = Kana.toKatakana(str2);
                        }
                        MainActivity.this.prefs.renameCat(str, str2);
                        MainActivity.this.prefs.setCatJp(str2, str3);
                        MainActivity.this.rebuildDock();
                    }
                });
            }
        });
        addRow(makeDialog, "MOVE UP", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.45
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.prefs.moveCat(str, -1);
                MainActivity.this.rebuildDock();
            }
        });
        addRow(makeDialog, "MOVE DOWN", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.46
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.prefs.moveCat(str, 1);
                MainActivity.this.rebuildDock();
            }
        });
        addRow(makeDialog, "DELETE CATEGORY", -1703918, new Runnable() { // from class: com.aaron.bluehour.MainActivity.47
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.prefs.deleteCat(str);
                MainActivity.this.rebuildDock();
                MainActivity.this.toast("DELETED " + str + " (APPS ARE NOT UNINSTALLED)");
            }
        });
        makeDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showDisplayMenu() {
        Dialog makeDialog = makeDialog("THEME & DISPLAY");
        addRow(makeDialog, "THEME:  " + Theme.get().name, -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.77
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.showThemeDialog();
            }
        });
        addRow(makeDialog, this.prefs.flag("crt") ? "CRT SHADER: ON" : "CRT SHADER: OFF", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.78
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.prefs.setFlag("crt", !MainActivity.this.prefs.flag("crt"));
                MainActivity.this.applyCrt();
                MainActivity.this.toast(MainActivity.this.prefs.flag("crt") ? "CRT ON" : "CRT OFF");
            }
        });
        addRow(makeDialog, this.prefs.flag("dh_force") ? "DARK HOUR PREVIEW: ON" : "DARK HOUR PREVIEW: OFF", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.79
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.prefs.setFlag("dh_force", !MainActivity.this.prefs.flag("dh_force"));
                MainActivity.this.updateClock();
                MainActivity.this.toast(MainActivity.this.prefs.flag("dh_force") ? "THE DARK HOUR IS UPON US" : "BACK TO NORMAL TIME");
            }
        });
        makeDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showHiddenDialog() {
        String str;
        Dialog makeDialog = makeDialog("HIDDEN APPS");
        Set<String> hidden = this.prefs.hidden();
        if (hidden.isEmpty()) {
            addRow(makeDialog, "NOTHING HIDDEN", -2012870644, new Runnable() { // from class: com.aaron.bluehour.MainActivity.105
                @Override // java.lang.Runnable
                public void run() {
                }
            });
        } else {
            for (final String str2 : hidden) {
                Iterator<AppEntry> it = this.apps.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        str = str2;
                        break;
                    }
                    AppEntry next = it.next();
                    if (next.pkg.equals(str2)) {
                        str = next.labelUp;
                        break;
                    }
                }
                addRow(makeDialog, "UNHIDE: " + str, -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.106
                    @Override // java.lang.Runnable
                    public void run() {
                        MainActivity.this.prefs.unhide(str2);
                        MainActivity.this.adapter.setHidden(MainActivity.this.prefs.hidden());
                        MainActivity.this.rebuildDock();
                        MainActivity.this.toast("RESTORED");
                    }
                });
            }
        }
        makeDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showMoreDialog(List<String> list) {
        Dialog makeDialog = makeDialog("MORE CATEGORIES");
        for (final String str : list) {
            addRow(makeDialog, str, -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.35
                @Override // java.lang.Runnable
                public void run() {
                    MainActivity.this.openDrawerFor(str);
                }
            });
        }
        addRow(makeDialog, "+ NEW CATEGORY", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.36
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.newCategory();
            }
        });
        makeDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showPeekDialog(String str, List<String> list) {
        String str2;
        Dialog makeDialog = makeDialog(str);
        boolean z = false;
        for (final String str3 : list) {
            int count = NotifService.count(str3);
            if (count != 0) {
                Iterator<AppEntry> it = this.apps.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        str2 = str3;
                        break;
                    }
                    AppEntry next = it.next();
                    if (next.pkg.equals(str3)) {
                        str2 = next.labelUp;
                        break;
                    }
                }
                StringBuilder sb = new StringBuilder(str2);
                if (count > 1) {
                    sb.append("  (").append(count).append(")");
                }
                List<String> titles = NotifService.titles(str3);
                if (!titles.isEmpty() && titles.get(0).length() > 0) {
                    sb.append("\n").append(TextUtils.join(" · ", titles));
                }
                addRow(makeDialog, sb.toString(), -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.40
                    @Override // java.lang.Runnable
                    public void run() {
                        AppEntry appEntry;
                        Iterator it2 = MainActivity.this.apps.iterator();
                        while (true) {
                            if (!it2.hasNext()) {
                                appEntry = null;
                                break;
                            } else {
                                appEntry = (AppEntry) it2.next();
                                if (appEntry.pkg.equals(str3)) {
                                    break;
                                }
                            }
                        }
                        if (appEntry != null) {
                            MainActivity.this.launch(appEntry, null);
                        } else {
                            MainActivity.this.startQuick(null, str3);
                        }
                    }
                });
                z = true;
            }
        }
        if (!z) {
            addRow(makeDialog, "NOTHING WAITING", -2012870644, new Runnable() { // from class: com.aaron.bluehour.MainActivity.41
                @Override // java.lang.Runnable
                public void run() {
                }
            });
        }
        makeDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showSettingsDialog() {
        String str = "";
        try {
            str = " v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
        }
        Dialog makeDialog = makeDialog("ARCANA LAUNCHER" + str);
        addRow(makeDialog, "▸  THEME & DISPLAY", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.68
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.showDisplayMenu();
            }
        });
        addRow(makeDialog, "▸  THE CARD", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.69
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.showCardMenu();
            }
        });
        addRow(makeDialog, "▸  WALLPAPER", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.70
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.showWallpaperMenu();
            }
        });
        addRow(makeDialog, "▸  APPS", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.71
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.showAppsMenu();
            }
        });
        addRow(makeDialog, "▸  BACKUP", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.72
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.showBackupMenu();
            }
        });
        addRow(makeDialog, "▸  SETUP", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.73
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.showSetupMenu();
            }
        });
        makeDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showSetupMenu() {
        Dialog makeDialog = makeDialog("SETUP");
        addRow(makeDialog, "SET AS DEFAULT LAUNCHER", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.92
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.requestHomeRole(true);
            }
        });
        addRow(makeDialog, NotifService.isEnabled(this) ? "NOTIFICATION ACCESS: ON" : "NOTIFICATION ACCESS: ENABLE", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.93
            @Override // java.lang.Runnable
            public void run() {
                try {
                    MainActivity.this.startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                } catch (Exception e) {
                }
            }
        });
        makeDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showSlotDialog(final int i) {
        Dialog makeDialog = makeDialog("DOCK SLOT " + (i + 1));
        String quickSlot = this.prefs.quickSlot(i);
        if (quickSlot.length() <= 0) {
            quickSlot = defaultQuickPkg(i);
        }
        if (quickSlot != null && NotifService.has(quickSlot)) {
            final ArrayList arrayList = new ArrayList();
            arrayList.add(quickSlot);
            addRow(makeDialog, "◆ NOTIFICATIONS (" + NotifService.count(quickSlot) + ")", -1703918, new Runnable() { // from class: com.aaron.bluehour.MainActivity.57
                @Override // java.lang.Runnable
                public void run() {
                    MainActivity.this.showPeekDialog("WAITING", arrayList);
                }
            });
        }
        addRow(makeDialog, "CHOOSE APP", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.58
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.enterPickMode(i);
            }
        });
        addRow(makeDialog, "RESET TO DEFAULT", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.59
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.prefs.setQuickSlot(i, "");
                MainActivity.this.buildQuickRow();
            }
        });
        makeDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showThemeDialog() {
        Dialog makeDialog = makeDialog("CHOOSE HOUR");
        for (final Theme theme : Theme.all()) {
            addRow(makeDialog, (theme.id == Theme.get().id ? "▶ " : "") + theme.name, -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.104
                @Override // java.lang.Runnable
                public void run() {
                    MainActivity.this.prefs.setIntVal("theme", theme.id);
                    Theme.set(theme.id);
                    MainActivity.this.recreate();
                }
            });
        }
        makeDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showWallpaperMenu() {
        Dialog makeDialog = makeDialog("WALLPAPER");
        boolean exists = new File(getFilesDir(), "wallpaper_portrait.jpg").exists();
        boolean exists2 = new File(getFilesDir(), "wallpaper_landscape.jpg").exists();
        addRow(makeDialog, exists ? "PORTRAIT IMAGE ✓" : "SET PORTRAIT IMAGE", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.74
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.pickWallpaper(20);
            }
        });
        addRow(makeDialog, exists2 ? "LANDSCAPE IMAGE ✓" : "SET LANDSCAPE IMAGE", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.75
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.pickWallpaper(21);
            }
        });
        addRow(makeDialog, "CLEAR BOTH (USE SYSTEM WALLPAPER)", -16381940, new Runnable() { // from class: com.aaron.bluehour.MainActivity.76
            @Override // java.lang.Runnable
            public void run() {
                new File(MainActivity.this.getFilesDir(), "wallpaper_portrait.jpg").delete();
                new File(MainActivity.this.getFilesDir(), "wallpaper_landscape.jpg").delete();
                if (MainActivity.this.bg != null) {
                    MainActivity.this.bg.reloadWallpapers();
                }
                MainActivity.this.toast("WALLPAPERS CLEARED");
            }
        });
        makeDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showWeather(String str) {
        if (this.weatherText == null) {
            return;
        }
        if (str == null || str.length() == 0) {
            this.weatherText.setVisibility(HOME_CAT_MAX);
        } else {
            this.weatherText.setText(str);
            this.weatherText.setVisibility(0);
        }
    }

    private void shuffleTime() {
        List<String> recents = this.prefs.recents();
        if (recents.isEmpty()) {
            toast("NO CARD TO DRAW YET");
        } else {
            final String str = recents.get(0);
            this.walletCard.animate().rotationY(88.0f).setDuration(130L).withEndAction(new Runnable() { // from class: com.aaron.bluehour.MainActivity.94
                @Override // java.lang.Runnable
                public void run() {
                    MainActivity.this.walletCard.setRotationY(0.0f);
                    MainActivity.this.startQuick(null, str);
                }
            }).start();
        }
    }

    private void startMediaWatch() {
        try {
            this.msm = (MediaSessionManager) getSystemService("media_session");
            ComponentName componentName = new ComponentName(this, (Class<?>) NotifService.class);
            this.msm.addOnActiveSessionsChangedListener(this.sessionsListener, componentName);
            pickController(this.msm.getActiveSessions(componentName));
        } catch (Exception e) {
            if (this.playerView != null) {
                this.playerView.setVisibility(HOME_CAT_MAX);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startQuick(Intent intent, String str) {
        Intent intent2 = null;
        if (str != null) {
            try {
                intent2 = getPackageManager().getLaunchIntentForPackage(str);
            } catch (Exception e) {
                toast("CAN'T LAUNCH");
                return;
            }
        }
        if (intent2 != null) {
            intent = intent2;
        }
        if (intent == null) {
            toast("APP NOT FOUND");
            return;
        }
        Intent intent3 = new Intent(intent);
        intent3.addFlags(268435456);
        startActivity(intent3);
        flashAccent();
        if (str != null) {
            this.prefs.pushRecent(str);
        }
    }

    private void stopMediaWatch() {
        try {
            if (this.msm != null) {
                this.msm.removeOnActiveSessionsChangedListener(this.sessionsListener);
            }
        } catch (Exception e) {
        }
        if (this.mediaController != null) {
            try {
                this.mediaController.unregisterCallback(this.mediaCb);
            } catch (Exception e2) {
            }
            this.mediaController = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toast(String str) {
        Toast.makeText(this, str, 0).show();
    }

    private void toggleTorch() {
        try {
            CameraManager cameraManager = (CameraManager) getSystemService("camera");
            for (String str : cameraManager.getCameraIdList()) {
                Boolean bool = (Boolean) cameraManager.getCameraCharacteristics(str).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (bool != null && bool.booleanValue()) {
                    this.torchOn = !this.torchOn;
                    cameraManager.setTorchMode(str, this.torchOn);
                    toast(this.torchOn ? "TORCH ON" : "TORCH OFF");
                    return;
                }
            }
            toast("NO FLASH ON THIS DEVICE");
        } catch (Exception e) {
            toast("TORCH UNAVAILABLE");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateClock() {
        String str;
        Calendar calendar = Calendar.getInstance();
        boolean is24HourFormat = DateFormat.is24HourFormat(this);
        int i = calendar.get(11);
        int i2 = calendar.get(12);
        int i3 = is24HourFormat ? i : calendar.get(10);
        if (!is24HourFormat && i3 == 0) {
            i3 = 12;
        }
        this.timeText.setTime(String.format(Locale.US, "%d:%02d", Integer.valueOf(i3), Integer.valueOf(i2)));
        this.dayText.setText(new SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.getTime()).toUpperCase(Locale.getDefault()));
        this.dateText.setText(new SimpleDateFormat("M/d", Locale.getDefault()).format(calendar.getTime()));
        try {
            if (this.battText != null && this.battBox != null) {
                AlarmManager alarmManager = (AlarmManager) getSystemService("alarm");
                AlarmManager.AlarmClockInfo nextAlarmClock = alarmManager == null ? null : alarmManager.getNextAlarmClock();
                boolean z = this.playerView != null && this.playerView.getVisibility() == 0;
                if (nextAlarmClock == null) {
                    this.battBox.setVisibility(HOME_CAT_MAX);
                } else {
                    Calendar calendar2 = Calendar.getInstance();
                    calendar2.setTimeInMillis(nextAlarmClock.getTriggerTime());
                    this.battText.setText(new SimpleDateFormat(calendar2.get(6) == calendar.get(6) ? is24HourFormat ? "H:mm" : "h:mm" : is24HourFormat ? "EEE H:mm" : "EEE h:mm", Locale.getDefault()).format(calendar2.getTime()).toUpperCase(Locale.getDefault()));
                    if (!z) {
                        this.battBox.setVisibility(0);
                    }
                }
            }
        } catch (Exception e) {
        }
        boolean isDarkHour = isDarkHour(i);
        int i4 = Theme.get().shapeStyle == 2 ? -1 : Theme.get().subtitle;
        if (isDarkHour) {
            str = "DARK HOUR";
            Objects.requireNonNull(Theme.get());
            i4 = Ui.DARK_HOUR;
        } else {
            str = i < 5 ? "LATE NIGHT" : i < 11 ? "MORNING" : i < 17 ? "DAYTIME" : i < 20 ? "EVENING" : "LATE NIGHT";
        }
        this.phaseText.setText(str);
        this.phaseText.setTextColor(i4);
        if (isDarkHour != this.lastDh) {
            this.lastDh = isDarkHour;
            this.bg.setDarkHour(isDarkHour);
            this.ruleView.setBackgroundColor(isDarkHour ? -1281622072 : -1275068417);
            rebuildDock();
        }
    }

    private void updateDefaultHint() {
        boolean z;
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager roleManager = (RoleManager) getSystemService("role");
            z = roleManager == null || roleManager.isRoleHeld("android.app.role.HOME");
        } else {
            z = true;
        }
        this.defaultHint.setVisibility(z ? HOME_CAT_MAX : 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDots() {
        this.adapter.notifyDataSetChanged();
        for (int i = 0; i < this.dockRow.getChildCount(); i++) {
            View childAt = this.dockRow.getChildAt(i);
            if ((childAt instanceof MenuWordView) && (childAt.getTag() instanceof String)) {
                ((MenuWordView) childAt).setDot(catHasDot((String) childAt.getTag()));
            }
        }
        for (int i2 = 0; i2 < this.quickRow.getChildCount(); i2++) {
            View childAt2 = this.quickRow.getChildAt(i2);
            if (childAt2 instanceof GlyphView) {
                String str = childAt2.getTag() instanceof String ? (String) childAt2.getTag() : null;
                ((GlyphView) childAt2).setDot(str != null && NotifService.has(str));
            }
        }
        if (this.mailButton != null) {
            this.mailButton.setCount(NotifService.feedCount());
        }
        if (this.mailView != null) {
            this.mailView.refresh();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePlayerWidget() {
        if (this.playerView == null) {
            return;
        }
        MediaMetadata metadata = this.mediaController == null ? null : this.mediaController.getMetadata();
        if (this.mediaController == null || metadata == null) {
            this.playerView.hideAnimated();
            updateClock();
            return;
        }
        String string = metadata.getString("android.media.metadata.TITLE");
        String string2 = metadata.getString("android.media.metadata.ARTIST");
        long j = metadata.getLong("android.media.metadata.DURATION");
        Bitmap bitmap = metadata.getBitmap("android.media.metadata.ALBUM_ART");
        this.playerView.setAlbumArt(bitmap == null ? metadata.getBitmap("android.media.metadata.ART") : bitmap);
        PlaybackState playbackState = this.mediaController.getPlaybackState();
        boolean z = playbackState != null && playbackState.getState() == 3;
        long position = playbackState == null ? 0L : playbackState.getPosition();
        long elapsedRealtime = playbackState == null ? SystemClock.elapsedRealtime() : playbackState.getLastPositionUpdateTime();
        float playbackSpeed = playbackState == null ? 1.0f : playbackState.getPlaybackSpeed();
        this.battBox.setVisibility(HOME_CAT_MAX);
        this.playerView.showAnimated();
        PlayerView playerView = this.playerView;
        String str = string == null ? "—" : string;
        if (string2 == null) {
            string2 = "";
        }
        playerView.update(str, string2, z, j, position, elapsedRealtime, playbackSpeed);
    }

    private LinearLayout.LayoutParams wordLp() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.topMargin = Ui.dpi(this, -2.0f);
        layoutParams.gravity = 8388611;
        return layoutParams;
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        boolean z = this.mailView != null && this.mailView.getVisibility() == 0;
        boolean z2 = this.mediaHub != null && this.mediaHub.getVisibility() == 0;
        if (!this.drawerOpen && !z && !z2) {
            if (this.homeGd != null) {
                this.homeGd.onTouchEvent(motionEvent);
            }
            switch (motionEvent.getActionMasked()) {
                case 0:
                    this.swDownX = motionEvent.getRawX();
                    this.swDownY = motionEvent.getRawY();
                    this.swDownT = motionEvent.getEventTime();
                    break;
                case 1:
                    float rawX = motionEvent.getRawX() - this.swDownX;
                    float rawY = motionEvent.getRawY() - this.swDownY;
                    long eventTime = motionEvent.getEventTime() - this.swDownT;
                    if (rawX > Ui.dp(this, 55.0f) && rawX > Math.abs(rawY) * 1.4f && eventTime < 700) {
                        openFeed();
                        break;
                    }
                    break;
            }
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (intent == null || intent.getData() == null) {
            return;
        }
        if (i == 20 || i == 21) {
            String str = i == 20 ? "wallpaper_portrait.jpg" : "wallpaper_landscape.jpg";
            try {
                InputStream openInputStream = getContentResolver().openInputStream(intent.getData());
                FileOutputStream fileOutputStream = new FileOutputStream(new File(getFilesDir(), str));
                byte[] bArr = new byte[8192];
                while (true) {
                    int read = openInputStream.read(bArr);
                    if (read <= 0) {
                        break;
                    } else {
                        fileOutputStream.write(bArr, 0, read);
                    }
                }
                fileOutputStream.close();
                openInputStream.close();
                if (this.bg != null) {
                    this.bg.reloadWallpapers();
                }
                toast(i == 20 ? "PORTRAIT WALLPAPER SET" : "LANDSCAPE WALLPAPER SET");
                return;
            } catch (Exception e) {
                toast("COULDN'T LOAD IMAGE");
                return;
            }
        }
        if (i == 10) {
            try {
                InputStream openInputStream2 = getContentResolver().openInputStream(intent.getData());
                FileOutputStream fileOutputStream2 = new FileOutputStream(new File(getFilesDir(), "wallet_card.png"));
                byte[] bArr2 = new byte[8192];
                while (true) {
                    int read2 = openInputStream2.read(bArr2);
                    if (read2 <= 0) {
                        fileOutputStream2.close();
                        openInputStream2.close();
                        loadWalletIcon();
                        toast("CARD FACE SET");
                        return;
                    }
                    fileOutputStream2.write(bArr2, 0, read2);
                }
            } catch (Exception e2) {
                toast("COULDN'T LOAD IMAGE");
            }
        } else {
            if (i != HOME_CAT_MAX) {
                return;
            }
            try {
                InputStream openInputStream3 = getContentResolver().openInputStream(intent.getData());
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] bArr3 = new byte[4096];
                while (true) {
                    int read3 = openInputStream3.read(bArr3);
                    if (read3 <= 0) {
                        openInputStream3.close();
                        this.prefs.importJson(byteArrayOutputStream.toString("UTF-8"));
                        this.adapter.setHidden(this.prefs.hidden());
                        loadApps();
                        toast("SETUP RESTORED");
                        return;
                    }
                    byteArrayOutputStream.write(bArr3, 0, read3);
                }
            } catch (Exception e3) {
                toast("RESTORE FAILED — NOT A VALID BACKUP?");
            }
        }
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (this.mediaHub == null || !this.mediaHub.handleBack()) {
            if (this.mailView == null || !this.mailView.handleBack()) {
                if (this.addModeCat != null) {
                    exitAddMode();
                } else if (this.drawerOpen) {
                    closeDrawer();
                }
            }
        }
    }

    @Override // android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        applyFoldLayout();
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.prefs = new Prefs(this);
        Theme.set(this.prefs.intVal("theme"));
        setupWindow();
        this.root = new FrameLayout(this);
        this.bg = new BackgroundView(this);
        this.bg.reloadWallpapers();
        this.root.addView(this.bg, new FrameLayout.LayoutParams(-1, -1));
        buildHomeLayer();
        buildDrawerLayer();
        rebuildDock();
        this.flash = new View(this);
        this.flash.setBackgroundColor(-1);
        this.flash.setAlpha(0.0f);
        this.flash.setClickable(false);
        this.root.addView(this.flash, new FrameLayout.LayoutParams(-1, -1));
        this.halftoneView = new HalftoneView(this);
        this.root.addView(this.halftoneView, new FrameLayout.LayoutParams(-1, -1));
        this.halftoneView.setOn(Theme.get().id == 2);
        this.filigreeView = new FiligreeView(this);
        this.root.addView(this.filigreeView, new FrameLayout.LayoutParams(-1, -1));
        this.filigreeView.setOn(Theme.get().id == 3 && this.prefs.flag("ivory_frame"));
        this.crtView = new CRTView(this);
        this.root.addView(this.crtView, new FrameLayout.LayoutParams(-1, -1));
        applyCrt();
        setContentView(this.root);
        this.root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() { // from class: com.aaron.bluehour.MainActivity.6
            @Override // android.view.View.OnApplyWindowInsetsListener
            public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                MainActivity.this.applyInsets(windowInsets);
                return windowInsets;
            }
        });
        this.drawerLayer.setVisibility(4);
        this.root.post(new Runnable() { // from class: com.aaron.bluehour.MainActivity.7
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.drawerLayer.setTranslationY(MainActivity.this.root.getHeight());
                MainActivity.this.drawerLayer.setVisibility(0);
            }
        });
        loadApps();
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        closeDrawer();
    }

    @Override // android.app.Activity
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        if (i == 9 && iArr.length > 0 && iArr[0] == 0) {
            maybeFetchWeather();
        }
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        ensureNotifBound();
        updateClock();
        updateDots();
        updateDefaultHint();
        requestHomeRole(false);
        if (this.crtView != null) {
            this.crtView.hostResumed();
        }
        if (this.wasStopped) {
            this.wasStopped = false;
            animateMenuIn();
        }
    }

    @Override // android.app.Activity
    protected void onStart() {
        super.onStart();
        startMediaWatch();
        maybeFetchWeather();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.TIME_TICK");
        intentFilter.addAction("android.intent.action.TIME_SET");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        regReceiver(this.timeReceiver, intentFilter);
        regReceiver(this.notifReceiver, new IntentFilter(NotifService.ACTION_CHANGED));
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter2.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter2.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter2.addDataScheme("package");
        regReceiver(this.pkgReceiver, intentFilter2);
        try {
            if (this.launcherApps == null) {
                this.launcherApps = (LauncherApps) getSystemService("launcherapps");
            }
            if (this.launcherApps != null) {
                this.launcherApps.registerCallback(this.launcherCb);
            }
        } catch (Exception e) {
        }
        if (this.firstStart) {
            this.firstStart = false;
        } else {
            scheduleAppReload();
        }
    }

    @Override // android.app.Activity
    protected void onStop() {
        super.onStop();
        this.wasStopped = true;
        stopMediaWatch();
        if (this.crtView != null) {
            this.crtView.hostStopped();
        }
        if (this.playerView != null) {
            this.playerView.hostStopped();
        }
        try {
            unregisterReceiver(this.timeReceiver);
        } catch (Exception e) {
        }
        try {
            unregisterReceiver(this.notifReceiver);
        } catch (Exception e2) {
        }
        try {
            unregisterReceiver(this.pkgReceiver);
        } catch (Exception e3) {
        }
        try {
            if (this.launcherApps != null) {
                this.launcherApps.unregisterCallback(this.launcherCb);
            }
        } catch (Exception e4) {
        }
        this.appReloadHandler.removeCallbacks(this.appReloadRun);
    }
}
