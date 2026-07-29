package com.lingko.lingko.core.domain.user;

import com.lingko.lingko.core.domain.auth.entity.RefreshTokenSession;
import com.lingko.lingko.core.domain.auth.repository.RefreshTokenSessionRepository;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationSyllable;
import com.lingko.lingko.core.domain.evaluation.entity.Syllable;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationJobRepository;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationLogRepository;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationSyllableRepository;
import com.lingko.lingko.core.domain.evaluation.repository.SyllableRepository;
import com.lingko.lingko.core.domain.quota.entity.DailyPracticeQuota;
import com.lingko.lingko.core.domain.quota.repository.DailyPracticeQuotaRepository;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import com.lingko.lingko.core.domain.user.service.AccountDeletionPersistenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 회원 탈퇴 transaction이 사용자 소유 DB 데이터만 삭제하고 공유 음절 기준 정보는 보존하는지 검증한다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:account_deletion;MODE=MySQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@Import(AccountDeletionPersistenceService.class)
class AccountDeletionPersistenceServiceTest {

    @Autowired
    private AccountDeletionPersistenceService deletionService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenSessionRepository refreshTokenSessionRepository;
    @Autowired
    private EvaluationJobRepository evaluationJobRepository;
    @Autowired
    private EvaluationLogRepository evaluationLogRepository;
    @Autowired
    private EvaluationSyllableRepository evaluationSyllableRepository;
    @Autowired
    private SyllableRepository syllableRepository;
    @Autowired
    private DailyPracticeQuotaRepository quotaRepository;

    @Test
    @DisplayName("탈퇴 사용자의 세션·작업·결과·쿼터를 삭제하고 공유 음절은 유지한다")
    void deletesAllUserOwnedDatabaseRecords() {
        User user = userRepository.saveAndFlush(User.builder()
                .socialId("account-delete-user")
                .socialType(User.SocialType.GOOGLE)
                .build());
        refreshTokenSessionRepository.save(RefreshTokenSession.create(
                UUID.randomUUID().toString(),
                user,
                "a".repeat(64),
                Instant.now().plusSeconds(3600)
        ));
        quotaRepository.save(DailyPracticeQuota.create(user, LocalDate.now(), 5));
        evaluationJobRepository.save(EvaluationJob.create(
                UUID.randomUUID().toString(),
                user,
                "delete-idempotency",
                "b".repeat(64),
                "evaluation-audio/" + user.getUserIdx() + "/audio.wav",
                EvaluationLog.PracticeSource.CUSTOM,
                null,
                "안녕하세요.",
                "안녕하세여.",
                LocalDate.now(),
                PracticeQuotaService.QuotaSource.FREE,
                Instant.now()
        ));
        Syllable syllable = syllableRepository.save(Syllable.builder()
                .syllableChar("안")
                .build());
        EvaluationLog log = EvaluationLog.builder()
                .user(user)
                .originalWord("안녕하세요.")
                .score(90)
                .source(EvaluationLog.PracticeSource.CUSTOM)
                .standardPronunciation("안녕하세여.")
                .build();
        log.addSyllable(EvaluationSyllable.builder()
                .syllable(syllable)
                .score(90)
                .positionNo(0)
                .build());
        evaluationLogRepository.saveAndFlush(log);

        deletionService.deleteUserData(user.getUserIdx());

        assertThat(userRepository.count()).isZero();
        assertThat(refreshTokenSessionRepository.count()).isZero();
        assertThat(evaluationJobRepository.count()).isZero();
        assertThat(evaluationLogRepository.count()).isZero();
        assertThat(evaluationSyllableRepository.count()).isZero();
        assertThat(quotaRepository.count()).isZero();
        assertThat(syllableRepository.count()).isEqualTo(1);
    }
}
