package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import com.lingko.lingko.core.util.SyllableMappingUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class EvaluationServiceTest {

    private final EvaluationService service = new EvaluationService(mock(SyllableMappingUtil.class));

    @Test
    @DisplayName("표준 발음 변환 기능 테스트")
    void convertToStandardPronunciationTest() {
        //Given
        String text = "밥을 먹었어요.";

        //When
        String result = service.convertToStandardPronunciation(text);

        // Then
        assertThat(result).isEqualTo("바블 머거써요.");

        System.out.println(result);
    }
}
