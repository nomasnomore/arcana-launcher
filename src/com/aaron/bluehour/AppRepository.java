package com.aaron.bluehour;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Queries launchable apps and loads their icons on a background thread. */
public class AppRepository {

    public interface Callback {
        void onLoaded(List<AppEntry> apps);
        void onIconsUpdated();
    }

    private static final int ICON_PX = 128;

    public static void load(final Context ctx, final Callback cb) {
        final Handler main = new Handler(Looper.getMainLooper());
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final PackageManager pm = ctx.getPackageManager();
                Intent q = new Intent(Intent.ACTION_MAIN);
                q.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> ris;
                try {
                    ris = pm.queryIntentActivities(q, 0);
                } catch (Exception e) {
                    ris = new ArrayList<ResolveInfo>();
                }

                final Prefs prefs = new Prefs(ctx);
                final List<AppEntry> entries = new ArrayList<AppEntry>();
                final List<ResolveInfo> kept = new ArrayList<ResolveInfo>();
                for (ResolveInfo ri : ris) {
                    if (ri.activityInfo == null) continue;
                    String pkg = ri.activityInfo.packageName;
                    if (ctx.getPackageName().equals(pkg)) continue;
                    CharSequence l = ri.loadLabel(pm);
                    String label = l == null ? pkg : l.toString().trim();
                    if (label.length() == 0) label = pkg;
                    AppEntry e = new AppEntry(pkg, ri.activityInfo.name, label);
                    e.cat = Cats.fromAppInfo(ri.activityInfo.applicationInfo);
                    entries.add(e);
                    kept.add(ri);
                }

                // no seeding: a fresh install starts with zero categories —
                // just "+ NEW" and the pinned SYSTEM row. Users build their own.

                // sort entries + their ResolveInfos together, by label
                final List<Integer> order = new ArrayList<Integer>();
                for (int i = 0; i < entries.size(); i++) order.add(Integer.valueOf(i));
                Collections.sort(order, new Comparator<Integer>() {
                    @Override
                    public int compare(Integer a, Integer b) {
                        return entries.get(a.intValue()).label
                                .compareToIgnoreCase(entries.get(b.intValue()).label);
                    }
                });
                final List<AppEntry> sorted = new ArrayList<AppEntry>();
                final List<ResolveInfo> sortedRi = new ArrayList<ResolveInfo>();
                for (Integer i : order) {
                    sorted.add(entries.get(i.intValue()));
                    sortedRi.add(kept.get(i.intValue()));
                }

                main.post(new Runnable() {
                    @Override
                    public void run() {
                        cb.onLoaded(sorted);
                    }
                });

                // icons, batched notifications
                int sinceNotify = 0;
                for (int i = 0; i < sorted.size(); i++) {
                    try {
                        Drawable d = sortedRi.get(i).loadIcon(pm);
                        if (d != null) sorted.get(i).icon = toBitmap(d, ICON_PX);
                    } catch (Throwable ignored) {
                    }
                    sinceNotify++;
                    if (sinceNotify >= 14) {
                        sinceNotify = 0;
                        main.post(new Runnable() {
                            @Override
                            public void run() {
                                cb.onIconsUpdated();
                            }
                        });
                    }
                }
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        cb.onIconsUpdated();
                    }
                });
            }
        }, "bluehour-load");
        t.setPriority(Thread.NORM_PRIORITY - 1);
        t.start();
    }

    static Bitmap toBitmap(Drawable d, int size) {
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, size, size);
        d.draw(c);
        return b;
    }
}
