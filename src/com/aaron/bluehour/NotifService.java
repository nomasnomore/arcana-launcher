package com.aaron.bluehour;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Tracks notifications for two consumers:
 *   1. the per-icon dots (packages that have a non-ongoing notification), and
 *   2. the MAIL feed — one rich item per notification, newest first, with the
 *      full body text and the tap-through intent, for the mail panel.
 */
public class NotifService extends NotificationListenerService {

    public static final String ACTION_CHANGED = "com.aaron.bluehour.NOTIF_CHANGED";

    /** One notification, as the mail panel needs it. */
    public static final class Item {
        public final String key;
        public final String pkg;
        public final String title;
        public final String text;
        public final long when;
        public final PendingIntent intent;

        Item(String key, String pkg, String title, String text,
             long when, PendingIntent intent) {
            this.key = key;
            this.pkg = pkg;
            this.title = title;
            this.text = text;
            this.when = when;
            this.intent = intent;
        }
    }

    private static final HashMap<String, ArrayList<String>> NOTES =
            new HashMap<String, ArrayList<String>>();
    // key -> Item, so an update to the same notification replaces its entry
    private static final HashMap<String, Item> FEED = new HashMap<String, Item>();
    // the currently-connected service instance, for cancelling notifications
    private static NotifService instance;

    /** Dismiss every clearable notification (also clears them from the shade). */
    public static void clearAll() {
        try {
            if (instance != null) instance.cancelAllNotifications();
        } catch (Exception ignored) {}
    }

    /** Dismiss a single notification by its key. */
    public static void clearKey(String key) {
        try {
            if (instance != null && key != null) instance.cancelNotification(key);
        } catch (Exception ignored) {}
    }

    public static boolean has(String pkg) {
        synchronized (NOTES) {
            return NOTES.containsKey(pkg);
        }
    }

    public static int count(String pkg) {
        synchronized (NOTES) {
            ArrayList<String> l = NOTES.get(pkg);
            return l == null ? 0 : l.size();
        }
    }

    public static List<String> titles(String pkg) {
        synchronized (NOTES) {
            ArrayList<String> l = NOTES.get(pkg);
            return l == null ? new ArrayList<String>() : new ArrayList<String>(l);
        }
    }

    /** How many notifications are waiting overall (the mail badge count). */
    public static int feedCount() {
        synchronized (FEED) {
            return FEED.size();
        }
    }

    /** A snapshot of the whole feed, newest first. */
    public static List<Item> feed() {
        ArrayList<Item> out;
        synchronized (FEED) {
            out = new ArrayList<Item>(FEED.values());
        }
        Collections.sort(out, new Comparator<Item>() {
            @Override public int compare(Item a, Item b) {
                return a.when < b.when ? 1 : (a.when > b.when ? -1 : 0);
            }
        });
        return out;
    }

    public static boolean isEnabled(Context c) {
        String flat = Settings.Secure.getString(
                c.getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(c.getPackageName());
    }

    private static String extra(Notification n, String key) {
        try {
            if (n.extras == null) return null;
            CharSequence t = n.extras.getCharSequence(key);
            return t == null ? null : t.toString().trim();
        } catch (Exception e) {
            return null;
        }
    }

    private static String titleOf(StatusBarNotification sbn) {
        try {
            Notification n = sbn.getNotification();
            String t = extra(n, Notification.EXTRA_TITLE);
            if (t != null && t.length() > 0) return t;
            t = extra(n, Notification.EXTRA_TEXT);
            if (t != null && t.length() > 0) return t;
            if (n.tickerText != null) return n.tickerText.toString();
        } catch (Exception ignored) {
        }
        return null;
    }

    /** The best available body text: big text > text > sub text > ticker. */
    private static String bodyOf(StatusBarNotification sbn) {
        try {
            Notification n = sbn.getNotification();
            String t = extra(n, Notification.EXTRA_BIG_TEXT);
            if (t != null && t.length() > 0) return t;
            t = extra(n, Notification.EXTRA_TEXT);
            if (t != null && t.length() > 0) return t;
            t = extra(n, Notification.EXTRA_SUB_TEXT);
            if (t != null && t.length() > 0) return t;
            if (n.tickerText != null) return n.tickerText.toString();
        } catch (Exception ignored) {
        }
        return null;
    }

    private static void add(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        String title = titleOf(sbn);

        // ---- dots data (unchanged behaviour) ----
        synchronized (NOTES) {
            ArrayList<String> l = NOTES.get(pkg);
            if (l == null) {
                l = new ArrayList<String>();
                NOTES.put(pkg, l);
            }
            if (title != null && !l.contains(title) && l.size() < 4) {
                l.add(title);
            } else if (title == null && l.isEmpty()) {
                l.add(""); // presence without a readable title still counts
            }
        }

        // ---- rich feed item (one per key) ----
        try {
            String body = bodyOf(sbn);
            PendingIntent pi = sbn.getNotification().contentIntent;
            String key = sbn.getKey();
            if (key == null) key = pkg + "#" + sbn.getId();
            Item it = new Item(key, pkg,
                    title == null ? "" : title,
                    body == null ? "" : body,
                    sbn.getPostTime(), pi);
            synchronized (FEED) {
                FEED.put(key, it);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onListenerConnected() {
        instance = this;
        rebuild();
    }

    @Override
    public void onListenerDisconnected() {
        instance = null;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.isOngoing()) return;
        add(sbn);
        notifyChanged();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        rebuild();
    }

    private void rebuild() {
        synchronized (NOTES) {
            NOTES.clear();
        }
        synchronized (FEED) {
            FEED.clear();
        }
        try {
            StatusBarNotification[] all = getActiveNotifications();
            if (all != null) {
                for (StatusBarNotification s : all) {
                    if (s != null && !s.isOngoing()) add(s);
                }
            }
        } catch (Exception ignored) {
        }
        notifyChanged();
    }

    private void notifyChanged() {
        Intent i = new Intent(ACTION_CHANGED);
        i.setPackage(getPackageName());
        sendBroadcast(i);
    }
}
