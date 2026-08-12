package com.lingko.lingko.core.domain.quota;

import com.lingko.lingko.api.quota.dto.AdRewardSessionResponse;
import com.lingko.lingko.core.config.AdMobSsvSettings;
import com.lingko.lingko.core.domain.quota.entity.AdRewardSessionStatus;
import com.lingko.lingko.core.domain.quota.repository.AdRewardReceiptRepository;
import com.lingko.lingko.core.domain.quota.repository.AdRewardSessionRepository;
import com.lingko.lingko.core.domain.quota.repository.DailyPracticeQuotaRepository;
import com.lingko.lingko.core.domain.quota.service.AdRewardService;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import com.lingko.lingko.core.domain.quota.service.VerifiedAdRewardCallback;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/** client 요청만으로는 지급하지 않고 verified SSV transaction만 한 번 지급하는 계약을 검증한다. */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ad_reward;MODE=MySQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "admob.ssv.allowed-ad-unit-ids=3927267131"
})
@Import({AdRewardService.class, PracticeQuotaService.class, AdMobSsvSettings.class})
class AdRewardServiceTest {

    @Autowired private AdRewardService rewardService;
    @Autowired private PracticeQuotaService quotaService;
    @Autowired private UserRepository userRepository;
    @Autowired private AdRewardSessionRepository sessionRepository;
    @Autowired private AdRewardReceiptRepository receiptRepository;
    @Autowired private DailyPracticeQuotaRepository quotaRepository;

    @Test
    @DisplayName("signed callback이 session token과 정책에 맞으면 1회 지급하고 완료 상태가 된다")
    void grantsVerifiedRewardOnce() {
        User user = saveUser();
        quotaService.consumePractice(user.getUserIdx());
        AdRewardSessionResponse session = rewardService.createSession(user.getUserIdx());
        VerifiedAdRewardCallback callback = callback(session.sessionToken(), "transaction-1");

        rewardService.processVerifiedCallback(callback);
        rewardService.processVerifiedCallback(callback);

        assertThat(rewardService.getSessionStatus(user.getUserIdx(), session.sessionToken()).status())
                .isEqualTo(AdRewardSessionStatus.COMPLETED);
        assertThat(rewardService.getSessionStatus(user.getUserIdx(), session.sessionToken()).credited())
                .isTrue();
        assertThat(quotaService.getTodayQuota(user.getUserIdx()).remainingPractices()).isEqualTo(5);
        assertThat(receiptRepository.count()).isEqualTo(1);
        assertThat(sessionRepository.count()).isEqualTo(1);
        assertThat(quotaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("세션 생성만으로는 quota를 지급하지 않는다")
    void doesNotTrustClientSessionCreation() {
        User user = saveUser();
        quotaService.consumePractice(user.getUserIdx());

        rewardService.createSession(user.getUserIdx());

        assertThat(quotaService.getTodayQuota(user.getUserIdx()).remainingPractices()).isEqualTo(4);
        assertThat(receiptRepository.count()).isZero();
    }

    @Test
    @DisplayName("같은 Google transaction은 다른 session에도 두 번 지급하지 않는다")
    void rejectsDuplicateGoogleTransactionAcrossSessions() {
        User first = saveUser("ssv-user-1");
        User second = saveUser("ssv-user-2");
        quotaService.consumePractice(first.getUserIdx());
        quotaService.consumePractice(second.getUserIdx());
        AdRewardSessionResponse firstSession = rewardService.createSession(first.getUserIdx());
        AdRewardSessionResponse secondSession = rewardService.createSession(second.getUserIdx());

        rewardService.processVerifiedCallback(callback(firstSession.sessionToken(), "transaction-1"));
        rewardService.processVerifiedCallback(callback(secondSession.sessionToken(), "transaction-1"));

        assertThat(quotaService.getTodayQuota(first.getUserIdx()).remainingPractices()).isEqualTo(5);
        assertThat(quotaService.getTodayQuota(second.getUserIdx()).remainingPractices()).isEqualTo(4);
        assertThat(receiptRepository.count()).isEqualTo(1);
    }

    private VerifiedAdRewardCallback callback(String token, String transactionId) {
        return new VerifiedAdRewardCallback(
                "3927267131", token, 1, "pronunciation_chance", 1786500000000L, transactionId
        );
    }

    private User saveUser() {
        return saveUser("ssv-user");
    }

    private User saveUser(String socialId) {
        return userRepository.save(User.builder()
                .socialId(socialId)
                .socialType(User.SocialType.GOOGLE)
                .build());
    }
}
