package com.lingko.lingko.api.evaluation.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 상태값을 enum으로 바꾼 뒤에도 클라이언트가 보는 JSON 문자열이 그대로인지 보장한다.
 *
 * 앱은 서버와 동시에 배포되지 않으므로 wire format이 바뀌면 기존 사용자의 결과 화면이 즉시 깨진다.
 * 타입 안전성을 얻는 과정에서 계약이 바뀌지 않았다는 점을 회귀 테스트로 고정한다.
 */
class ScoreStatusSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("점수 상태 enum은 기존 대문자 문자열 그대로 직렬화된다")
    void serializesScoreStatusAsUppercaseWireValue() throws Exception {
        PracticeWordResultResponse word = PracticeWordResultResponse.builder()
                .position(0)
                .text("안녕하세요")
                .score(91)
                .scoreStatus(ScoreStatus.AVAILABLE)
                .syllables(List.of())
                .build();

        assertThat(objectMapper.writeValueAsString(word))
                .contains("\"scoreStatus\":\"AVAILABLE\"");

        assertThat(objectMapper.writeValueAsString(
                PracticeWordResultResponse.builder()
                        .scoreStatus(ScoreStatus.UNAVAILABLE)
                        .syllables(List.of())
                        .build()
        )).contains("\"scoreStatus\":\"UNAVAILABLE\"");
    }

    @Test
    @DisplayName("가이드 상태는 점수 상태와 다른 값 집합을 유지한다")
    void serializesGuideStatusAsItsOwnVocabulary() throws Exception {
        GuideCharacterResponse character = GuideCharacterResponse.builder()
                .position(0)
                .text("안")
                .scoreStatus(ScoreStatus.UNAVAILABLE)
                .guideStatus(GuideStatus.AVAILABLE)
                .build();

        String json = objectMapper.writeValueAsString(character);

        // 점수를 신뢰할 수 없어도 가이드는 제공되는 조합이 정상 동작이다.
        assertThat(json).contains("\"scoreStatus\":\"UNAVAILABLE\"");
        assertThat(json).contains("\"guideStatus\":\"AVAILABLE\"");
    }

    @Test
    @DisplayName("점수 유무에서 상태를 유도하는 규칙은 한곳에서만 정의된다")
    void derivesStatusFromNullableScore() {
        assertThat(ScoreStatus.ofNullableScore(null)).isEqualTo(ScoreStatus.UNAVAILABLE);
        assertThat(ScoreStatus.ofNullableScore(0)).isEqualTo(ScoreStatus.AVAILABLE);
        assertThat(ScoreStatus.AVAILABLE.isAvailable()).isTrue();
        assertThat(ScoreStatus.UNAVAILABLE.isAvailable()).isFalse();
    }
}
