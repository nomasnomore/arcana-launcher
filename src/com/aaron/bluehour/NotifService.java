package com.aaron.bluehour;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Tracks which packages have (non-ongoing) notifications, and their titles. */
public class NotifService extends NotificationListenerService {

    public static final String ACTION_CHANGED = "com.aaron.bluehour.NOTIF_CHANGED";
    private static final HashMap<String, ArrayList<String>> NOTES =
            new HashMap<String, ArrayList<String>>();

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

    public static boolean isEnabled(Context c) {
        String flat = Settings.Secure.getString(
                c.getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(c.getPackageName());
    }

    private static String titleOf(StatusBarNotification sbn) {
        try {
            Notification n = sbn.getNotification();
            if (n.extras != null) {
                CharSequence t = n.extras.getCharSequence(Notification.EXTRA_TITLE);
                if (t == null) t = n.extras.getCharSequence(Notification.EXTRA_TEXT);
                if (t != null) return t.toString();
            }
            if (n.tickerText != null) return n.tickerText.toString();
        } catch (Exception ignored) {
        }
        return null;
    }

    private static void add(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        String title = titleOf(sbn);
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
    }

    @Override
    public void onListenerConnected() {
        rebuild();
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
