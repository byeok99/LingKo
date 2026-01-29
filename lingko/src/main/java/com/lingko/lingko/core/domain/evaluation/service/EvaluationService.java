package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.util.KoreanPhonemeUtil;
import org.springframework.stereotype.Service;

@Service
public class EvaluationService {

    public String convertToStandardPronunciation(String text) {
        return KoreanPhonemeUtil.toPronunciation(text);
    }
}
