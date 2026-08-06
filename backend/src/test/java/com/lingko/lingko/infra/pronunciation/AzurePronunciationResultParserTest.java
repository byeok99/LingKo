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

    @Test
    @DisplayName("개수는 같아도 같은 위치의 단어가 다르면 전체를 버린다")
    void rejectsWhenAlignedCountButDifferentWord() {
        // Azure가 수사를 숫자로 정규화하는 등 개수는 맞고 표기만 달라지는 경우다.
        // 개수만 보고 통과시키면 '한'의 점수가 '1잔'의 점수로 바뀌어 사용자에게 나간다.
        String normalizedNumeral = """
                {"NBest":[{"Words":[
                  {"Word":"물","PronunciationAssessment":{"AccuracyScore":88}},
                  {"Word":"1","PronunciationAssessment":{"AccuracyScore":70}},
                  {"Word":"잔","PronunciationAssessment":{"AccuracyScore":81}},
                  {"Word":"주세요","PronunciationAssessment":{"AccuracyScore":77}}
                ]}]}
                """;

        assertThat(parser.parseReliableWordScores(normalizedNumeral, "물 한 잔 주세요")).isEmpty();
    }

    @Test
    @DisplayName("문장부호만 다른 것은 같은 단어로 본다")
    void ignoresPunctuationOnlyDifference() {
        // 표준 발음에는 마침표가 남고 Azure 응답에는 없는 경우가 흔하다. 이것까지 버리면
        // 정상적인 평가가 문장부호 때문에 단어 점수를 잃는다.
        String withoutPunctuation = """
                {"NBest":[{"Words":[
                  {"Word":"마싯게따","PronunciationAssessment":{"AccuracyScore":74}}
                ]}]}
                """;

        assertThat(parser.parseReliableWordScores(withoutPunctuation, "마싯게따."))
                .extracting("text")
                .containsExactly("마싯게따.");
    }
}
