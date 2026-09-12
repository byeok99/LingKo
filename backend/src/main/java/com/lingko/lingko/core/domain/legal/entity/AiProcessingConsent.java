package com.lingko.lingko.core.domain.legal.entity;

import com.lingko.lingko.core.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.Instant;

/** AI 전송 허용·철회를 덮어쓰지 않는 이력으로 보존하고 작업의 동의 근거를 식별한다. */
@Entity
@Table(name = "ai_processing_consents", indexes = @Index(name = "idx_ai_consent_user_id", columnList = "user_idx,id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiProcessingConsent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_idx", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "notice_version", nullable = false, length = 32)
    private String noticeVersion;

    /** true는 명시적 허용, false는 철회다. 기록 부재는 미동의로 해석한다. */
    @Column(nullable = false)
    private boolean granted;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    /** 시각은 신뢰할 수 있는 서버 Clock에서 전달하며 기기 시각을 받지 않는다. */
    public static AiProcessingConsent record(User user, String version, boolean granted, Instant now) {
        AiProcessingConsent record = new AiProcessingConsent();
        record.user = user;
        record.noticeVersion = version;
        record.granted = granted;
        record.recordedAt = now;
        return record;
    }
}
