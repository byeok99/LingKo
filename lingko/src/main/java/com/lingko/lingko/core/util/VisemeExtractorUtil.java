package com.lingko.lingko.core.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 입/혀 모양 이미지 URL 추출 서비스
 *
 * 목적: 발음에 영향을 주는 자음/모음의 이미지 URL 추출
 * 결과:
 * - 2개: [IMG1, IMG2]
 * - 3개: [IMG1, IMG2], [IMG2, IMG3]
 * - 1개: [IMG1]
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisemeExtractorUtil {

    private final SyllableMappingUtil syllableMappingUtil;

    // 발음에 영향 없는 자음 (제외)
    private static final Set<String> SILENT_PHONEMES = Set.of("ㅎ", "ㅇ");

    // 변이음 구분용 (ㅅ, ㅆ + i계열 모음)
    private static final Set<String> PALATAL_VOWELS = Set.of(
            "ㅣ", "ㅑ", "ㅒ", "ㅕ", "ㅖ", "ㅛ", "ㅠ"
    );

    /**
     * 혀 모양 이미지 URL 쌍 추출
     *
     * @param syllable 한 글자 (예: "한")
     * @return [[ㅏ_url, ㄴ_url]] 또는 [[ㅏ_url]]
     */
    public List<List<String>> extractTongueUrls(String syllable) {
        List<String> phonemeUrls = extractPhonemeUrls(syllable, true);
        return pairUrls(phonemeUrls);
    }

    /**
     * 입 모양 이미지 URL 쌍 추출
     *
     * @param syllable 한 글자 (예: "밥")
     * @return [[ㅂ_url, ㅏ_url], [ㅏ_url, ㅂ_url]]
     */
    public List<List<String>> extractLipsUrls(String syllable) {
        List<String> phonemeUrls = extractPhonemeUrls(syllable, false);
        return pairUrls(phonemeUrls);
    }

    /**
     * 음소별 이미지 URL 추출
     *
     * @param syllable 한 글자
     * @param isTongue true: 혀, false: 입
     * @return [IMG1_url, IMG2_url, IMG3_url]
     */
    private List<String> extractPhonemeUrls(String syllable, boolean isTongue) {
        List<String> urls = new ArrayList<>();

        if (syllable == null || syllable.isEmpty()) {
            return urls;
        }

        char ch = syllable.charAt(0);
        KoreanPhonemeUtil.HangulChar hc = KoreanPhonemeUtil.decompose(ch);

        if (hc == null) {
            return urls;
        }

        String chosung = hc.getChosung();
        String jungsung = hc.getJungsung();
        String jongsung = hc.getJongsung();

        // 초성 (발음 영향 있는 것만)
        if (!SILENT_PHONEMES.contains(chosung)) {
            String url = getImageUrl(chosung, jungsung, isTongue);
            if (url != null && !url.isEmpty()) {
                urls.add(url);
            }
        }

        // 중성 (항상 추가)
        String url = getImageUrl(jungsung, null, isTongue);
        if (url != null && !url.isEmpty()) {
            urls.add(url);
        }

        // 종성 (발음 영향 있는 것만)
        if (!jongsung.isEmpty() && !SILENT_PHONEMES.contains(jongsung)) {
            url = getImageUrl(jongsung, null, isTongue);
            if (url != null && !url.isEmpty()) {
                urls.add(url);
            }
        }

        return urls;
    }

    /**
     * URL을 쌍으로 만들기
     *
     * - 1개: [[IMG1]]
     * - 2개: [[IMG1, IMG2]]
     * - 3개: [[IMG1, IMG2], [IMG2, IMG3]]
     *
     * @param urls 음소 URL 리스트
     * @return URL 쌍 리스트
     */
    private List<List<String>> pairUrls(List<String> urls) {
        List<List<String>> pairs = new ArrayList<>();

        if (urls.isEmpty()) {
            return pairs;
        }

        if (urls.size() == 1) {
            // 1개: [IMG1]
            pairs.add(List.of(urls.get(0)));
        } else {
            // 2개 이상: 인접한 쌍으로
            for (int i = 0; i < urls.size() - 1; i++) {
                pairs.add(List.of(urls.get(i), urls.get(i + 1)));
            }
        }

        return pairs;
    }

    /**
     * 이미지 URL 가져오기
     *
     * @param phoneme 자음 또는 모음
     * @param nextVowel 다음 모음 (변이음 판단용, 없으면 null)
     * @param isTongue true: 혀, false: 입
     * @return 이미지 URL
     */
    private String getImageUrl(String phoneme, String nextVowel, boolean isTongue) {
        if (phoneme == null || phoneme.isEmpty()) {
            return null;
        }

        // 변이음 처리 (ㅅ, ㅆ + i계열 모음)
        String key = phoneme;
        if ((phoneme.equals("ㅅ") || phoneme.equals("ㅆ")) &&
                nextVowel != null && PALATAL_VOWELS.contains(nextVowel)) {
            key = "변이음" + phoneme;
        }

        // 매핑 조회
        SyllableMappingUtil.SyllableMapping mapping = syllableMappingUtil.getMapping(key);

        if (mapping == null) {
            log.warn("매핑을 찾을 수 없음: {}", key);
            return null;
        }

        // URL 반환
        String url = isTongue ? mapping.getTongueUrl() : mapping.getLipsUrl();

        // 변이음 URL이 없으면 기본 자음으로 폴백
        if ((url == null || url.isEmpty()) && !key.equals(phoneme)) {
            mapping = syllableMappingUtil.getMapping(phoneme);
            url = mapping != null ?
                    (isTongue ? mapping.getTongueUrl() : mapping.getLipsUrl()) :
                    null;
        }

        return url;
    }
}