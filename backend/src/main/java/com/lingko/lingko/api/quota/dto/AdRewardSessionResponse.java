package com.lingko.lingko.api.quota.dto;

import java.time.OffsetDateTime;

/** 앱이 AdMob customData에 넣을 1회성 token과 만료 시각이다. */
public record AdRewardSessionResponse(String sessionToken, OffsetDateTime expiresAt) {
}
