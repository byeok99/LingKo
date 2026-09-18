package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;

import java.util.List;
import java.util.Objects;

/**
 * 한 번만 생성하면 여러 음절이 공유할 수 있는 고유 가이드 영상 작업이다.
 *
 * representativeSyllable은 로그와 수동 검수용 예시이며 cache identity에는 참여하지 않는다.
 */
public record GuideMediaPrewarmItem(
        String representativeSyllable,
        VideoType type,
        List<List<String>> urlPairs,
        String signature
) {
    public GuideMediaPrewarmItem {
        Objects.requireNonNull(representativeSyllable, "representativeSyllable");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(urlPairs, "urlPairs");
        Objects.requireNonNull(signature, "signature");
        urlPairs = urlPairs.stream().map(List::copyOf).toList();
    }
}
