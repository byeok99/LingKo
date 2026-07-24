package com.lingko.lingko.api.evaluation.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * HTTP 경계에서 사용하는 Pronunciation Prepare 요청·응답 구조를 정의한다.
 *
 * 영속 엔티티를 직접 노출하지 않고 전송 계약을 독립적으로 유지하기 위해 전용 DTO를 선택했다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PronunciationPrepareRequest {

    private static final int MAX_CUSTOM_TEXT_LENGTH = 100;

    @NotBlank(message = "source is required")
    private String source;

    private String text;

    private Long sentenceId;

    @AssertTrue(message = "text is required for CUSTOM prepare request")
    public boolean isCustomTextValid() {
        if (!isCustom()) {
            return true;
        }

        return text != null && !text.trim().isEmpty();
    }

    @AssertTrue(message = "text must be 100 characters or fewer")
    public boolean isCustomTextLengthValid() {
        if (!isCustom() || text == null) {
            return true;
        }

        return text.trim().length() <= MAX_CUSTOM_TEXT_LENGTH;
    }

    @AssertTrue(message = "sentenceId is required for RECOMMENDED prepare request")
    public boolean isRecommendedSentenceIdValid() {
        if (!isRecommended()) {
            return true;
        }

        return sentenceId != null;
    }

    public boolean isCustom() {
        return "CUSTOM".equalsIgnoreCase(source);
    }

    public boolean isRecommended() {
        return "RECOMMENDED".equalsIgnoreCase(source);
    }

    public String trimmedText() {
        return text == null ? null : text.trim();
    }
}
