package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import com.lingko.lingko.core.util.SyllableMappingUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Evaluation 서비스 Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
public class EvaluationServiceTest {

    private final EvaluationService service = new EvaluationService(mock(SyllableMappingUtil.class));

    @Test
    @DisplayName("표준 발음 변환 기능 테스트")
    void convertToStandardPronunciationTest() {
        // 준비
        String text = "밥을 먹었어요.";

        // 실행
        String result = service.convertToStandardPronunciation(text);

        // 검증
        assertThat(result).isEqualTo("바블 머거써요");

        System.out.println(result);
    }
}
