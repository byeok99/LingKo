package com.lingko.lingko.api.evaluation.dto;

import java.time.Instant;

/**
 * 비공개 S3 object와 제한 시간 PUT URL을 앱에 제공한다.
 */
public record EvaluationUploadResponse(
        String objectKey,
        String uploadUrl,
        Instant expiresAt
) {
}
