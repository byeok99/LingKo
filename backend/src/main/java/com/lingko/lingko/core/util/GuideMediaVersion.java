package com.lingko.lingko.core.util;

/**
 * 가이드 매핑과 생성 영상 cache가 공유하는 명시적 계약 버전이다.
 *
 * 이미지 내용이나 프레임 순서가 달라지면 이 값을 올려 이전 DB·S3 영상이 새 조음 계약으로
 * 오인되지 않게 한다. URL 문자열이 같아도 자산 내용은 바뀔 수 있으므로 수동 버전을 cache key에 포함한다.
 */
public final class GuideMediaVersion {

    public static final String CURRENT = "phonology-v3";

    private GuideMediaVersion() {
    }
}
