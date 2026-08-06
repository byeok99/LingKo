package com.lingko.lingko.api.sentence.dto;

/** 저장 토글 결과다. 화면이 다시 조회하지 않고 아이콘 상태를 바꿀 수 있게 현재 상태를 돌려준다. */
public record SavedSentenceResponse(Long sentenceId, boolean saved) {
}
