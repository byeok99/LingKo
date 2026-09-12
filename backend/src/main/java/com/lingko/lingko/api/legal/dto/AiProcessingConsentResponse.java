package com.lingko.lingko.api.legal.dto;

import java.time.Instant;

/** 현재 고지 버전의 허용 여부다. recordedAt=null은 이력이 없으며 허용하지 않았다는 뜻이다. */
public record AiProcessingConsentResponse(boolean granted, String noticeVersion, Instant recordedAt) { }
