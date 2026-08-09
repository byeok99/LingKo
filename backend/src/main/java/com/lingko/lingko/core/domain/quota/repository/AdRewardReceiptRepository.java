package com.lingko.lingko.core.domain.quota.repository;

import com.lingko.lingko.core.domain.quota.entity.AdRewardReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 광고 reward event의 사용자별 idempotency 기록을 관리한다. */
public interface AdRewardReceiptRepository extends JpaRepository<AdRewardReceipt, Long> {

    boolean existsByUserIdAndRewardEventId(Long userId, String rewardEventId);

    @Modifying
    @Query("delete from AdRewardReceipt receipt where receipt.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
