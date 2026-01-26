package com.lingko.lingko.core.domain.practice.service;

import com.lingko.lingko.core.util.KoreanPhonemeUtil;
import org.springframework.stereotype.Service;

@Service
public class PracticeService {

    public String convertToStandardPronunciation(String text) {
        return KoreanPhonemeUtil.toPronunciation(text);
    }
}
