package com.lingko.lingko.core.domain.speech;

import com.lingko.lingko.core.domain.speech.dto.AssessmentResult;

public interface SpeechEvaluator {
    AssessmentResult evaluate(String audioPath, String referneceText);
}
