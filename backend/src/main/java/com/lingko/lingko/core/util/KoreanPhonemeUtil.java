package com.lingko.lingko.core.util;

import java.util.*;

/**
 * 한국어 표준 발음 규칙 변환 및 음소 추출 유틸리티다.
 *
 * 연음화·비음화·유음화·경음화·구개음화·ㅎ 관련 규칙과 대표 받침을 결정적으로 적용한다.
 * 형태소 사전이나 예외 발음 사전은 사용하지 않으므로 어휘별 예외까지 완전한 G2P를 보장하는 경계는 아니다.
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

        // 자음 뒤 ㅢ는 실제 발음의 [ㅣ]로 먼저 정규화해 이후 가이드도 같은 중성을 사용한다.
        for (HangulChar current : chars) {
            if (current != null && !"ㅇ".equals(current.chosung) && "ㅢ".equals(current.jungsung)) {
                current.jungsung = "ㅣ";
            }
        }

        // 모음으로 시작하는 다음 음절에는 구개음화·겹받침 분리·연음을 먼저 적용한다.
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

            if (next != null && !current.jongsung.isEmpty() && next.chosung.equals("ㅇ")) {
                applyLiaison(current, next);
                continue;
            }

            if (next == null) {
                current.jongsung = toRepresentativeFinalSound(current.jongsung);
                continue;
            }

            if (applyHieutRule(current, next)) {
                continue;
            }

            String originalFinal = current.jongsung;
            if ("ㄺ".equals(originalFinal) && "ㄱ".equals(next.chosung)) {
                // 읽고·맑게 계열은 ㄹ을 남기면서 뒤 ㄱ을 된소리로 발음한다.
                current.jongsung = "ㄹ";
                next.chosung = "ㄲ";
            } else if (isBalpSyllable(current) && !"ㅇ".equals(next.chosung)) {
                // 표준 발음의 어간 '밟-' 예외는 일반 ㄼ 대표음(ㄹ)과 달리 ㅂ으로 실현된다.
                current.jongsung = "ㅂ";
            } else {
                current.jongsung = toRepresentativeFinalSound(originalFinal);
            }

            // 폐쇄음 뒤 ㄹ은 ㄴ으로 바뀐 뒤 비음화되므로 국립·협력의 두 변화를 한 순서로 적용한다.
            if (isObstruentCoda(current.jongsung) && "ㄹ".equals(next.chosung)) {
                next.chosung = "ㄴ";
            }

            applyNasalization(current, next);
            applyLiquidAssimilation(current, next);
            if (isObstruentCoda(current.jongsung)) {
                next.chosung = tense(next.chosung);
            }
        }

        // 재조합
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

    private static void applyLiaison(HangulChar current, HangulChar next) {
        String finalSound = current.jongsung;
        if ("ㅎ".equals(finalSound)) {
            current.jongsung = "";
            return;
        }
        if ("ㄶ".equals(finalSound)) {
            current.jongsung = "";
            next.chosung = "ㄴ";
            return;
        }
        if ("ㅀ".equals(finalSound)) {
            current.jongsung = "";
            next.chosung = "ㄹ";
            return;
        }

        String first = firstClusterSound(finalSound);
        String second = secondClusterSound(finalSound);
        if (first != null && second != null) {
            current.jongsung = first;
            next.chosung = second;
            return;
        }

        String transferSound = JONGSUNG_TO_CHOSUNG.get(finalSound);
        if (transferSound != null) {
            next.chosung = transferSound;
            current.jongsung = "";
        }
    }

    private static boolean applyHieutRule(HangulChar current, HangulChar next) {
        if ("ㅎ".equals(next.chosung) && isAspiratableCoda(current.jongsung)) {
            next.chosung = aspirate(current.jongsung);
            current.jongsung = "";
            return true;
        }
        if ("ㅎ".equals(current.jongsung)) {
            if ("ㄴ".equals(next.chosung) || "ㅁ".equals(next.chosung)) {
                current.jongsung = next.chosung;
            } else if (isAspiratableOnset(next.chosung)) {
                next.chosung = aspirate(next.chosung);
                current.jongsung = "";
            }
            return true;
        }
        if ("ㄶ".equals(current.jongsung) || "ㅀ".equals(current.jongsung)) {
            String remainingCoda = "ㄶ".equals(current.jongsung) ? "ㄴ" : "ㄹ";
            if (isAspiratableOnset(next.chosung)) {
                next.chosung = aspirate(next.chosung);
                current.jongsung = remainingCoda;
                return true;
            }
            current.jongsung = remainingCoda;
            // ㅎ만 탈락한 뒤에는 싫네[실레]처럼 남은 ㄹ·ㄴ의 후속 동화를 계속 적용해야 한다.
            return false;
        }
        return false;
    }

    private static void applyNasalization(HangulChar current, HangulChar next) {
        if (!"ㄴ".equals(next.chosung) && !"ㅁ".equals(next.chosung)) {
            return;
        }
        current.jongsung = switch (current.jongsung) {
            case "ㄱ" -> "ㅇ";
            case "ㄷ" -> "ㄴ";
            case "ㅂ" -> "ㅁ";
            default -> current.jongsung;
        };
    }

    private static void applyLiquidAssimilation(HangulChar current, HangulChar next) {
        if ("ㄴ".equals(current.jongsung) && "ㄹ".equals(next.chosung)) {
            current.jongsung = "ㄹ";
        }
        if ("ㄹ".equals(current.jongsung) && "ㄴ".equals(next.chosung)) {
            next.chosung = "ㄹ";
        }
    }

    private static boolean isBalpSyllable(HangulChar current) {
        return "ㅂ".equals(current.chosung)
                && "ㅏ".equals(current.jungsung)
                && "ㄼ".equals(current.jongsung);
    }

    private static boolean isObstruentCoda(String finalSound) {
        return "ㄱ".equals(finalSound) || "ㄷ".equals(finalSound) || "ㅂ".equals(finalSound);
    }

    private static boolean isAspiratableCoda(String finalSound) {
        return "ㄱ".equals(finalSound) || "ㄷ".equals(finalSound)
                || "ㅂ".equals(finalSound) || "ㅈ".equals(finalSound);
    }

    private static boolean isAspiratableOnset(String onset) {
        return "ㄱ".equals(onset) || "ㄷ".equals(onset)
                || "ㅂ".equals(onset) || "ㅈ".equals(onset);
    }

    private static String aspirate(String sound) {
        return switch (sound) {
            case "ㄱ" -> "ㅋ";
            case "ㄷ" -> "ㅌ";
            case "ㅂ" -> "ㅍ";
            case "ㅈ" -> "ㅊ";
            default -> sound;
        };
    }

    private static String tense(String onset) {
        return switch (onset) {
            case "ㄱ" -> "ㄲ";
            case "ㄷ" -> "ㄸ";
            case "ㅂ" -> "ㅃ";
            case "ㅅ" -> "ㅆ";
            case "ㅈ" -> "ㅉ";
            default -> onset;
        };
    }

    private static String firstClusterSound(String finalSound) {
        return switch (finalSound) {
            case "ㄳ" -> "ㄱ";
            case "ㄵ", "ㄶ" -> "ㄴ";
            case "ㄺ", "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ" -> "ㄹ";
            case "ㅄ" -> "ㅂ";
            default -> null;
        };
    }

    private static String secondClusterSound(String finalSound) {
        return switch (finalSound) {
            case "ㄳ", "ㄽ", "ㅄ" -> "ㅆ";
            case "ㄵ" -> "ㅈ";
            case "ㄶ" -> "ㅎ";
            case "ㄺ" -> "ㄱ";
            case "ㄻ" -> "ㅁ";
            case "ㄼ" -> "ㅂ";
            case "ㄾ" -> "ㅌ";
            case "ㄿ" -> "ㅍ";
            case "ㅀ" -> "ㅎ";
            default -> null;
        };
    }

    private static String toRepresentativeFinalSound(String finalSound) {
        return switch (finalSound) {
            case "ㄲ", "ㄳ", "ㄺ", "ㅋ" -> "ㄱ";
            case "ㄵ", "ㄶ" -> "ㄴ";
            case "ㅅ", "ㅆ", "ㅈ", "ㅊ", "ㅌ", "ㅎ" -> "ㄷ";
            case "ㄼ", "ㄽ", "ㄾ", "ㅀ" -> "ㄹ";
            case "ㄻ" -> "ㅁ";
            case "ㅄ", "ㄿ", "ㅍ" -> "ㅂ";
            default -> finalSound;
        };
    }
}
