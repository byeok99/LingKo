package com.lingko.lingko.api.user;

import com.lingko.lingko.api.user.dto.UserPreferencesResponse;
import com.lingko.lingko.api.user.dto.UserPreferencesUpdateRequest;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.user.service.UserPreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증된 활성 세션 사용자의 설정을 조회하고 변경한다.
 */
@RestController
@RequestMapping("/api/users/me/preferences")
@RequiredArgsConstructor
public class UserPreferencesController {

    private final UserPreferencesService preferencesService;
    private final ActiveSessionAuthenticator activeSessionAuthenticator;

    /**
     * 활성 Bearer 세션 사용자가 소유한 설정을 반환한다.
     */
    @GetMapping
    public UserPreferencesResponse getMyPreferences(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return preferencesService.findPreferences(activeSessionAuthenticator.authenticateBearer(authorization));
    }

    /**
     * 검증된 설정 변경을 활성 Bearer 세션의 사용자에게 적용한다.
     */
    @PatchMapping
    public UserPreferencesResponse updateMyPreferences(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody UserPreferencesUpdateRequest request
    ) {
        return preferencesService.updatePreferences(
                activeSessionAuthenticator.authenticateBearer(authorization),
                request
        );
    }
}
