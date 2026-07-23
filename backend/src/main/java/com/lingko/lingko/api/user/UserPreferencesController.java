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

@RestController
@RequestMapping("/api/users/me/preferences")
@RequiredArgsConstructor
public class UserPreferencesController {

    private final UserPreferencesService preferencesService;
    private final ActiveSessionAuthenticator activeSessionAuthenticator;

    @GetMapping
    public UserPreferencesResponse getMyPreferences(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return preferencesService.findPreferences(activeSessionAuthenticator.authenticateBearer(authorization));
    }

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
