package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.core.config.GuideGenerationJobSettings;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영에서 가이드 API를 켤 때 내부 Secret과 비용 제한이 반드시 유효하도록 설정 계약을 검증한다.
 */
class GuideGenerationJobSettingsTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void disabledApiDoesNotRequireInternalToken() {
        // Given
        GuideGenerationJobSettings settings = new GuideGenerationJobSettings();

        // When & Then
        assertThat(validator.validate(settings)).isEmpty();
    }

    @Test
    void enabledApiRequiresAtLeastThirtyTwoCharacterInternalToken() {
        // Given: 짧은 token으로 API를 켠다
        GuideGenerationJobSettings settings = new GuideGenerationJobSettings();
        settings.setApiEnabled(true);
        settings.setInternalToken("short-token");

        // When & Then
        assertThat(validator.validate(settings))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("secureWhenEnabled"));

        // When: 32자 token으로 바꾼다
        settings.setInternalToken("0123456789abcdef0123456789abcdef");
        // Then
        assertThat(validator.validate(settings)).isEmpty();
    }
}
