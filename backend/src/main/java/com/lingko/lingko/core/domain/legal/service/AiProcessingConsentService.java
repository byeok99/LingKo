package com.lingko.lingko.core.domain.legal.service;

import com.lingko.lingko.api.legal.dto.AiProcessingConsentResponse;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.legal.entity.AiProcessingConsent;
import com.lingko.lingko.core.domain.legal.repository.AiProcessingConsentRepository;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.Objects;

/** 약관 확인과 분리된 AI 전송 권한, 고지 버전, 철회 및 작업별 동의 근거를 관리한다. */
@Service
@RequiredArgsConstructor
public class AiProcessingConsentService {
    /** 앱의 명시적 Azure·AWS 전송 설명과 맞추며 내용 변경 시 기존 허용을 재사용하지 않는다. */
    public static final String CURRENT_VERSION = "2026-09-12";
    private final AiProcessingConsentRepository repository;
    private final UserRepository users;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AiProcessingConsentResponse getStatus(Long userId) {
        if (!users.existsById(userId)) {
            throw new AuthException("Authenticated user not found");
        }
        return response(repository.findFirstByUserUserIdxOrderByIdDesc(userId).orElse(null));
    }

    /** 같은 선택의 재시도는 멱등 처리하고, 철회 뒤 재허용은 새 이력으로 남긴다. */
    @Transactional
    public AiProcessingConsentResponse record(Long userId, boolean granted, String version) {
        if (granted && !CURRENT_VERSION.equals(version)) {
            throw new IllegalArgumentException("Unsupported AI consent notice version");
        }
        var user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new AuthException("Authenticated user not found"));
        var latest = repository.findFirstByUserUserIdxOrderByIdDesc(userId).orElse(null);
        if (latest != null && latest.isGranted() == granted
                && CURRENT_VERSION.equals(latest.getNoticeVersion())) {
            return response(latest);
        }
        // 구버전 앱에서도 철회는 허용하되 재허용에는 반드시 현행 고지를 요구한다.
        return response(repository.saveAndFlush(AiProcessingConsent.record(
                user, CURRENT_VERSION, granted, clock.instant())));
    }

    /** 외부 전송을 허용한 이력 ID를 반환한다. 기존 약관 동의를 AI 허용으로 승격하지 않는다. */
    @Transactional(readOnly = true)
    public Long requireGranted(Long userId) {
        var latest = repository.findFirstByUserUserIdxOrderByIdDesc(userId).orElse(null);
        if (!isGranted(latest)) {
            throw new AiConsentRequiredException();
        }
        return latest.getId();
    }

    /** 철회·재동의 이후 옛 작업이 살아나지 않도록 현재 허용과 생성 당시 ID를 함께 비교한다. */
    @Transactional(readOnly = true)
    public void requireJobConsent(Long userId, Long consentId) {
        if (consentId == null || !Objects.equals(requireGranted(userId), consentId)) {
            throw new AiConsentRequiredException();
        }
    }

    private boolean isGranted(AiProcessingConsent consent) {
        return consent != null && consent.isGranted() && CURRENT_VERSION.equals(consent.getNoticeVersion());
    }

    private AiProcessingConsentResponse response(AiProcessingConsent consent) {
        return new AiProcessingConsentResponse(isGranted(consent), CURRENT_VERSION,
                consent == null ? null : consent.getRecordedAt());
    }
}
