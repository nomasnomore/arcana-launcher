package com.aaron.bluehour;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Prefs {

    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getSharedPreferences("bluehour", Context.MODE_PRIVATE);
    }

    // ----- hidden apps (by package) -----
    public Set<String> hidden() {
        return new HashSet<String>(sp.getStringSet("hidden", Collections.<String>emptySet()));
    }

    public void hide(String pkg) {
        Set<String> h = hidden();
        h.add(pkg);
        sp.edit().putStringSet("hidden", h).apply();
    }

    public void unhide(String pkg) {
        Set<String> h = hidden();
        h.remove(pkg);
        sp.edit().putStringSet("hidden", h).apply();
    }

    // ----- user-editable categories -----
    // "cats" = ordered names joined with "|"; per-cat membership + JP subtitle.

    public List<String> catNames() {
        String v = sp.getString("cats", "");
        List<String> out = new ArrayList<String>();
        if (!TextUtils.isEmpty(v)) out.addAll(Arrays.asList(v.split("\\|")));
        return out;
    }

    public void setCatNames(List<String> names) {
        sp.edit().putString("cats", TextUtils.join("|", names)).apply();
    }

    public Set<String> catApps(String name) {
        return new HashSet<String>(sp.getStringSet("catapps_" + name,
                Collections.<String>emptySet()));
    }

    public void setCatApps(String name, Set<String> pkgs) {
        sp.edit().putStringSet("catapps_" + name, pkgs).apply();
    }

    public String catJp(String name) {
        return sp.getString("catjp_" + name, "");
    }

    public void setCatJp(String name, String jp) {
        sp.edit().putString("catjp_" + name, jp).apply();
    }

    public void addCat(String name, String jp) {
        List<String> names = catNames();
        if (names.contains(name)) return;
        names.add(name);
        setCatNames(names);
        setCatJp(name, jp);
    }

    public void renameCat(String oldName, String newName) {
        List<String> names = catNames();
        int i = names.indexOf(oldName);
        if (i < 0 || names.contains(newName)) return;
        names.set(i, newName);
        setCatNames(names);
        setCatApps(newName, catApps(oldName));
        setCatJp(newName, catJp(oldName));
        sp.edit().remove("catapps_" + oldName).remove("catjp_" + oldName).apply();
    }

    public void deleteCat(String name) {
        List<String> names = catNames();
        names.remove(name);
        setCatNames(names);
        sp.edit().remove("catapps_" + name).remove("catjp_" + name).apply();
    }

    public void moveCat(String name, int delta) {
        List<String> names = catNames();
        int i = names.indexOf(name);
        int j = i + delta;
        if (i < 0 || j < 0 || j >= names.size()) return;
        Collections.swap(names, i, j);
        setCatNames(names);
    }

    public void toggleCatApp(String name, String pkg) {
        Set<String> s = catApps(name);
        if (!s.remove(pkg)) s.add(pkg);
        setCatApps(name, s);
    }

    // ----- media hub (tablet): attached media apps, ordered -----
    public List<String> mediaApps() {
        String v = sp.getString("media_apps", "");
        List<String> out = new ArrayList<String>();
        if (!TextUtils.isEmpty(v)) out.addAll(Arrays.asList(v.split("\\|")));
        return out;
    }

    public void addMediaApp(String pkg) {
        List<String> l = mediaApps();
        if (!l.contains(pkg)) { l.add(pkg); sp.edit()
                .putString("media_apps", TextUtils.join("|", l)).apply(); }
    }

    public void removeMediaApp(String pkg) {
        List<String> l = mediaApps();
        if (l.remove(pkg)) sp.edit()
                .putString("media_apps", TextUtils.join("|", l)).apply();
    }

    // ----- dock quick slots (0..4); "" = built-in default -----
    public String quickSlot(int i) {
        return sp.getString("quick_" + i, "");
    }

    public void setQuickSlot(int i, String pkg) {
        sp.edit().putString("quick_" + i, pkg == null ? "" : pkg).apply();
    }

    // ----- recents (ordered, most recent first, capped) -----
    public List<String> recents() {
        String v = sp.getString("recents", "");
        List<String> out = new ArrayList<String>();
        if (!TextUtils.isEmpty(v)) out.addAll(Arrays.asList(v.split("\\|")));
        return out;
    }

    public void pushRecent(String pkg) {
        List<String> r = recents();
        r.remove(pkg);
        r.add(0, pkg);
        while (r.size() > 10) r.remove(r.size() - 1);
        sp.edit().putString("recents", TextUtils.join("|", r)).apply();
    }

    /** Scrub an uninstalled package from every corner of the config. */
    public void removePkgEverywhere(String pkg) {
        for (String name : catNames()) {
            Set<String> s = catApps(name);
            if (s.remove(pkg)) setCatApps(name, s);
        }
        for (int i = 0; i < 5; i++) {
            if (pkg.equals(quickSlot(i))) setQuickSlot(i, "");
        }
        List<String> r = recents();
        if (r.remove(pkg)) {
            sp.edit().putString("recents", TextUtils.join("|", r)).apply();
        }
        unhide(pkg);
    }

    // ----- launch counts for search ranking -----
    public int uses(String pkg) {
        return sp.getInt("uses_" + pkg, 0);
    }

    public void bumpUses(String pkg) {
        sp.edit().putInt("uses_" + pkg, uses(pkg) + 1).apply();
    }

    // ----- backup / restore -----

    public String exportJson() throws org.json.JSONException {
        org.json.JSONObject o = new org.json.JSONObject();
        o.put("v", 1);
        org.json.JSONArray cats = new org.json.JSONArray();
        for (String name : catNames()) {
            org.json.JSONObject c = new org.json.JSONObject();
            c.put("name", name);
            c.put("jp", catJp(name));
            c.put("apps", new org.json.JSONArray(catApps(name)));
            cats.put(c);
        }
        o.put("cats", cats);
        o.put("hidden", new org.json.JSONArray(hidden()));
        org.json.JSONArray quick = new org.json.JSONArray();
        for (int i = 0; i < 5; i++) quick.put(quickSlot(i));
        o.put("quick", quick);
        o.put("recents", new org.json.JSONArray(recents()));
        org.json.JSONObject uses = new org.json.JSONObject();
        for (java.util.Map.Entry<String, ?> en : sp.getAll().entrySet()) {
            if (en.getKey().startsWith("uses_") && en.getValue() instanceof Integer) {
                uses.put(en.getKey().substring(5), en.getValue());
            }
        }
        o.put("uses", uses);
        return o.toString(2);
    }

    public void importJson(String json) throws org.json.JSONException {
        org.json.JSONObject o = new org.json.JSONObject(json);
        SharedPreferences.Editor ed = sp.edit();
        // wipe current category/uses data before applying the backup
        for (String k : sp.getAll().keySet()) {
            if (k.startsWith("catapps_") || k.startsWith("catjp_")
                    || k.startsWith("uses_") || k.startsWith("quick_")) {
                ed.remove(k);
            }
        }
        org.json.JSONArray cats = o.getJSONArray("cats");
        List<String> names = new ArrayList<String>();
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < cats.length(); i++) {
            org.json.JSONObject c = cats.getJSONObject(i);
            // sanitize: "|" is our list delimiter, so strip it; drop empties/dupes
            String name = c.getString("name").replace("|", "").trim();
            if (name.length() == 0) continue;
            if (!seen.add(name.toLowerCase())) continue;
            names.add(name);
            ed.putString("catjp_" + name, c.optString("jp", ""));
            Set<String> apps = new HashSet<String>();
            org.json.JSONArray aa = c.getJSONArray("apps");
            for (int j = 0; j < aa.length(); j++) apps.add(aa.getString(j));
            ed.putStringSet("catapps_" + name, apps);
        }
        ed.putString("cats", TextUtils.join("|", names));

        Set<String> hid = new HashSet<String>();
        org.json.JSONArray ha = o.optJSONArray("hidden");
        if (ha != null) for (int i = 0; i < ha.length(); i++) hid.add(ha.getString(i));
        ed.putStringSet("hidden", hid);

        org.json.JSONArray quick = o.optJSONArray("quick");
        if (quick != null) {
            for (int i = 0; i < 5 && i < quick.length(); i++) {
                ed.putString("quick_" + i, quick.optString(i, ""));
            }
        }

        org.json.JSONArray rec = o.optJSONArray("recents");
        if (rec != null) {
            List<String> r = new ArrayList<String>();
            for (int i = 0; i < rec.length(); i++) r.add(rec.getString(i));
            ed.putString("recents", TextUtils.join("|", r));
        }

        org.json.JSONObject uses = o.optJSONObject("uses");
        if (uses != null) {
            java.util.Iterator<String> it = uses.keys();
            while (it.hasNext()) {
                String pkg = it.next();
                ed.putInt("uses_" + pkg, uses.optInt(pkg, 0));
            }
        }
        ed.apply();
    }

    // ----- misc flags / values -----
    public boolean flag(String k) { return sp.getBoolean(k, false); }
    public void setFlag(String k, boolean v) { sp.edit().putBoolean(k, v).apply(); }
    public int intVal(String k) { return sp.getInt(k, 0); }
    public void setIntVal(String k, int v) { sp.edit().putInt(k, v).apply(); }
    public long longVal(String k) { return sp.getLong(k, 0L); }
    public void setLongVal(String k, long v) { sp.edit().putLong(k, v).apply(); }
    public String strVal(String k) { return sp.getString(k, ""); }
    public void setStrVal(String k, String v) { sp.edit().putString(k, v).apply(); }
}
