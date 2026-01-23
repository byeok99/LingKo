package com.lingko.lingko.core.domain.pronunciation.service;

import com.lingko.lingko.core.util.KoreanPhonemeUtil;
import org.springframework.stereotype.Service;

@Service
public class PronunciationService {

    public String convertToStandardPronunciation(String text) {
        return KoreanPhonemeUtil.toPronunciation(text);
    }
}
