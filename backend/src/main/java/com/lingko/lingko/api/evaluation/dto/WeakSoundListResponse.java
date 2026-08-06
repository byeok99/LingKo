package com.lingko.lingko.api.evaluation.dto;

import java.util.List;

/** 취약 음절 목록을 감싸 이후 필드 추가 시 응답 형태가 깨지지 않게 한다. */
public record WeakSoundListResponse(List<WeakSoundResponse> items) {
}
