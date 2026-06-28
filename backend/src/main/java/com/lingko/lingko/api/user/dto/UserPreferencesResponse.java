package com.lingko.lingko.api.user.dto;

import com.lingko.lingko.core.domain.user.entity.User;

public record UserPreferencesResponse(
        String displayLanguage,
        String nativeLanguage,
        User.LearningLevel targetLevel
) {
}
