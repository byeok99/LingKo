package com.lingko.lingko.core.domain.auth.service;

/**
 * O Auth Identity 업무 규칙을 조율한다.
 *
 * 컨트롤러와 외부 어댑터가 정책을 소유하지 않도록 도메인 서비스에 조율을 집중했다.
 */
public record OAuthIdentity(
        String socialId,
        String email,
        String name,
        String profileImageUrl
) {
}
