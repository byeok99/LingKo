package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.domain.evaluation.service.VideoGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 사전 생성 실행기가 dry-run·범위 제한·실패 격리를 지키는지 검증한다. */
class GuideMediaPrewarmServiceTest {

    @Test
    void dryRunReportsSelectionWithoutCallingExternalGenerator() {
        GuideMediaPrewarmPlanner planner = mock(GuideMediaPrewarmPlanner.class);
        VideoGenerator generator = mock(VideoGenerator.class);
        when(planner.planAll()).thenReturn(items());
        GuideMediaPrewarmService service = new GuideMediaPrewarmService(planner, generator);

        var report = service.prewarm(new GuideMediaPrewarmOptions(true, 0, 1, null, true));

        assertThat(report.planned()).isEqualTo(2);
        assertThat(report.plannedSegments()).isEqualTo(2);
        assertThat(report.selected()).isEqualTo(1);
        assertThat(report.selectedSegments()).isEqualTo(1);
        assertThat(report.succeeded()).isZero();
        assertThat(report.failed()).isZero();
        verifyNoInteractions(generator);
    }

    @Test
    void continuesAfterOneItemFailsAndReportsBothOutcomes() {
        GuideMediaPrewarmPlanner planner = mock(GuideMediaPrewarmPlanner.class);
        when(planner.planAll()).thenReturn(items());
        AtomicInteger calls = new AtomicInteger();
        VideoGenerator generator = (pairs, syllable, type) -> {
            if (calls.getAndIncrement() == 0) {
                throw new IllegalStateException("provider failed");
            }
            return "https://bucket/final.mp4";
        };
        GuideMediaPrewarmService service = new GuideMediaPrewarmService(planner, generator);

        var report = service.prewarm(new GuideMediaPrewarmOptions(false, 0, 2, null, true));

        assertThat(report.succeeded()).isOne();
        assertThat(report.failed()).isOne();
        assertThat(report.failedSignatures()).containsExactly("mouth-signature");
    }

    private List<GuideMediaPrewarmItem> items() {
        return List.of(
                new GuideMediaPrewarmItem(
                        "가",
                        VideoType.MOUTH,
                        List.of(List.of("https://guides/a.png", "https://guides/b.png")),
                        "mouth-signature"
                ),
                new GuideMediaPrewarmItem(
                        "나",
                        VideoType.TONGUE,
                        List.of(List.of("https://guides/c.png", "https://guides/d.png")),
                        "tongue-signature"
                )
        );
    }
}
