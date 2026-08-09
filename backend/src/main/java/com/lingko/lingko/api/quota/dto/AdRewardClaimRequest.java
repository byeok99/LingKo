package com.lingko.lingko.api.quota.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Mobile Ads SDK의 reward callback 한 건을 서버 지급 요청으로 전달한다.
 *
 * <p>event ID만 받으며 사용자와 보상량은 받지 않는다. 사용자는 Bearer token으로 확인하고
 * 지급량은 서버가 1회로 고정해 클라이언트 입력을 신뢰하지 않는다.</p>
 */
public record AdRewardClaimRequest(
        @NotBlank
        @Size(min = 16, max = 80)
        @Pattern(regexp = "[A-Za-z0-9_-]+")
        String rewardEventId
) {
}
