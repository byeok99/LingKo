package com.lingko.lingko.core.util;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 음절의 입·혀 프레임 쌍을 공통 역할 기반 매핑에서 추출한다.
 *
 * 평가 경로와 별도 규칙을 유지하면 변이음·복합 모음 처리 결과가 달라지므로 모든 호출을
 * {@link SyllableMappingUtil}의 단일 시퀀스 계약으로 위임한다.
 */
@Component
@RequiredArgsConstructor
public class VisemeExtractorUtil {

    private final SyllableMappingUtil syllableMappingUtil;

    public List<List<String>> extractTongueUrls(String syllable) {
        return syllableMappingUtil.createFramePairs(syllable, VideoType.TONGUE);
    }

    public List<List<String>> extractLipsUrls(String syllable) {
        return syllableMappingUtil.createFramePairs(syllable, VideoType.MOUTH);
    }
}
