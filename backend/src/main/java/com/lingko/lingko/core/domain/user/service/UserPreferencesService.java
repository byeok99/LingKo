package com.lingko.lingko.core.domain.user.service;

import com.lingko.lingko.api.user.dto.UserPreferencesResponse;
import com.lingko.lingko.api.user.dto.UserPreferencesUpdateRequest;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User Preferences 업무 규칙을 조율한다.
 *
 * 컨트롤러와 외부 어댑터가 정책을 소유하지 않도록 도메인 서비스에 조율을 집중했다.
 */
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
        user.updateNativeLanguage(request.nativeLanguage().trim());

        return toResponse(user);
    }

    private User findAuthenticatedUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Authenticated user not found"));
    }

    private UserPreferencesResponse toResponse(User user) {
        return new UserPreferencesResponse(user.getNativeLanguage());
    }
}
