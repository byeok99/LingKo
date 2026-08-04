package com.lingko.lingko.core.domain.user.service;

import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.repository.RefreshTokenSessionRepository;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationJobRepository;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationLogRepository;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationSyllableRepository;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationWordRepository;
import com.lingko.lingko.core.domain.quota.repository.DailyPracticeQuotaRepository;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴 시 사용자 소유 DB 데이터를 foreign key 순서에 맞춰 하나의 transaction으로 삭제한다.
 */
@Service
@RequiredArgsConstructor
public class AccountDeletionPersistenceService {

    private final UserRepository userRepository;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final EvaluationJobRepository evaluationJobRepository;
    private final EvaluationLogRepository evaluationLogRepository;
    private final EvaluationSyllableRepository evaluationSyllableRepository;
    private final EvaluationWordRepository evaluationWordRepository;
    private final DailyPracticeQuotaRepository quotaRepository;

    /**
     * 공유 음절 기준 정보는 보존하고 사용자·세션·작업·결과·쿼터만 삭제한다.
     */
    @Transactional
    public void deleteUserData(Long userId) {
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        refreshTokenSessionRepository.deleteAllByUserId(userId);
        evaluationJobRepository.deleteAllByUserId(userId);
        evaluationSyllableRepository.deleteAllByUserId(userId);
        evaluationWordRepository.deleteAllByUserId(userId);
        evaluationLogRepository.deleteAllByUserId(userId);
        quotaRepository.deleteAllByUserId(userId);
        if (userRepository.deleteAccountById(userId) != 1) {
            throw new AuthException("User not found");
        }
    }
}
