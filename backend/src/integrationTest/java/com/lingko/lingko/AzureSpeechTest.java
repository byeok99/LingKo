package com.lingko.lingko;

import com.lingko.lingko.core.config.AzureSettings;
import com.lingko.lingko.core.domain.evaluation.dto.AssessmentResult;
import com.lingko.lingko.core.domain.evaluation.service.SpeechEvaluator;
import com.lingko.lingko.infra.pronunciation.AzureSpeechEvaluator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = AzureSpeechTest.AzureEvaluatorTestConfig.class)
@Tag("external")
public class AzureSpeechTest {

    @Autowired
    private SpeechEvaluator speechEvaluator;

    @Autowired
    private AzureSettings azureSettings;

    @Test
    @DisplayName("Azure 발음 평가 기능 테스트")
    void testWithInterface() {
        String filePath = "src/test/resources/speech_1766933005782.wav";
        String referenceText = "오늘은 좋은 기분으로 하루를 시작했다.";

        assertThat(azureSettings.getSecretKey()).isNotBlank();
        assertThat(azureSettings.getRegion()).isNotBlank();

        AssessmentResult result = speechEvaluator.evaluate(filePath, referenceText);

        assertThat(result.getAccuracyScore()).isNotNull();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AzureSettings.class)
    @Import(AzureSpeechEvaluator.class)
    static class AzureEvaluatorTestConfig {
    }
}
