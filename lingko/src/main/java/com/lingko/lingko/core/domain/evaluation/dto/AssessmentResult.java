package com.lingko.lingko.core.domain.evaluation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssessmentResult {
    private Double accuracyScore;
    private Double fluencyScore;
    private Double completenessScore;
    private Double pronunciationScore;
    private String recognizedText;
}
