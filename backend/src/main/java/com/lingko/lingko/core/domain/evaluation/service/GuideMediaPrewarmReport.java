package com.lingko.lingko.core.domain.evaluation.service;

import java.util.List;

/** 사전 생성 실행 범위와 성공·실패 수를 운영자가 재개 지점과 함께 판단할 수 있게 반환한다. */
public record GuideMediaPrewarmReport(
        int planned,
        int plannedSegments,
        int selected,
        int selectedSegments,
        int succeeded,
        int failed,
        boolean dryRun,
        List<String> failedSignatures
) {
    public GuideMediaPrewarmReport {
        failedSignatures = List.copyOf(failedSignatures);
    }
}
