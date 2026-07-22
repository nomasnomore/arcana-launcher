package com.aaron.bluehour;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Rough English → katakana transliterator for auto-subtitling new
 * categories. Heuristic (English spelling isn't phonetic), so a manually
 * typed subtitle always wins — this is the fallback.
 */
public final class Kana {

    private static final Map<String, String[]> ROWS = new HashMap<String, String[]>();
    private static final Map<Character, String> TAIL = new HashMap<Character, String>();
    // real loanword readings for likely category names — checked first
    private static final Map<String, String> DICT = new HashMap<String, String>();

    static {
        // consonant row → { a, i, u, e, o }
        ROWS.put("", new String[]{"ア", "イ", "ウ", "エ", "オ"});
        ROWS.put("k", new String[]{"カ", "キ", "ク", "ケ", "コ"});
        ROWS.put("g", new String[]{"ガ", "ギ", "グ", "ゲ", "ゴ"});
        ROWS.put("s", new String[]{"サ", "シ", "ス", "セ", "ソ"});
        ROWS.put("z", new String[]{"ザ", "ジ", "ズ", "ゼ", "ゾ"});
        ROWS.put("t", new String[]{"タ", "チ", "ツ", "テ", "ト"});
        ROWS.put("d", new String[]{"ダ", "ジ", "ズ", "デ", "ド"});
        ROWS.put("n", new String[]{"ナ", "ニ", "ヌ", "ネ", "ノ"});
        ROWS.put("h", new String[]{"ハ", "ヒ", "フ", "ヘ", "ホ"});
        ROWS.put("b", new String[]{"バ", "ビ", "ブ", "ベ", "ボ"});
        ROWS.put("p", new String[]{"パ", "ピ", "プ", "ペ", "ポ"});
        ROWS.put("m", new String[]{"マ", "ミ", "ム", "メ", "モ"});
        ROWS.put("y", new String[]{"ヤ", "イ", "ユ", "イェ", "ヨ"});
        ROWS.put("r", new String[]{"ラ", "リ", "ル", "レ", "ロ"});
        ROWS.put("l", new String[]{"ラ", "リ", "ル", "レ", "ロ"});
        ROWS.put("w", new String[]{"ワ", "ウィ", "ウ", "ウェ", "ウォ"});
        ROWS.put("f", new String[]{"ファ", "フィ", "フ", "フェ", "フォ"});
        ROWS.put("v", new String[]{"ヴァ", "ヴィ", "ヴ", "ヴェ", "ヴォ"});
        ROWS.put("j", new String[]{"ジャ", "ジ", "ジュ", "ジェ", "ジョ"});
        ROWS.put("c", new String[]{"カ", "シ", "ク", "セ", "コ"});
        ROWS.put("q", new String[]{"クァ", "クィ", "ク", "クェ", "クォ"});
        ROWS.put("x", new String[]{"クサ", "クシ", "クス", "クセ", "クソ"});
        ROWS.put("ch", new String[]{"チャ", "チ", "チュ", "チェ", "チョ"});
        ROWS.put("sh", new String[]{"シャ", "シ", "シュ", "シェ", "ショ"});
        ROWS.put("th", new String[]{"サ", "シ", "ス", "セ", "ソ"});
        ROWS.put("ph", new String[]{"ファ", "フィ", "フ", "フェ", "フォ"});
        ROWS.put("ts", new String[]{"ツァ", "ツィ", "ツ", "ツェ", "ツォ"});

        // word/cluster-final consonants
        TAIL.put(Character.valueOf('b'), "ブ");
        TAIL.put(Character.valueOf('c'), "ク");
        TAIL.put(Character.valueOf('d'), "ド");
        TAIL.put(Character.valueOf('f'), "フ");
        TAIL.put(Character.valueOf('g'), "グ");
        TAIL.put(Character.valueOf('j'), "ジ");
        TAIL.put(Character.valueOf('k'), "ク");
        TAIL.put(Character.valueOf('l'), "ル");
        TAIL.put(Character.valueOf('m'), "ム");
        TAIL.put(Character.valueOf('n'), "ン");
        TAIL.put(Character.valueOf('p'), "プ");
        TAIL.put(Character.valueOf('q'), "ク");
        TAIL.put(Character.valueOf('r'), "ル");
        TAIL.put(Character.valueOf('s'), "ス");
        TAIL.put(Character.valueOf('t'), "ト");
        TAIL.put(Character.valueOf('v'), "ブ");
        TAIL.put(Character.valueOf('x'), "クス");
        TAIL.put(Character.valueOf('y'), "イ");
        TAIL.put(Character.valueOf('z'), "ズ");

        DICT.put("work", "ワーク");
        DICT.put("music", "ミュージック");
        DICT.put("school", "スクール");
        DICT.put("finance", "ファイナンス");
        DICT.put("money", "マネー");
        DICT.put("banking", "バンキング");
        DICT.put("photos", "フォト");
        DICT.put("photo", "フォト");
        DICT.put("camera", "カメラ");
        DICT.put("shopping", "ショッピング");
        DICT.put("shop", "ショップ");
        DICT.put("reading", "リーディング");
        DICT.put("books", "ブックス");
        DICT.put("book", "ブック");
        DICT.put("news", "ニュース");
        DICT.put("sports", "スポーツ");
        DICT.put("sport", "スポーツ");
        DICT.put("fitness", "フィットネス");
        DICT.put("health", "ヘルス");
        DICT.put("travel", "トラベル");
        DICT.put("food", "フード");
        DICT.put("crypto", "クリプト");
        DICT.put("dev", "デブ");
        DICT.put("code", "コード");
        DICT.put("coding", "コーディング");
        DICT.put("video", "ビデオ");
        DICT.put("movies", "ムービー");
        DICT.put("movie", "ムービー");
        DICT.put("anime", "アニメ");
        DICT.put("manga", "マンガ");
        DICT.put("art", "アート");
        DICT.put("design", "デザイン");
        DICT.put("study", "スタディ");
        DICT.put("stuff", "スタッフ");
        DICT.put("utilities", "ユーティリティ");
        DICT.put("streaming", "ストリーミング");
        DICT.put("social", "ソーシャル");
        DICT.put("games", "ゲーム");
        DICT.put("game", "ゲーム");
        DICT.put("gaming", "ゲーミング");
        DICT.put("emulators", "エミュレータ");
        DICT.put("weather", "ウェザー");
        DICT.put("car", "カー");
        DICT.put("smart", "スマート");
        DICT.put("home", "ホーム");
        DICT.put("family", "ファミリー");
        // --- expanded batch: common category / app words ---
        DICT.put("tools", "ツール");
        DICT.put("tool", "ツール");
        DICT.put("utility", "ユーティリティ");
        DICT.put("media", "メディア");
        DICT.put("financial", "フィナンシャル");
        DICT.put("finances", "ファイナンス");
        DICT.put("banks", "バンク");
        DICT.put("bank", "バンク");
        DICT.put("wallet", "ウォレット");
        DICT.put("pay", "ペイ");
        DICT.put("payments", "ペイメント");
        DICT.put("level", "レベル");
        DICT.put("levels", "レベル");
        DICT.put("solo", "ソロ");
        DICT.put("links", "リンクス");
        DICT.put("link", "リンク");
        DICT.put("lifestyle", "ライフスタイル");
        DICT.put("life", "ライフ");
        DICT.put("style", "スタイル");
        DICT.put("maps", "マップ");
        DICT.put("map", "マップ");
        DICT.put("navigation", "ナビ");
        DICT.put("productivity", "プロダクティビティ");
        DICT.put("work", "ワーク");
        DICT.put("business", "ビジネス");
        DICT.put("office", "オフィス");
        DICT.put("mail", "メール");
        DICT.put("email", "イーメール");
        DICT.put("messages", "メッセージ");
        DICT.put("message", "メッセージ");
        DICT.put("chat", "チャット");
        DICT.put("phone", "フォン");
        DICT.put("calls", "コール");
        DICT.put("contacts", "コンタクト");
        DICT.put("browser", "ブラウザ");
        DICT.put("web", "ウェブ");
        DICT.put("internet", "インターネット");
        DICT.put("search", "サーチ");
        DICT.put("cloud", "クラウド");
        DICT.put("files", "ファイル");
        DICT.put("file", "ファイル");
        DICT.put("photography", "フォトグラフィ");
        DICT.put("gallery", "ギャラリー");
        DICT.put("pictures", "ピクチャー");
        DICT.put("images", "イメージ");
        DICT.put("videos", "ビデオ");
        DICT.put("streaming", "ストリーミング");
        DICT.put("stream", "ストリーム");
        DICT.put("podcasts", "ポッドキャスト");
        DICT.put("podcast", "ポッドキャスト");
        DICT.put("radio", "ラジオ");
        DICT.put("audio", "オーディオ");
        DICT.put("sound", "サウンド");
        DICT.put("entertainment", "エンタメ");
        DICT.put("movies", "ムービー");
        DICT.put("shows", "ショー");
        DICT.put("tv", "テレビ");
        DICT.put("comics", "コミック");
        DICT.put("comic", "コミック");
        DICT.put("emulator", "エミュレータ");
        DICT.put("retro", "レトロ");
        DICT.put("arcade", "アーケード");
        DICT.put("puzzle", "パズル");
        DICT.put("board", "ボード");
        DICT.put("cards", "カード");
        DICT.put("casino", "カジノ");
        DICT.put("racing", "レーシング");
        DICT.put("shooter", "シューター");
        DICT.put("adventure", "アドベンチャー");
        DICT.put("action", "アクション");
        DICT.put("strategy", "ストラテジー");
        DICT.put("fantasy", "ファンタジー");
        DICT.put("horror", "ホラー");
        DICT.put("fitness", "フィットネス");
        DICT.put("workout", "ワークアウト");
        DICT.put("running", "ランニング");
        DICT.put("training", "トレーニング");
        DICT.put("wellness", "ウェルネス");
        DICT.put("medical", "メディカル");
        DICT.put("meditation", "メディテーション");
        DICT.put("recipes", "レシピ");
        DICT.put("recipe", "レシピ");
        DICT.put("cooking", "クッキング");
        DICT.put("coffee", "コーヒー");
        DICT.put("drinks", "ドリンク");
        DICT.put("delivery", "デリバリー");
        DICT.put("dating", "デーティング");
        DICT.put("messaging", "メッセージング");
        DICT.put("calls", "コール");
        DICT.put("calendar", "カレンダー");
        DICT.put("clock", "クロック");
        DICT.put("alarm", "アラーム");
        DICT.put("notes", "ノート");
        DICT.put("note", "ノート");
        DICT.put("reminders", "リマインダー");
        DICT.put("tasks", "タスク");
        DICT.put("todo", "トゥードゥー");
        DICT.put("wallpapers", "ウォールペーパー");
        DICT.put("wallpaper", "ウォールペーパー");
        DICT.put("themes", "テーマ");
        DICT.put("theme", "テーマ");
        DICT.put("icons", "アイコン");
        DICT.put("launcher", "ランチャー");
        DICT.put("settings", "セッティング");
        DICT.put("system", "システム");
        DICT.put("security", "セキュリティ");
        DICT.put("vpn", "ブイピーエヌ");
        DICT.put("password", "パスワード");
        DICT.put("privacy", "プライバシー");
        DICT.put("battery", "バッテリー");
        DICT.put("storage", "ストレージ");
        DICT.put("backup", "バックアップ");
        DICT.put("hidden", "ヒドゥン");
        DICT.put("secret", "シークレット");
        DICT.put("favorites", "フェイバリット");
        DICT.put("favourites", "フェイバリット");
        DICT.put("essentials", "エッセンシャル");
        DICT.put("misc", "ミスク");
        DICT.put("other", "アザー");
        DICT.put("others", "アザーズ");
        DICT.put("apps", "アプリ");
        DICT.put("app", "アプリ");
        DICT.put("google", "グーグル");
        DICT.put("samsung", "サムスン");
        DICT.put("crypto", "クリプト");
        DICT.put("trading", "トレーディング");
        DICT.put("stocks", "ストック");
        DICT.put("investing", "インベスティング");
        DICT.put("weather", "ウェザー");
        DICT.put("outdoors", "アウトドア");
        DICT.put("nature", "ネイチャー");
        DICT.put("hobbies", "ホビー");
        DICT.put("creative", "クリエイティブ");
        DICT.put("productivity", "プロダクティビティ");
        DICT.put("education", "エデュケーション");
        DICT.put("learning", "ラーニング");
        DICT.put("language", "ランゲージ");
        DICT.put("kids", "キッズ");
    }

    private Kana() {}

    private static boolean isVowel(char c) {
        return c == 'a' || c == 'i' || c == 'u' || c == 'e' || c == 'o';
    }

    private static int vowelIndex(char c) {
        switch (c) {
            case 'a': return 0;
            case 'i': return 1;
            case 'u': return 2;
            case 'e': return 3;
            default:  return 4;
        }
    }

    public static String toKatakana(String english) {
        String s = english.toLowerCase(Locale.US).trim();
        StringBuilder out = new StringBuilder();

        for (String word : s.split("[^a-z]+")) {
            if (word.length() == 0) continue;
            if (out.length() > 0) out.append('・');

            String known = DICT.get(word);
            if (known != null) {
                out.append(known);
                continue;
            }

            // silent final e ("game", "phone")
            if (word.length() > 2 && word.endsWith("e")
                    && !isVowel(word.charAt(word.length() - 2))) {
                word = word.substring(0, word.length() - 1);
            }

            int i = 0;
            int n = word.length();
            while (i < n) {
                char c = word.charAt(i);

                // collapse doubled letters
                if (i + 1 < n && word.charAt(i + 1) == c) {
                    word = word.substring(0, i) + word.substring(i + 1);
                    n--;
                    continue;
                }

                if (isVowel(c)) {
                    // common vowel pairs → long sounds
                    if (i + 1 < n && isVowel(word.charAt(i + 1))) {
                        String pair = word.substring(i, i + 2);
                        String mapped = null;
                        if (pair.equals("ee") || pair.equals("ea") || pair.equals("ie")) mapped = "イー";
                        else if (pair.equals("oo")) mapped = "ウー";
                        else if (pair.equals("ai") || pair.equals("ei") || pair.equals("ay")) mapped = "エイ";
                        else if (pair.equals("oa") || pair.equals("au")) mapped = "オー";
                        else if (pair.equals("ou")) mapped = "アウ";
                        if (mapped != null) {
                            out.append(mapped);
                            i += 2;
                            continue;
                        }
                    }
                    out.append(ROWS.get("")[vowelIndex(c)]);
                    i++;
                    continue;
                }

                // consonant (maybe digraph)
                String cons = String.valueOf(c);
                if (i + 1 < n) {
                    String two = word.substring(i, i + 2);
                    if (ROWS.containsKey(two)) {
                        cons = two;
                    }
                }
                int consLen = cons.length();

                if (i + consLen < n && isVowel(word.charAt(i + consLen))) {
                    // consonant + vowel → one kana
                    char v = word.charAt(i + consLen);
                    String[] row = ROWS.get(cons);
                    if (row == null) row = ROWS.get("");
                    out.append(row[vowelIndex(v)]);
                    i += consLen + 1;
                } else {
                    // bare consonant → tail form
                    String tail = TAIL.get(Character.valueOf(cons.charAt(consLen - 1)));
                    if (tail != null) out.append(tail);
                    i += consLen;
                }
            }
        }
        return out.toString();
    }
}
