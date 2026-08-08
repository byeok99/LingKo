package com.lingko.lingko.core.domain.legal.service;

import com.lingko.lingko.api.legal.dto.LegalConsentRequest;
import com.lingko.lingko.api.legal.dto.LegalConsentStatusResponse;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.legal.LegalConsentPolicy;
import com.lingko.lingko.core.domain.legal.entity.LegalConsent;
import com.lingko.lingko.core.domain.legal.repository.LegalConsentRepository;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 사용자와 현재 문서 버전 사이의 동의 필요 여부와 기록 생성을 조율한다.
 */
@Service
@RequiredArgsConstructor
public class LegalConsentService {

    private final LegalConsentRepository legalConsentRepository;
    private final UserRepository userRepository;

    /**
     * 현재 버전 기록이 존재하는지 사용자 범위 안에서 판정한다.
     */
    @Transactional(readOnly = true)
    public LegalConsentStatusResponse getStatus(Long userId) {
        requireUser(userId);
        return statusFor(userId);
    }

    /**
     * 필수 항목과 문서 버전을 방어적으로 재검증한 뒤 최초 기록만 보존한다.
     *
     * <p>사용자 row를 잠가 같은 사용자의 동시 재시도를 직렬화한다. 이미 기록이 있으면
     * 성공으로 돌려 idempotent하게 처리하며 최초 마케팅 선택과 시각은 덮어쓰지 않는다.</p>
     */
    @Transactional
    public LegalConsentStatusResponse record(Long userId, LegalConsentRequest request) {
        validate(request);
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AuthException("Authenticated user not found"));

        if (!legalConsentRepository.existsByUserUserIdxAndDocumentVersion(
                userId,
                LegalConsentPolicy.CURRENT_DOCUMENT_VERSION
        )) {
            legalConsentRepository.saveAndFlush(LegalConsent.record(
                    user,
                    LegalConsentPolicy.CURRENT_DOCUMENT_VERSION,
                    request.marketingOptIn(),
                    request.agreedAt()
            ));
        }
        return statusFor(userId);
    }

    private void validate(LegalConsentRequest request) {
        if (!LegalConsentPolicy.CURRENT_DOCUMENT_VERSION.equals(request.documentVersion())) {
            throw new IllegalArgumentException("Unsupported consent document version");
        }
        if (!Boolean.TRUE.equals(request.termsAgreed())
                || !Boolean.TRUE.equals(request.privacyAcknowledged())) {
            throw new IllegalArgumentException("Required consent is missing");
        }
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Authenticated user not found"));
    }

    private LegalConsentStatusResponse statusFor(Long userId) {
        boolean recorded = legalConsentRepository.existsByUserUserIdxAndDocumentVersion(
                userId,
                LegalConsentPolicy.CURRENT_DOCUMENT_VERSION
        );
        return new LegalConsentStatusResponse(
                !recorded,
                LegalConsentPolicy.CURRENT_DOCUMENT_VERSION
        );
    }
}
