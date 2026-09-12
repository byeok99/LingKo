package com.lingko.lingko.core.domain.legal.service;

/** 외부 전송 허용이 없거나 철회되어 업로드·새 평가·재시도를 중단해야 함을 나타낸다. */
public class AiConsentRequiredException extends RuntimeException {
    public AiConsentRequiredException() {
        super("Permission for AI pronunciation assessment is required");
    }
}
