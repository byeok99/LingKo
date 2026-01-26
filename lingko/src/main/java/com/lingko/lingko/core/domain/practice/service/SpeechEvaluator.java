package com.lingko.lingko.core.domain.practice.service;

import com.lingko.lingko.core.domain.practice.dto.AssessmentResult;

public interface SpeechEvaluator {
    AssessmentResult evaluate(String audioPath, String referneceText);
}
