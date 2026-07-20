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
