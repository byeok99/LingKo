package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.util.GuideMediaCacheKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 고유 가이드 영상 계획을 제한된 범위로 실행한다.
 *
 * S3 cache hit는 VideoGenerator가 멱등하게 처리하므로 같은 명령을 재실행해도 완료 항목은 외부 AI를
 * 다시 호출하지 않는다. 기본 순차 실행은 Replicate rate limit과 일반 평가 Worker의 비용 경쟁을 줄인다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuideMediaPrewarmService {

    private static final int PROGRESS_LOG_INTERVAL = 25;

    private final GuideMediaPrewarmPlanner planner;
    private final VideoGenerator videoGenerator;

    /** 지정 범위의 cache를 계획하거나 실제로 채우고 재개 가능한 실행 결과를 반환한다. */
    public GuideMediaPrewarmReport prewarm(GuideMediaPrewarmOptions options) {
        List<GuideMediaPrewarmItem> plan = planner.planAll();
        List<GuideMediaPrewarmItem> selected = plan.stream()
                .filter(item -> options.type() == null || item.type() == options.type())
                .skip(options.offset())
                .limit(options.limit())
                .toList();
        int plannedSegments = countUniqueSegments(plan);
        int selectedSegments = countUniqueSegments(selected);

        log.info(
                "Guide prewarm plan: planned={}, plannedSegments={}, selected={}, selectedSegments={}, "
                        + "type={}, offset={}, limit={}, dryRun={}",
                plan.size(),
                plannedSegments,
                selected.size(),
                selectedSegments,
                options.type() == null ? "ALL" : options.type(),
                options.offset(),
                options.limit(),
                options.dryRun()
        );
        if (options.dryRun()) {
            return new GuideMediaPrewarmReport(
                    plan.size(),
                    plannedSegments,
                    selected.size(),
                    selectedSegments,
                    0,
                    0,
                    true,
                    List.of()
            );
        }

        int succeeded = 0;
        List<String> failedSignatures = new ArrayList<>();
        for (int index = 0; index < selected.size(); index++) {
            GuideMediaPrewarmItem item = selected.get(index);
            try {
                videoGenerator.generate(item.urlPairs(), item.representativeSyllable(), item.type());
                succeeded++;
            } catch (RuntimeException exception) {
                failedSignatures.add(item.signature());
                log.warn(
                        "Guide prewarm item failed: index={}/{}, syllable={}, type={}, signature={}",
                        index + 1,
                        selected.size(),
                        item.representativeSyllable(),
                        item.type(),
                        item.signature(),
                        exception
                );
                if (!options.continueOnError()) {
                    throw exception;
                }
            }
            if ((index + 1) % PROGRESS_LOG_INTERVAL == 0 || index + 1 == selected.size()) {
                log.info(
                        "Guide prewarm progress: completed={}/{}, succeeded={}, failed={}",
                        index + 1,
                        selected.size(),
                        succeeded,
                        failedSignatures.size()
                );
            }
        }
        return new GuideMediaPrewarmReport(
                plan.size(),
                plannedSegments,
                selected.size(),
                selectedSegments,
                succeeded,
                failedSignatures.size(),
                false,
                failedSignatures
        );
    }

    /** 선택된 최종 영상들이 공유하는 유료 생성 후보 전이를 중복 없이 계산한다. */
    static int countUniqueSegments(List<GuideMediaPrewarmItem> items) {
        Set<String> signatures = new HashSet<>();
        for (GuideMediaPrewarmItem item : items) {
            for (List<String> pair : item.urlPairs()) {
                signatures.add(GuideMediaCacheKey.segmentSignature(item.type(), pair));
            }
        }
        return signatures.size();
    }
}
