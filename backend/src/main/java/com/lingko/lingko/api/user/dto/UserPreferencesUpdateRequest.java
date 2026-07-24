package com.lingko.lingko.api.user.dto;

import com.lingko.lingko.core.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * HTTP 경계에서 사용하는 User Preferences Update 요청·응답 구조를 정의한다.
 *
 * 영속 엔티티를 직접 노출하지 않고 전송 계약을 독립적으로 유지하기 위해 전용 DTO를 선택했다.
 */
public record UserPreferencesUpdateRequest(
        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = "en|ko|ja")
        String displayLanguage,

        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = "en|ko|ja")
        String nativeLanguage,

        @NotNull
        User.LearningLevel targetLevel
) {
}
