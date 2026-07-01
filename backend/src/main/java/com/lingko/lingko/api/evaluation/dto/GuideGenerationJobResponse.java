package com.lingko.lingko.api.evaluation.dto;

import com.lingko.lingko.core.domain.evaluation.dto.GuideGenerationJobStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuideGenerationJobResponse {
    private String jobId;
    private GuideGenerationJobStatus status;
    private String cacheKey;
    private String resultUrl;
    private String errorMessage;
}
