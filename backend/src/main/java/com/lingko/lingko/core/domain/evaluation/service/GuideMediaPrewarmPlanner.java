package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.util.GuideMediaCacheKey;
import com.lingko.lingko.core.util.SyllableMappingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 현대 한글 11,172자를 외부 생성이 필요한 고유 프레임 시퀀스로 축약한다.
 *
 * 문장 목록은 무한하지만 현재 이미지 자산으로 표현되는 시퀀스는 유한하다. 이 planner는 실제
 * 운영 resolver와 같은 매핑 함수를 사용해 별도 corpus 도구가 제품 규칙과 어긋나지 않게 한다.
 */
@Component
@RequiredArgsConstructor
public class GuideMediaPrewarmPlanner {

    private static final char FIRST_MODERN_HANGUL = 0xAC00;
    private static final char LAST_MODERN_HANGUL = 0xD7A3;

    private final SyllableMappingUtil syllableMappingUtil;

    /** 현대 한글 전체에서 정적 결과를 제외한 고유 동적 시퀀스를 반환한다. */
    public List<GuideMediaPrewarmItem> planAll() {
        Map<String, GuideMediaPrewarmItem> uniqueItems = new LinkedHashMap<>();
        for (VideoType type : VideoType.values()) {
            for (char value = FIRST_MODERN_HANGUL; value <= LAST_MODERN_HANGUL; value++) {
                String syllable = String.valueOf(value);
                List<List<String>> pairs = syllableMappingUtil.createFramePairs(syllable, type);
                if (isStatic(pairs)) {
                    continue;
                }
                String signature = GuideMediaCacheKey.sequenceSignature(type, pairs);
                uniqueItems.putIfAbsent(
                        signature,
                        new GuideMediaPrewarmItem(syllable, type, pairs, signature)
                );
            }
        }
        return List.copyOf(uniqueItems.values());
    }

    private boolean isStatic(List<List<String>> pairs) {
        return pairs.isEmpty() || (pairs.size() == 1 && pairs.get(0).size() == 1);
    }
}
