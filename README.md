# Arcana Launcher

A custom Android home launcher inspired by the *Persona* menu style — hand-drawn entirely in code, with **three complete swappable themes** modeled on the modern Persona look (P3 / P4 / P5). No ripped game assets, no logos: only original UI that echoes the *design language* of those menus.

Built and daily-driven on a Galaxy S23 Ultra. Sideloaded APK, not on the Play Store — this is a passion project.

> **Fan project.** Not affiliated with, endorsed by, or associated with ATLUS or SEGA. See [LICENSE](LICENSE).

---

## Install (just want to use it)

1. Download **[ArcanaLauncher-v15.14.apk](ArcanaLauncher-v15.14.apk)** (click the file above, then the download button).
2. Copy it to your phone and open it. Android will ask you to allow installing from this source — say yes.
3. Tap the home button and pick **Arcana Launcher** as your home app.

For the full experience, grant it notification access (for dots and the now-playing widget) in Android settings when it prompts.

---

## Three themes ("Hours")

Swap the whole look instantly from one menu — your apps, categories, and layout carry across.

- **Blue Hour (P3)** — electric blue and cyan, sharp diagonal slashes, a layered "electric slash" selection, and a menu that slides in with overshoot when you come home.
- **Yellow Hour (P4)** — Midnight-Channel gold, rounded bubbly shapes, and a rainbow motif on the category underlines.
- **Red Hour (P5)** — ransom-note lettering (every letter a different size and tilt, some scissored into red-and-black cut-out boxes), jagged torn-paper edges, star bursts behind selections, and halftone comic dots pooling in the corners.

## Features

- **CRT shader** overlay — subtle scanlines, vignette, and a slow rolling scan bar. Toggleable, tinted per theme.
- **Dark Hour** — from midnight to 1 AM the whole UI shifts to a sickly green (with a preview toggle).
- Big condensed-italic **clock** with day + date on themed ribbon tags, plus a time-of-day phase readout.
- Live **weather** line and next-alarm display.
- **Now-playing widget** — scrolling track title, animated EQ bars, live progress, prev/play/next, album art tinted to match the theme.
- **The Arcana Card** — a face-down tarot card in the corner: hold to open your payment app, double-tap for flashlight, swipe up to jump to your last app. Pick any payment app and swap in your own card-face image.
- **Category menu** — fully editable categories with bilingual English + katakana labels (auto-generated), drag-to-highlight navigation with haptics, and a pinned system-settings entry.
- **App drawer** — type-to-search ranked by launch frequency, an iOS-style **magnify A–Z fast-scroll rail** (letters bulge out around your finger so you always see where you are), hide apps, and long-press deep shortcuts.
- 5-slot **dock**, each remappable, with custom icons auto-desaturated to match.
- Notification dots and a "peek" popup, swipe-down for recents, full **backup/restore** to a file, and wallpaper safe-zone templates.

---

## Build it yourself

This uses a deliberately minimal, no-Gradle toolchain. You need `aapt`, `apksigner`, `zipalign`, a dexer (`dalvik-exchange` or `dx`), a JDK, and an `android.jar` for API 34.

```bash
./build.sh
```

That runs: aapt (resources) → javac → dex → aapt add → zipalign → apksigner, and drops a signed APK in `build/`.

**Note on the toolchain:** because it dexes with a legacy dexer, the code avoids Java 8 lambdas, method references, and default interface methods — callbacks are plain anonymous inner classes. The `build.sh` script expects `android.jar` at `/opt/android-jars/android-34/android.jar`; edit the `AJ` variable at the top if yours lives elsewhere.

**Keep your keystore.** The first build generates `debug.keystore`. To ship an update that installs *over* an existing copy, you must sign it with the **same** keystore — so back that file up somewhere safe. It's intentionally left out of this repo (see `.gitignore`).

---

## Credits & license

Code is MIT licensed — see [LICENSE](LICENSE). The bundled **Barlow Condensed** and **Playfair Display** fonts are under the SIL Open Font License 1.1 (see [assets/OFL.txt](assets/OFL.txt) and [assets/OFL-Playfair.txt](assets/OFL-Playfair.txt)). All UI art is hand-drawn in `Canvas`/`Paint`/`Path`; no Persona assets are included.
