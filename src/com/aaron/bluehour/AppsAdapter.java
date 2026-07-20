package com.aaron.bluehour;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AppsAdapter extends BaseAdapter {

    private final Context ctx;
    private List<AppEntry> all = new ArrayList<AppEntry>();
    private final List<AppEntry> visible = new ArrayList<AppEntry>();
    private Set<String> hidden = new HashSet<String>();
    private String filter = "";
    private Set<String> catPkgs = null;     // null = all apps
    private List<String> orderedPkgs = null; // non-null = fixed order (recents)
    private Set<String> addMembers = null;  // non-null = add-mode (show all, mark members)
    private Map<String, Integer> uses = new HashMap<String, Integer>();

    public AppsAdapter(Context c) {
        ctx = c;
    }

    public void setData(List<AppEntry> apps) {
        all = apps;
        refresh();
    }

    public void setHidden(Set<String> h) {
        hidden = h;
        refresh();
    }

    public void setFilter(String f) {
        filter = f == null ? "" : f.trim().toUpperCase(Locale.getDefault());
        refresh();
    }

    /** Category view: only these packages (null = everything). */
    public void setCatPkgs(Set<String> pkgs) {
        catPkgs = pkgs;
        refresh();
    }

    /** Recents view: exactly these packages in exactly this order. */
    public void setOrderedPkgs(List<String> pkgs) {
        orderedPkgs = pkgs;
        refresh();
    }

    /** Add-mode: show all apps, mark membership of this live set. */
    public void setAddMembers(Set<String> members) {
        addMembers = members;
        refresh();
    }

    public void setUses(Map<String, Integer> u) {
        uses = u;
    }

    public void refresh() {
        visible.clear();
        boolean adding = addMembers != null;

        // recents: preserve the given order, no sorting
        if (orderedPkgs != null && filter.length() == 0 && !adding) {
            for (String p : orderedPkgs) {
                if (hidden.contains(p)) continue;
                for (AppEntry e : all) {
                    if (e.pkg.equals(p)) {
                        visible.add(e);
                        break;
                    }
                }
            }
            notifyDataSetChanged();
            return;
        }

        if (filter.length() == 0) {
            for (AppEntry e : all) {
                if (hidden.contains(e.pkg)) continue;
                if (!adding && catPkgs != null && !catPkgs.contains(e.pkg)) continue;
                if (!adding && orderedPkgs != null && !orderedPkgs.contains(e.pkg)) continue;
                visible.add(e);
            }
        } else {
            List<AppEntry> pre = new ArrayList<AppEntry>();
            List<AppEntry> sub = new ArrayList<AppEntry>();
            for (AppEntry e : all) {
                if (hidden.contains(e.pkg)) continue;
                if (!adding && catPkgs != null && !catPkgs.contains(e.pkg)) continue;
                if (!adding && orderedPkgs != null && !orderedPkgs.contains(e.pkg)) continue;
                if (e.labelUp.startsWith(filter)) pre.add(e);
                else if (e.labelUp.contains(filter)) sub.add(e);
            }
            // most-launched first within each group
            Comparator<AppEntry> byUse = new Comparator<AppEntry>() {
                @Override
                public int compare(AppEntry a, AppEntry b) {
                    int ua = useOf(a), ub = useOf(b);
                    if (ua != ub) return ub - ua;
                    return a.label.compareToIgnoreCase(b.label);
                }
            };
            Collections.sort(pre, byUse);
            Collections.sort(sub, byUse);
            visible.addAll(pre);
            visible.addAll(sub);
        }
        notifyDataSetChanged();
    }

    private int useOf(AppEntry e) {
        Integer u = uses.get(e.pkg);
        return u == null ? 0 : u.intValue();
    }

    public AppEntry first() {
        return visible.isEmpty() ? null : visible.get(0);
    }

    public int firstIndexForLetter(char letter) {
        for (int i = 0; i < visible.size(); i++) {
            char c0 = visible.get(i).labelUp.length() > 0
                    ? visible.get(i).labelUp.charAt(0) : ' ';
            if (letter == '#') {
                if (c0 < 'A' || c0 > 'Z') return i;
            } else if (c0 == letter) {
                return i;
            }
        }
        return -1;
    }

    @Override public int getCount() { return visible.size(); }
    @Override public Object getItem(int i) { return visible.get(i); }
    @Override public long getItemId(int i) { return i; }

    @Override
    public View getView(int pos, View convert, ViewGroup parent) {
        SlantRowView v = (convert instanceof SlantRowView)
                ? (SlantRowView) convert : new SlantRowView(ctx);
        AppEntry e = visible.get(pos);
        int member = addMembers == null ? -1
                : (addMembers.contains(e.pkg) ? 1 : 0);
        v.bind(e, NotifService.has(e.pkg), pos, member);
        return v;
    }
}
