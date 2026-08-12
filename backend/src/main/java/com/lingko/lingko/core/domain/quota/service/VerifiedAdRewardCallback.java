package com.lingko.lingko.core.domain.quota.service;

/** 암호학적 검증을 통과한 AdMob SSV 지급 parameter만 도메인에 전달한다. */
public record VerifiedAdRewardCallback(
        String adUnitId,
        String customData,
        int rewardAmount,
        String rewardItem,
        long timestamp,
        String transactionId
) {
}
