package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class EvaluationServiceTest {

    @Autowired
    private EvaluationService service;

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
