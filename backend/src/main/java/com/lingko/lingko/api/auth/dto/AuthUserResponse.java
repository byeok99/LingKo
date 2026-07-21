package com.lingko.lingko.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
