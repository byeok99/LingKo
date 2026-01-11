package com.lingko.lingko;

import com.lingko.lingko.core.domain.speech.SpeechEvaluator;
import com.lingko.lingko.core.domain.speech.dto.AssessmentResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AzureSpeechTest {

    @Autowired
    private SpeechEvaluator speechEvaluator;

    @Test
    @DisplayName("Azure 발음 평가 기능 테스트")
    void testWithInterface() {
        String filePath = "src/test/resources/speech_1766933005782.wav";
        String referenceText = "오늘은 좋은 기분으로 하루를 시작했다.";

        AssessmentResult result = speechEvaluator.evaluate(filePath, referenceText);

        System.out.println(result.getAccuracyScore());

        assertThat(result.getAccuracyScore()).isNotNull();
    }
}
