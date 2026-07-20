package com.aaron.bluehour;

import android.content.pm.ApplicationInfo;

/** App categories — set matches Aaron's home mock. */
public final class Cats {

    public static final int ALL = -1;
    public static final int MAPS = 0;
    public static final int MEDIA = 1;
    public static final int COMM = 2;
    public static final int TOOLS = 3;
    public static final int LIFE = 4;
    public static final int GAMES = 5;
    public static final int SYSTEM = 6;

    public static final int[] ORDER = {MAPS, MEDIA, COMM, TOOLS, LIFE, GAMES, SYSTEM};

    private Cats() {}

    public static String label(int c) {
        switch (c) {
            case MAPS:   return "MAPS";
            case MEDIA:  return "MEDIA";
            case COMM:   return "COMMUNICATION";
            case TOOLS:  return "TOOLS";
            case LIFE:   return "LIFESTYLE";
            case GAMES:  return "GAMES";
            case SYSTEM: return "SYSTEM";
            default:     return "ALL APPS";
        }
    }

    public static String jp(int c) {
        switch (c) {
            case MAPS:   return "マップ";
            case MEDIA:  return "メディア";
            case COMM:   return "コミュニケーション";
            case TOOLS:  return "ツール";
            case LIFE:   return "ライフスタイル";
            case GAMES:  return "ゲーム";
            case SYSTEM: return "システム";
            default:     return "すべて";
        }
    }

    public static int fromAppInfo(ApplicationInfo ai) {
        if (ai == null) return TOOLS;
        if ((ai.flags & ApplicationInfo.FLAG_IS_GAME) != 0) return GAMES;
        switch (ai.category) {
            case ApplicationInfo.CATEGORY_GAME:
                return GAMES;
            case ApplicationInfo.CATEGORY_SOCIAL:
                return COMM;
            case ApplicationInfo.CATEGORY_AUDIO:
            case ApplicationInfo.CATEGORY_VIDEO:
            case ApplicationInfo.CATEGORY_IMAGE:
            case ApplicationInfo.CATEGORY_NEWS:
                return MEDIA;
            case ApplicationInfo.CATEGORY_MAPS:
                return MAPS;
            case ApplicationInfo.CATEGORY_PRODUCTIVITY:
                return TOOLS;
            default:
                boolean system = (ai.flags & (ApplicationInfo.FLAG_SYSTEM
                        | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
                return system ? SYSTEM : TOOLS;
        }
    }
}
