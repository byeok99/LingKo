package com.lingko.lingko.core.domain.user.service;

import com.lingko.lingko.api.auth.dto.RefreshTokenRequest;
import com.lingko.lingko.core.domain.auth.service.AuthService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationAudioStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 회원 탈퇴 재확인, S3 음성 삭제와 DB 개인정보 삭제를 안전한 순서로 조율한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionService {

    private final AuthService authService;
    private final EvaluationAudioStorage audioStorage;
    private final AccountDeletionPersistenceService persistenceService;

    /**
     * S3 정리가 실패하면 DB 계정을 보존해 동일 인증 세션으로 재시도할 수 있게 한다.
     */
    public void deleteAccount(Long userId, RefreshTokenRequest request) {
        authService.validateCurrentRefreshToken(userId, request);

        final int deletedObjectCount;
        try {
            deletedObjectCount = audioStorage.deleteAllForUser(userId);
        } catch (RuntimeException exception) {
            // 운영 로그가 국외 CloudWatch로 복제될 수 있으므로 계정 식별자는 기록하지 않는다.
            log.warn("Account audio cleanup failed", exception);
            throw new AccountDeletionUnavailableException(exception);
        }

        persistenceService.deleteUserData(userId);
        log.info("Account deletion completed: deletedAudioObjects={}", deletedObjectCount);
    }
}
