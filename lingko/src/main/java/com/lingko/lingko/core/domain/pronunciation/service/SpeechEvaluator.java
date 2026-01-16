package com.lingko.lingko.core.domain.pronunciation.service;

import com.lingko.lingko.core.domain.pronunciation.dto.AssessmentResult;

public interface SpeechEvaluator {
    AssessmentResult evaluate(String audioPath, String referneceText);
}
