package com.lingko.lingko.core.domain.legal.repository;

import com.lingko.lingko.core.domain.legal.entity.LegalConsent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사용자별 문서 버전 동의 기록의 영속성 경계를 제공한다.
 */
public interface LegalConsentRepository extends JpaRepository<LegalConsent, Long> {

    boolean existsByUserUserIdxAndDocumentVersion(Long userId, String documentVersion);
}
