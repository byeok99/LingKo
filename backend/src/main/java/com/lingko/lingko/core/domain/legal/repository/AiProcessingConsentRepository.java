package com.lingko.lingko.core.domain.legal.repository;

import com.lingko.lingko.core.domain.legal.entity.AiProcessingConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/** 사용자 row lock으로 직렬화된 동의 이력 중 마지막 기록만 현재 권한으로 해석한다. */
public interface AiProcessingConsentRepository extends JpaRepository<AiProcessingConsent, Long> {
    Optional<AiProcessingConsent> findFirstByUserUserIdxOrderByIdDesc(Long userId);
}
