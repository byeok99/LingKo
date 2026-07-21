package com.lingko.lingko.api.user.dto;

import com.lingko.lingko.core.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
