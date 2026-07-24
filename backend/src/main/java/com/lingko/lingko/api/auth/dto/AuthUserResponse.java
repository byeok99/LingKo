package com.lingko.lingko.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * HTTP 경계에서 사용하는 Auth User 요청·응답 구조를 정의한다.
 *
 * 영속 엔티티를 직접 노출하지 않고 전송 계약을 독립적으로 유지하기 위해 전용 DTO를 선택했다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUserResponse {
    private Long userId;
    private String email;
    private String name;
    private String profileImageUrl;
}
