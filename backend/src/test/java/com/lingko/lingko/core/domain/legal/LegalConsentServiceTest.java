package com.lingko.lingko.core.domain.legal;

import com.lingko.lingko.api.legal.dto.LegalConsentRequest;
import com.lingko.lingko.core.domain.legal.entity.LegalConsent;
import com.lingko.lingko.core.domain.legal.repository.LegalConsentRepository;
import com.lingko.lingko.core.domain.legal.service.LegalConsentService;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 사용자별 약관 버전 기록과 재동의 판정이 지켜야 하는 영속 계약을 검증한다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem=legal_consent;MODE=MySQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@Import(LegalConsentService.class)
class LegalConsentServiceTest {

    @Autowired
    private LegalConsentService legalConsentService;

    @Autowired
    private LegalConsentRepository legalConsentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("현재 문서 버전 기록이 없는 사용자는 동의가 필요하다")
    void currentConsentIsRequiredWhenNoRecordExists() {
        User user = saveUser("google-1");

        var status = legalConsentService.getStatus(user.getUserIdx());

        assertThat(status.required()).isTrue();
        assertThat(status.documentVersion()).isEqualTo(LegalConsentPolicy.CURRENT_DOCUMENT_VERSION);
    }

    @Test
    @DisplayName("필수 동의를 제출하면 서버 시각과 사용자 귀속으로 기록되고 재동의가 해제된다")
    void recordsCurrentConsentForAuthenticatedUser() {
        User user = saveUser("google-2");
        Instant clientAgreedAt = Instant.parse("2026-08-07T01:02:03Z");

        var status = legalConsentService.record(
                user.getUserIdx(),
                new LegalConsentRequest(true, true, false,
                        LegalConsentPolicy.CURRENT_DOCUMENT_VERSION, clientAgreedAt)
        );

        assertThat(status.required()).isFalse();
        LegalConsent saved = legalConsentRepository.findAll().getFirst();
        assertThat(saved.getUser().getUserIdx()).isEqualTo(user.getUserIdx());
        assertThat(saved.getClientAgreedAt()).isEqualTo(clientAgreedAt);
        assertThat(saved.getRecordedAt()).isNotNull();
        assertThat(saved.isMarketingOptIn()).isFalse();
    }

    @Test
    @DisplayName("이전 또는 임의 문서 버전으로 현재 동의를 충족할 수 없다")
    void rejectsUnknownDocumentVersion() {
        User user = saveUser("google-3");

        assertThatThrownBy(() -> legalConsentService.record(
                user.getUserIdx(),
                new LegalConsentRequest(true, true, false,
                        "2025-01-01", Instant.parse("2026-08-07T01:02:03Z"))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported consent document version");
    }

    @Test
    @DisplayName("다른 사용자의 동의 기록은 현재 사용자의 동의를 충족하지 않는다")
    void consentIsScopedToAuthenticatedUser() {
        User first = saveUser("google-4");
        User second = saveUser("google-5");
        legalConsentService.record(
                first.getUserIdx(),
                new LegalConsentRequest(true, true, true,
                        LegalConsentPolicy.CURRENT_DOCUMENT_VERSION, Instant.now())
        );

        assertThat(legalConsentService.getStatus(first.getUserIdx()).required()).isFalse();
        assertThat(legalConsentService.getStatus(second.getUserIdx()).required()).isTrue();
    }

    private User saveUser(String socialId) {
        return userRepository.save(User.builder()
                .socialId(socialId)
                .socialType(User.SocialType.GOOGLE)
                .email(socialId + "@example.com")
                .name("LingKo User")
                .build());
    }
}
