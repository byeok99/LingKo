package com.lingko.lingko.api.evaluation.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PronunciationPrepareRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("CUSTOM prepare 요청은 trim 후 비어 있지 않은 text가 필요하다")
    void customPrepareRequiresText() {
        PronunciationPrepareRequest request = new PronunciationPrepareRequest("CUSTOM", "   ", null);

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("customTextValid"));
    }

    @Test
    @DisplayName("CUSTOM prepare text는 100자 이하여야 한다")
    void customPrepareLimitsTextLength() {
        PronunciationPrepareRequest request = new PronunciationPrepareRequest("CUSTOM", "가".repeat(101), null);

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("customTextLengthValid"));
    }

    @Test
    @DisplayName("RECOMMENDED prepare 요청은 sentenceId가 필요하다")
    void recommendedPrepareRequiresSentenceId() {
        PronunciationPrepareRequest request = new PronunciationPrepareRequest("RECOMMENDED", null, null);

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("recommendedSentenceIdValid"));
    }
}
