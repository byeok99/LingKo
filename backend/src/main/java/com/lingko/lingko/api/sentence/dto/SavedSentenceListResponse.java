package com.lingko.lingko.api.sentence.dto;

import java.util.List;

/**
 * 저장한 문장 목록이다.
 *
 * {@code totalCount}를 따로 두는 이유는 화면 머리말의 개수가 목록 길이와 반드시 일치해야 하기
 * 때문이다. 화면이 직접 세면 이후 페이지네이션이 붙었을 때 조용히 어긋난다.
 */
public record SavedSentenceListResponse(
        List<PracticeSentenceResponse> items,
        int totalCount
) {
}
