package com.lingko.lingko.core.domain.quota.repository;

import com.lingko.lingko.core.domain.quota.entity.DailyPracticeQuota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyPracticeQuotaRepository extends JpaRepository<DailyPracticeQuota, Long> {

    Optional<DailyPracticeQuota> findByUserUserIdxAndQuotaDate(Long userId, LocalDate quotaDate);
}
