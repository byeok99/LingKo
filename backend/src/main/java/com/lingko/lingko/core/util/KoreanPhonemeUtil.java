package com.lingko.lingko.core.util;

import java.util.*;

/**
 * 한국어 표준발음 변환 및 음소 추출 유틸리티
 *
 * 핵심 기능:
 * - 표준발음 변환 (연음화, 비음화, 경음화, 구개음화)
 * - 음소 추출 (초성, 중성, 종성)
 */
public class KoreanPhonemeUtil {

    // 초성, 중성, 종성
    private static final String[] CHOSUNG = {
            "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ",
            "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };

    private static final String[] JUNGSUNG = {
            "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ",
            "ㅙ", "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ"
    };

    private static final String[] JONGSUNG = {
            "", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ",
            "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ",
            "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };

    // 종성을 초성으로 변환 (연음화용)
    private static final Map<String, String> JONGSUNG_TO_CHOSUNG = new HashMap<>();
    static {
        JONGSUNG_TO_CHOSUNG.put("ㄱ", "ㄱ");
        JONGSUNG_TO_CHOSUNG.put("ㄲ", "ㄲ");
        JONGSUNG_TO_CHOSUNG.put("ㄳ", "ㄱ");
        JONGSUNG_TO_CHOSUNG.put("ㄴ", "ㄴ");
        JONGSUNG_TO_CHOSUNG.put("ㄵ", "ㄴ");
        JONGSUNG_TO_CHOSUNG.put("ㄶ", "ㄴ");
        JONGSUNG_TO_CHOSUNG.put("ㄷ", "ㄷ");
        JONGSUNG_TO_CHOSUNG.put("ㄹ", "ㄹ");
        JONGSUNG_TO_CHOSUNG.put("ㄺ", "ㄱ");
        JONGSUNG_TO_CHOSUNG.put("ㄻ", "ㅁ");
        JONGSUNG_TO_CHOSUNG.put("ㄼ", "ㄹ");
        JONGSUNG_TO_CHOSUNG.put("ㄽ", "ㄹ");
        JONGSUNG_TO_CHOSUNG.put("ㄾ", "ㄹ");
        JONGSUNG_TO_CHOSUNG.put("ㄿ", "ㅂ");
        JONGSUNG_TO_CHOSUNG.put("ㅀ", "ㄹ");
        JONGSUNG_TO_CHOSUNG.put("ㅁ", "ㅁ");
        JONGSUNG_TO_CHOSUNG.put("ㅂ", "ㅂ");
        JONGSUNG_TO_CHOSUNG.put("ㅄ", "ㅂ");
        JONGSUNG_TO_CHOSUNG.put("ㅅ", "ㅅ");
        JONGSUNG_TO_CHOSUNG.put("ㅆ", "ㅆ");
        JONGSUNG_TO_CHOSUNG.put("ㅇ", "ㅇ");
        JONGSUNG_TO_CHOSUNG.put("ㅈ", "ㅈ");
        JONGSUNG_TO_CHOSUNG.put("ㅊ", "ㅊ");
        JONGSUNG_TO_CHOSUNG.put("ㅋ", "ㅋ");
        JONGSUNG_TO_CHOSUNG.put("ㅌ", "ㅌ");
        JONGSUNG_TO_CHOSUNG.put("ㅍ", "ㅍ");
        JONGSUNG_TO_CHOSUNG.put("ㅎ", "ㅎ");
    }

    /**
     * 한글 글자 분해 결과
     */
    public static class HangulChar {
        String chosung;
        String jungsung;
        String jongsung;

        public HangulChar(String cho, String jung, String jong) {
            this.chosung = cho;
            this.jungsung = jung;
            this.jongsung = jong;
        }

        public char toChar() {
            int cho = Arrays.asList(CHOSUNG).indexOf(chosung);
            int jung = Arrays.asList(JUNGSUNG).indexOf(jungsung);
            int jong = Arrays.asList(JONGSUNG).indexOf(jongsung);

            if (cho == -1 || jung == -1 || jong == -1) return '?';

            return (char) (0xAC00 + cho * 588 + jung * 28 + jong);
        }

        // Getter 메서드 추가
        public String getChosung() { return chosung; }
        public String getJungsung() { return jungsung; }
        public String getJongsung() { return jongsung; }
    }

    /**
     * 한글 글자 분해
     */
    public static HangulChar decompose(char ch) {
        if (ch < 0xAC00 || ch > 0xD7A3) return null;

        int code = ch - 0xAC00;
        int cho = code / 588;
        int jung = (code % 588) / 28;
        int jong = code % 28;

        return new HangulChar(CHOSUNG[cho], JUNGSUNG[jung], JONGSUNG[jong]);
    }

    /**
     * 문자열을 음소(자음+모음) 리스트로 분리
     *
     * 예: "한" → ["ㅎ", "ㅏ", "ㄴ"]
     * 예: "밥이" → ["ㅂ", "ㅏ", "ㅂ", "ㅇ", "ㅣ"]
     *
     * @param text 입력 텍스트
     * @return 음소 리스트
     */
    public static List<String> toPhonemeList(String text) {
        List<String> phonemes = new ArrayList<>();

        for (char ch : text.toCharArray()) {
            HangulChar hc = decompose(ch);
            if (hc != null) {
                // 초성 추가 (ㅇ 제외)
                if (!hc.chosung.equals("ㅇ")) {
                    phonemes.add(hc.chosung);
                }

                // 중성 추가
                phonemes.add(hc.jungsung);

                // 종성 추가 (있을 경우)
                if (!hc.jongsung.isEmpty()) {
                    phonemes.add(hc.jongsung);
                }
            } else if (!Character.isWhitespace(ch)) {
                // 한글이 아닌 문자 (공백 제외)
                phonemes.add(String.valueOf(ch));
            }
        }

        return phonemes;
    }

    /**
     * 표준 발음으로 변환
     *
     * @param text 입력 텍스트
     * @return 표준 발음
     */
    public static String toPronunciation(String text) {
        List<HangulChar> chars = new ArrayList<>();
        List<Character> nonHangul = new ArrayList<>();
        List<Boolean> isHangul = new ArrayList<>();

        // 1단계: 모든 글자 분해
        for (char ch : text.toCharArray()) {
            HangulChar hc = decompose(ch);
            if (hc != null) {
                chars.add(hc);
                nonHangul.add(null);
                isHangul.add(true);
            } else {
                chars.add(null);
                nonHangul.add(ch);
                isHangul.add(false);
            }
        }

        // 2단계: 발음 규칙 적용
        for (int i = 0; i < chars.size(); i++) {
            if (chars.get(i) == null) continue;

            HangulChar current = chars.get(i);
            HangulChar next = (i + 1 < chars.size()) ? chars.get(i + 1) : null;

            // 구개음화
            if (!current.jongsung.isEmpty() && next != null) {
                if (current.jongsung.equals("ㄷ") && next.chosung.equals("ㅇ")
                        && next.jungsung.equals("ㅣ")) {
                    next.chosung = "ㅈ";
                    current.jongsung = "";
                } else if (current.jongsung.equals("ㅌ") && next.chosung.equals("ㅇ")
                        && next.jungsung.equals("ㅣ")) {
                    next.chosung = "ㅊ";
                    current.jongsung = "";
                }
            }

            // 연음화
            if (next != null && !current.jongsung.isEmpty() && next.chosung.equals("ㅇ")) {
                String transferSound = JONGSUNG_TO_CHOSUNG.get(current.jongsung);
                if (transferSound != null) {
                    next.chosung = transferSound;
                    current.jongsung = "";
                }
            }

            // 자음 앞 받침은 대표음으로 바꾼 뒤 비음화·경음화를 적용해야 한다.
            if (next != null && !next.chosung.equals("ㅇ")) {
                current.jongsung = toRepresentativeFinalSound(current.jongsung);
            }

            // 비음화
            if (next != null && !current.jongsung.isEmpty()) {
                if ((current.jongsung.equals("ㄱ") || current.jongsung.equals("ㄺ"))
                        && (next.chosung.equals("ㄴ") || next.chosung.equals("ㅁ"))) {
                    current.jongsung = "ㅇ";
                } else if (current.jongsung.equals("ㄷ")
                        && (next.chosung.equals("ㄴ") || next.chosung.equals("ㅁ"))) {
                    current.jongsung = "ㄴ";
                } else if ((current.jongsung.equals("ㅂ") || current.jongsung.equals("ㅄ") || current.jongsung.equals("ㄿ"))
                        && (next.chosung.equals("ㄴ") || next.chosung.equals("ㅁ"))) {
                    current.jongsung = "ㅁ";
                }

                // 유음화
                if (current.jongsung.equals("ㄴ") && next.chosung.equals("ㄹ")) {
                    current.jongsung = "ㄹ";
                }
                if (current.jongsung.equals("ㄹ") && next.chosung.equals("ㄴ")) {
                    next.chosung = "ㄹ";
                }
            }

            // 경음화
            if (next != null && !current.jongsung.isEmpty()) {
                if (current.jongsung.equals("ㄱ") || current.jongsung.equals("ㄷ")
                        || current.jongsung.equals("ㅂ") || current.jongsung.equals("ㅈ")) {
                    if (next.chosung.equals("ㄱ")) next.chosung = "ㄲ";
                    else if (next.chosung.equals("ㄷ")) next.chosung = "ㄸ";
                    else if (next.chosung.equals("ㅂ")) next.chosung = "ㅃ";
                    else if (next.chosung.equals("ㅅ")) next.chosung = "ㅆ";
                    else if (next.chosung.equals("ㅈ")) next.chosung = "ㅉ";
                }
            }

            // 격음화
            if (next != null && !current.jongsung.isEmpty()) {
                if (next.chosung.equals("ㅎ")) {
                    if (current.jongsung.equals("ㄱ")) {
                        next.chosung = "ㅋ";
                        current.jongsung = "";
                    } else if (current.jongsung.equals("ㄷ")) {
                        next.chosung = "ㅌ";
                        current.jongsung = "";
                    } else if (current.jongsung.equals("ㅂ")) {
                        next.chosung = "ㅍ";
                        current.jongsung = "";
                    } else if (current.jongsung.equals("ㅈ")) {
                        next.chosung = "ㅊ";
                        current.jongsung = "";
                    }
                }
            }
            if (current.jongsung.equals("ㅎ") && next != null) {
                if (next.chosung.equals("ㄱ")) {
                    next.chosung = "ㅋ";
                    current.jongsung = "";
                } else if (next.chosung.equals("ㄷ")) {
                    next.chosung = "ㅌ";
                    current.jongsung = "";
                } else if (next.chosung.equals("ㅈ")) {
                    next.chosung = "ㅊ";
                    current.jongsung = "";
                }
            }
        }

        // 종성 7음 규칙
        for (int i = 0; i < chars.size(); i++) {
            if (chars.get(i) == null) continue;
            HangulChar current = chars.get(i);

            if (!current.jongsung.isEmpty()) {
                switch (current.jongsung) {
                    case "ㄳ": current.jongsung = "ㄱ"; break;
                    case "ㄵ": current.jongsung = "ㄴ"; break;
                    case "ㄶ": current.jongsung = "ㄴ"; break;
                    case "ㄺ": current.jongsung = "ㄱ"; break;
                    case "ㄻ": current.jongsung = "ㅁ"; break;
                    case "ㄼ": current.jongsung = "ㄹ"; break;
                    case "ㄽ": current.jongsung = "ㄹ"; break;
                    case "ㄾ": current.jongsung = "ㄹ"; break;
                    case "ㄿ": current.jongsung = "ㅂ"; break;
                    case "ㅀ": current.jongsung = "ㄹ"; break;
                    case "ㅄ": current.jongsung = "ㅂ"; break;
                }
            }
        }

        // 3단계: 재조합
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < chars.size(); i++) {
            if (isHangul.get(i)) {
                result.append(chars.get(i).toChar());
            } else {
                result.append(nonHangul.get(i));
            }
        }

        return result.toString();
    }

    private static String toRepresentativeFinalSound(String finalSound) {
        return switch (finalSound) {
            case "ㄲ", "ㄳ", "ㄺ", "ㅋ" -> "ㄱ";
            case "ㄵ" -> "ㄴ";
            case "ㅅ", "ㅆ", "ㅈ", "ㅊ", "ㅌ" -> "ㄷ";
            case "ㄼ", "ㄽ", "ㄾ" -> "ㄹ";
            case "ㄻ" -> "ㅁ";
            case "ㅄ", "ㄿ", "ㅍ" -> "ㅂ";
            default -> finalSound;
        };
    }
}
