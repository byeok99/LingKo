package com.lingko.lingko.api.user;

import com.lingko.lingko.api.user.dto.UserPreferencesResponse;
import com.lingko.lingko.api.user.dto.UserPreferencesUpdateRequest;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.service.JwtTokenProvider;
import com.lingko.lingko.core.domain.user.service.UserPreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/preferences")
@RequiredArgsConstructor
public class UserPreferencesController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserPreferencesService preferencesService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    public UserPreferencesResponse getMyPreferences(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return preferencesService.findPreferences(resolveUserId(authorization));
    }

    @PatchMapping
    public UserPreferencesResponse updateMyPreferences(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody UserPreferencesUpdateRequest request
    ) {
        return preferencesService.updatePreferences(resolveUserId(authorization), request);
    }

    private Long resolveUserId(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new AuthException("Missing bearer token");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new AuthException("Missing bearer token");
        }

        return jwtTokenProvider.parseAccessTokenUserId(token);
    }
}
