package com.lingko.lingko.core.domain.quota.repository;

import com.lingko.lingko.core.domain.quota.entity.DailyPracticeQuota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Daily Practice 할당량 영속성 연산을 추상화한다.
 *
 * 도메인 서비스가 query와 저장 기술 세부사항에 의존하지 않도록 저장소 경계를 선택했다.
 */
public interface DailyPracticeQuotaRepository extends JpaRepository<DailyPracticeQuota, Long> {

    Optional<DailyPracticeQuota> findByUserUserIdxAndQuotaDate(Long userId, LocalDate quotaDate);
}
