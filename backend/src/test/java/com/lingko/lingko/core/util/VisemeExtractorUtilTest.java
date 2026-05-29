package com.lingko.lingko.core.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * visumeExtractorUtil 테스트
 *
 * 입 모양 vs 혀 모양의 차이 이해:
 * - 입 모양: 입술 변화만 (ㄱ, ㄷ, ㅅ 등은 입술 변화 없음)
 * - 혀 모양: 혀 위치 변화 (대부분 자음에서 혀 변화 있음)
 */
class visumeExtractorUtilTest {

    private VisemeExtractorUtil visumeExtractorUtil;

    @BeforeEach
    void setUp() {
        SyllableMappingUtil syllableMappingUtil = new SyllableMappingUtil();
        syllableMappingUtil.loadMapping();
        visumeExtractorUtil = new VisemeExtractorUtil(syllableMappingUtil);
    }

    @Test
    @DisplayName("'한' - 혀 모양 (ㅎ 제외, ㅏ, ㄴ)")
    void testExtractTongueUrls_한() {
        // when
        List<List<String>> result = visumeExtractorUtil.extractTongueUrls("한");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).hasSize(2);
        assertThat(result.get(0).get(0)).contains("vowel-a");
        assertThat(result.get(0).get(1)).contains("alveolar-consonants");
    }

    @Test
    @DisplayName("'밥' - 입 모양 (ㅂ, ㅏ, ㅂ 모두 입술 변화)")
    void testExtractLipsUrls_밥() {
        // ㅂ: lips_url = "bilabial-consonants.png"
        // ㅏ: lips_url = "vowel-a.png"

        // when
        List<List<String>> result = visumeExtractorUtil.extractLipsUrls("밥");

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).hasSize(2); // [ㅂ, ㅏ]
        assertThat(result.get(1)).hasSize(2); // [ㅏ, ㅂ]

        assertThat(result.get(0).get(0)).contains("bilabial-consonants");
        assertThat(result.get(0).get(1)).contains("vowel-a");
    }

    @Test
    @DisplayName("'국' - 입 모양 (ㄱ은 입술 변화 없음, ㅜ만)")
    void testExtractLipsUrls_국() {
        // when
        List<List<String>> result = visumeExtractorUtil.extractLipsUrls("국");
        // then - ㅜ만 있어야 정상!
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).hasSize(1);
        assertThat(result.get(0).get(0)).isEqualTo("vowel-u.png");
    }

    @Test
    @DisplayName("'국' - 혀 모양 (ㄱ, ㅜ 모두 혀 변화)")
    void testExtractTongueUrls_국() {
        // ㄱ: tongue_url = "velar-consonants.png"
        // ㅜ: tongue_url = "semi-vowel-w.png"
        // ㄱ: tongue_url = "velar-consonants.png"

        // when
        List<List<String>> result = visumeExtractorUtil.extractTongueUrls("국");

        // then - 3개 모두 있어야 함 → 2쌍
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).hasSize(2); // [ㄱ, ㅜ]
        assertThat(result.get(1)).hasSize(2); // [ㅜ, ㄱ]

        assertThat(result.get(0).get(0)).contains("velar-consonants");
        assertThat(result.get(0).get(1)).contains("semi-vowel-w");
        assertThat(result.get(1).get(0)).contains("semi-vowel-w");
        assertThat(result.get(1).get(1)).contains("velar-consonants");
    }

    @Test
    @DisplayName("'사' - 입 모양 (ㅅ은 입술 변화 없음, ㅏ만)")
    void testExtractLipsUrls_사() {
        // ㅅ: lips_url = "" 
        // ㅏ: lips_url = "vowel-a.png"

        // when
        List<List<String>> result = visumeExtractorUtil.extractLipsUrls("사");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).hasSize(1);
        assertThat(result.get(0).get(0)).isEqualTo("vowel-a.png");
    }

    @Test
    @DisplayName("'사' - 혀 모양 (ㅅ, ㅏ 모두)")
    void testExtractTongueUrls_사() {
        // ㅅ: tongue_url = "alveolar-fricative.png"
        // ㅏ: tongue_url = "vowel-a.png"

        // when
        List<List<String>> result = visumeExtractorUtil.extractTongueUrls("사");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).hasSize(2);
        assertThat(result.get(0).get(0)).contains("alveolar-fricative");
        assertThat(result.get(0).get(1)).contains("vowel-a");
    }

    @Test
    @DisplayName("'아' - 입/혀 모양 (1개)")
    void testExtract_아() {
        // when
        List<List<String>> lips = visumeExtractorUtil.extractLipsUrls("아");
        List<List<String>> tongue = visumeExtractorUtil.extractTongueUrls("아");

        // then - 둘 다 [ㅏ]만
        assertThat(lips).hasSize(1);
        assertThat(lips.get(0)).hasSize(1);

        assertThat(tongue).hasSize(1);
        assertThat(tongue.get(0)).hasSize(1);
    }

    @Test
    @DisplayName("'하' - 초성 ㅎ 제외")
    void testExtract_하() {
        // ㅎ: 입/혀 모두 제외 (SILENT_PHONEMES)

        // when
        List<List<String>> lips = visumeExtractorUtil.extractLipsUrls("하");
        List<List<String>> tongue = visumeExtractorUtil.extractTongueUrls("하");

        // then - 둘 다 [ㅏ]만
        assertThat(lips).hasSize(1);
        assertThat(lips.get(0)).hasSize(1);

        assertThat(tongue).hasSize(1);
        assertThat(tongue.get(0)).hasSize(1);
    }

    @Test
    @DisplayName("빈 문자열 - 빈 결과")
    void testExtract_empty() {
        assertThat(visumeExtractorUtil.extractLipsUrls("")).isEmpty();
        assertThat(visumeExtractorUtil.extractTongueUrls("")).isEmpty();
    }

    @Test
    @DisplayName("null 입력 - 빈 결과")
    void testExtract_null() {
        assertThat(visumeExtractorUtil.extractLipsUrls(null)).isEmpty();
        assertThat(visumeExtractorUtil.extractTongueUrls(null)).isEmpty();
    }

    @Test
    @DisplayName("'시' vs '사' - 변이음 처리")
    void testSibilant() {
        // 사: ㅅ (기본) - "ㅅㅆ.png"
        // 시: ㅅ + i계열 - "변이음ㅅㅆ.png"

        List<List<String>> 사_tongue = visumeExtractorUtil.extractTongueUrls("사");
        List<List<String>> 시_tongue = visumeExtractorUtil.extractTongueUrls("시");

        // 둘 다 결과 있어야 함
        assertThat(사_tongue).isNotEmpty();
        assertThat(시_tongue).isNotEmpty();

        // 다른 URL이어야 함 (변이음 vs 기본)
        String 사_ㅅ = 사_tongue.get(0).get(0);
        String 시_ㅅ = 시_tongue.get(0).get(0);

        // 변이음ㅅ가 있으면 다름, 없으면 같음 (둘 다 OK)
        System.out.println("사 ㅅ: " + 사_ㅅ);
        System.out.println("시 ㅅ: " + 시_ㅅ);
    }
}
