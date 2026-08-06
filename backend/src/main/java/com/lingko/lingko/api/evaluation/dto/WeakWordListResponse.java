package com.lingko.lingko.api.evaluation.dto;

import java.util.List;

/** 취약 어절 목록을 감싸 이후 필드 추가 시 응답 형태가 깨지지 않게 한다. */
public record WeakWordListResponse(List<WeakWordResponse> items) {
}
