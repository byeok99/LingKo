package com.lingko.lingko.core.domain.user.service;

import com.lingko.lingko.api.user.dto.UserPreferencesResponse;
import com.lingko.lingko.api.user.dto.UserPreferencesUpdateRequest;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPreferencesService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserPreferencesResponse findPreferences(Long userId) {
        User user = findAuthenticatedUser(userId);

        return toResponse(user);
    }

    @Transactional
    public UserPreferencesResponse updatePreferences(Long userId, UserPreferencesUpdateRequest request) {
        User user = findAuthenticatedUser(userId);
        user.updateLearningPreferences(
                request.displayLanguage().trim(),
                request.nativeLanguage().trim(),
                request.targetLevel()
        );

        return toResponse(user);
    }

    private User findAuthenticatedUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Authenticated user not found"));
    }

    private UserPreferencesResponse toResponse(User user) {
        return new UserPreferencesResponse(
                user.getDisplayLanguage(),
                user.getNativeLanguage(),
                user.getTargetLevel()
        );
    }
}
