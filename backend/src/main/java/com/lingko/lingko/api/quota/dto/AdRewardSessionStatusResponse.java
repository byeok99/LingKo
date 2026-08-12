package com.lingko.lingko.api.quota.dto;

import com.lingko.lingko.core.domain.quota.entity.AdRewardSessionStatus;

/** 앱이 signed callback 처리 완료를 polling하는 최소 상태 계약이다. */
public record AdRewardSessionStatusResponse(AdRewardSessionStatus status, Boolean credited) {
}
