package com.lingko.lingko.core.domain.quota.exception;

/** 인증 사용자에게 속한 광고 보상 세션을 찾을 수 없을 때 발생한다. */
public class AdRewardSessionNotFoundException extends RuntimeException {
    public AdRewardSessionNotFoundException() {
        super("Ad reward session not found");
    }
}
