package com.lingko.lingko.infra.pronunciation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Azure detailed JSON의 단어 배열이 LingKo 기준 문장과 일치할 때만 점수를 신뢰함을 검증한다.
 */
class AzurePronunciationResultParserTest {

    private final AzurePronunciationResultParser parser =
            new AzurePronunciationResultParser(new ObjectMapper());

    @Test
    @DisplayName("Azure Words가 기준 공백 단위와 일치하면 위치·텍스트·점수를 매핑한다")
    void parsesReliableKoreanWordScores() {
        String json = """
                {
                  "NBest": [{
                    "Words": [
                      {"Word":"김치찌개","PronunciationAssessment":{"AccuracyScore":82.4}},
                      {"Word":"하나","PronunciationAssessment":{"AccuracyScore":91.0}},
                      {"Word":"주세요","PronunciationAssessment":{"AccuracyScore":76.6}}
                    ]
                  }]
                }
                """;

        var scores = parser.parseReliableWordScores(json, "김치찌개 하나 주세요");

        assertThat(scores).extracting("position").containsExactly(0, 1, 2);
        assertThat(scores).extracting("text").containsExactly("김치찌개", "하나", "주세요");
        assertThat(scores).extracting("accuracyScore").containsExactly(82.4, 91.0, 76.6);
    }

    @Test
    @DisplayName("Azure token 개수·텍스트·점수가 다르면 단어 점수 전체를 사용하지 않는다")
    void rejectsUnreliableWordAlignment() {
        String mismatchedText = """
                {"NBest":[{"Words":[
                  {"Word":"김치","PronunciationAssessment":{"AccuracyScore":82}},
                  {"Word":"찌개","PronunciationAssessment":{"AccuracyScore":80}},
                  {"Word":"하나","PronunciationAssessment":{"AccuracyScore":91}},
                  {"Word":"주세요","PronunciationAssessment":{"AccuracyScore":77}}
                ]}]}
                """;
        String missingScore = """
                {"NBest":[{"Words":[
                  {"Word":"김치찌개","PronunciationAssessment":{}},
                  {"Word":"하나","PronunciationAssessment":{"AccuracyScore":91}},
                  {"Word":"주세요","PronunciationAssessment":{"AccuracyScore":77}}
                ]}]}
                """;

        assertThat(parser.parseReliableWordScores(mismatchedText, "김치찌개 하나 주세요")).isEmpty();
        assertThat(parser.parseReliableWordScores(missingScore, "김치찌개 하나 주세요")).isEmpty();
        assertThat(parser.parseReliableWordScores("not-json", "김치찌개 하나 주세요")).isEmpty();
    }
}
