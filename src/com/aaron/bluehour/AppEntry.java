package com.aaron.bluehour;

import android.graphics.Bitmap;

import java.util.Locale;

public class AppEntry {
    public final String pkg;
    public final String cls;
    public final String label;
    public final String labelUp;
    public volatile Bitmap icon; // loaded async
    public volatile int cat = Cats.TOOLS;

    public AppEntry(String pkg, String cls, String label) {
        this.pkg = pkg;
        this.cls = cls;
        this.label = label;
        this.labelUp = label.toUpperCase(Locale.getDefault());
    }

    public String id() {
        return pkg + "/" + cls;
    }
}
