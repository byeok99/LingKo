package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.domain.evaluation.dto.AssessmentResult;

public interface SpeechEvaluator {
    AssessmentResult evaluate(String audioPath, String referneceText);
}
