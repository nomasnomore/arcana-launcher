package com.aaron.bluehour;

/**
 * The theme engine. Every color that defines a game's look lives here as
 * a semantic role, so a whole "Hour" is one preset. Neutral colors (pure
 * white, near-black dialog text, shadows) stay as literals in the views —
 * they read fine on any theme. Blue Hour's values are the exact originals
 * so it stays pixel-identical.
 *
 * ids: 0 = Blue Hour (P3), 1 = Yellow Hour (P4), 2 = Red Hour (P5).
 */
public final class Theme {

    public final int id;
    public final String name;

    /** A text color that reads on top of accent (white or black by luminance). */
    public int accentText() {
        int r = (accent >> 16) & 0xFF, g = (accent >> 8) & 0xFF, b = accent & 0xFF;
        double lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
        return lum > 0.6 ? 0xFF141414 : 0xFFFFFFFF;
    }

    // background gradient (top → mid → bottom)
    public final int bgTop, bgMid, bgBot;

    // structural accents
    public final int accent;        // slashes, progress, blue diagonal, active bar
    public final int accentBright;  // phase text, EQ bars, bright highlights
    public final int pop;           // selection pop + notification dots

    // text
    public final int textLight;     // main light text (clock, day, words)
    public final int catWord;       // category words in the column
    public final int subtitle;      // JP subtitles, artist line
    public final int rule;          // underline rule color

    // drawer card (the white panel) + dialogs stay light; these tune it
    public final int cardFace;      // drawer card fill
    public final int cardAccent;    // diagonal accent on the drawer card

    // dark-hour wash (green in all themes — it's the Dark Hour, always)
    public final int darkHourWash = 0x2E2FBF7A;
    public final int darkHourText = 0xFF9BFFC8;

    // shape language: 0 = sharp slash (P3/P5), 1 = rounded (P4)
    public final int shapeStyle;

    // dark ink for text on the white date tag (per theme)
    public int dateInk = 0xFF0A2FA8;

    // P4's rainbow motif — used for underlines etc. when true
    public boolean rainbow = false;

    // P5's ransom-note lettering — per-letter jitter + cut-out boxes
    public boolean ransom = false;
    public static final int[] RAINBOW = {
            0xFFE53935, 0xFFFB8C00, 0xFFFDD835,
            0xFF43A047, 0xFF1E88E5, 0xFF8E24AA};

    private Theme(int id, String name, int bgTop, int bgMid, int bgBot,
                  int accent, int accentBright, int pop, int textLight,
                  int catWord, int subtitle, int rule, int cardFace,
                  int cardAccent, int shapeStyle) {
        this.id = id;
        this.name = name;
        this.bgTop = bgTop;
        this.bgMid = bgMid;
        this.bgBot = bgBot;
        this.accent = accent;
        this.accentBright = accentBright;
        this.pop = pop;
        this.textLight = textLight;
        this.catWord = catWord;
        this.subtitle = subtitle;
        this.rule = rule;
        this.cardFace = cardFace;
        this.cardAccent = cardAccent;
        this.shapeStyle = shapeStyle;
    }

    // ---- BLUE HOUR (P3) — exact originals ----
    public static final Theme BLUE = new Theme(0, "BLUE HOUR",
            0xFF2564F5, 0xFF1745CF, 0xFF0E2E9E,   // bg gradient
            0xFF2B5CFF,   // accent (royal blue)
            0xFF37C4F5,   // accentBright (cyan)
            0xFFE60012,   // pop (red)
            0xFFF2F5FA,   // textLight
            0xFFF6F9FF,   // catWord
            0xFF6FDBFF,   // subtitle
            0xB3FFFFFF,   // rule
            0xFFF4F6F9,   // cardFace
            0xFF2B5CFF,   // cardAccent
            0);           // sharp

    // ---- YELLOW HOUR (P4) — gold accents, light text (readable on any bg).
    // Authentic black-on-yellow is a refinement to opt into once a matching
    // yellow wallpaper is set.
    public static final Theme YELLOW = new Theme(1, "YELLOW HOUR",
            0xFFF7C518, 0xFFE0A400, 0xFF7A5600,   // warm gold gradient (no-wallpaper bg)
            0xFFFFC21E,   // accent (gold — slashes, bars, focus)
            0xFFFFE24D,   // accentBright (bright yellow — phase, EQ, progress)
            0xFF141414,   // pop (black — P4's high-contrast selection pop)
            0xFFF7F3E6,   // textLight (warm white)
            0xFFFFF3C4,   // catWord (pale gold-white)
            0xFFFFD84D,   // subtitle (bright amber)
            0xB3FFE24D,   // rule (yellow)
            0xFFFFF3C4,   // cardFace (pale yellow)
            0xFF141414,   // cardAccent (black diagonal — very P4)
            1);           // rounded
    static { YELLOW.rainbow = true; YELLOW.dateInk = 0xFF141414; }

    // ---- RED HOUR (P5) — red accents, white text. Ransom-note lettering
    // and jagged shapes come in a later pass with Aaron watching.
    public static final Theme RED = new Theme(2, "RED HOUR",
            0xFF12080A, 0xFF2A0609, 0xFF0A0304,   // near-black, red tint (no-wallpaper bg)
            0xFFE60012,   // accent (P5 red)
            0xFFFF3547,   // accentBright (hot red)
            0xFFFFFFFF,   // pop (white — P5 flips selection to white)
            0xFFF4F4F4,   // textLight (white)
            0xFFF6F6F6,   // catWord (white)
            0xFFFF4D5C,   // subtitle (light red)
            0xB3E60012,   // rule (red)
            0xFFF2ECEC,   // cardFace (light — drawer keeps dark text readable)
            0xFFE60012,   // cardAccent (red diagonal)
            2);           // jagged (P5)
    static { RED.dateInk = 0xFF8A0008; RED.ransom = true; }

    private static final Theme[] ALL = {BLUE, YELLOW, RED};
    private static Theme current = BLUE;

    public static Theme get() {
        return current;
    }

    public static void set(int id) {
        for (Theme t : ALL) {
            if (t.id == id) {
                current = t;
                return;
            }
        }
        current = BLUE;
    }

    public static Theme[] all() {
        return ALL;
    }
}
