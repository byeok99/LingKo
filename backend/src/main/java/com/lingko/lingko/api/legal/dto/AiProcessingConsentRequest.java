package com.lingko.lingko.api.legal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 명시적인 허용 또는 철회만 받으며 사용자와 기록 시각은 서버에서 결정한다. */
public record AiProcessingConsentRequest(
        @NotNull Boolean granted,
        @NotBlank @Size(max = 32) String noticeVersion
) { }
