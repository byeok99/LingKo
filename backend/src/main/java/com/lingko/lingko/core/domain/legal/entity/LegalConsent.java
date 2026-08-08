package com.lingko.lingko.core.domain.legal.entity;

import com.lingko.lingko.core.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * 사용자가 특정 문서 버전의 필수 항목을 수락한 사실을 보존한다.
 *
 * <p>사용자·버전 조합을 유일하게 두어 네트워크 재시도가 감사 기록을 중복 생성하지 않게 한다.
 * 기기 시각과 별도로 서버 기록 시각을 남겨 클라이언트 시간을 단독 증거로 사용하지 않는다.</p>
 */
@Entity
@Table(
        name = "legal_consents",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_idx", "document_version"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "legal_consent_idx")
    private Long legalConsentIdx;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_idx", nullable = false)
    private User user;

    @Column(name = "document_version", nullable = false, length = 32)
    private String documentVersion;

    @Column(name = "terms_agreed", nullable = false)
    private boolean termsAgreed;

    @Column(name = "privacy_acknowledged", nullable = false)
    private boolean privacyAcknowledged;

    @Column(name = "marketing_opt_in", nullable = false)
    private boolean marketingOptIn;

    @Column(name = "client_agreed_at", nullable = false)
    private Instant clientAgreedAt;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    private LegalConsent(
            User user,
            String documentVersion,
            boolean marketingOptIn,
            Instant clientAgreedAt
    ) {
        this.user = user;
        this.documentVersion = documentVersion;
        this.termsAgreed = true;
        this.privacyAcknowledged = true;
        this.marketingOptIn = marketingOptIn;
        this.clientAgreedAt = clientAgreedAt;
    }

    /**
     * 필수 두 항목이 검증된 현재 요청을 감사 가능한 영속 값으로 만든다.
     */
    public static LegalConsent record(
            User user,
            String documentVersion,
            boolean marketingOptIn,
            Instant clientAgreedAt
    ) {
        return new LegalConsent(user, documentVersion, marketingOptIn, clientAgreedAt);
    }
}
