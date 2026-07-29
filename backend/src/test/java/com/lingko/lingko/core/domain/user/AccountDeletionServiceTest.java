package com.lingko.lingko.core.domain.user;

import com.lingko.lingko.api.auth.dto.RefreshTokenRequest;
import com.lingko.lingko.core.domain.auth.service.AuthService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationAudioStorage;
import com.lingko.lingko.core.domain.user.service.AccountDeletionPersistenceService;
import com.lingko.lingko.core.domain.user.service.AccountDeletionService;
import com.lingko.lingko.core.domain.user.service.AccountDeletionUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 재인증, S3 prefix 정리와 DB 개인정보 삭제의 순서·실패 계약을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    @Mock
    private AuthService authService;
    @Mock
    private EvaluationAudioStorage audioStorage;
    @Mock
    private AccountDeletionPersistenceService persistenceService;

    @Test
    @DisplayName("현재 Refresh Token 확인 후 S3를 먼저 정리하고 DB 계정을 삭제한다")
    void deletesStorageBeforeDatabaseAccount() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh.jwt");
        when(audioStorage.deleteAllForUser(7L)).thenReturn(3);
        AccountDeletionService service = new AccountDeletionService(
                authService,
                audioStorage,
                persistenceService
        );

        service.deleteAccount(7L, request);

        InOrder order = inOrder(authService, audioStorage, persistenceService);
        order.verify(authService).validateCurrentRefreshToken(7L, request);
        order.verify(audioStorage).deleteAllForUser(7L);
        order.verify(persistenceService).deleteUserData(7L);
    }

    @Test
    @DisplayName("S3 정리 실패 시 사용자 DB를 보존해 같은 탈퇴 요청을 재시도할 수 있다")
    void preservesDatabaseAccountWhenStorageCleanupFails() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh.jwt");
        when(audioStorage.deleteAllForUser(7L))
                .thenThrow(new IllegalStateException("S3 unavailable"));
        AccountDeletionService service = new AccountDeletionService(
                authService,
                audioStorage,
                persistenceService
        );

        assertThatThrownBy(() -> service.deleteAccount(7L, request))
                .isInstanceOf(AccountDeletionUnavailableException.class);

        verify(persistenceService, never()).deleteUserData(7L);
    }
}
