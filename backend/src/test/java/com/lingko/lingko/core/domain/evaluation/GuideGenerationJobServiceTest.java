package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.core.domain.evaluation.dto.GuideGenerationJobStatus;
import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.domain.evaluation.exception.GuideJobCapacityExceededException;
import com.lingko.lingko.core.domain.evaluation.service.GuideGenerationJobService;
import com.lingko.lingko.core.domain.evaluation.service.GuideSourceUrlPolicy;
import com.lingko.lingko.core.domain.evaluation.service.VideoGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guide Generation Job 서비스 Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
class GuideGenerationJobServiceTest {

    @Test
    void submitCompletesJobWithGeneratedVideoUrl() {
        VideoGenerator generator = (urlPairs, syllable, type) -> "https://cdn.example.com/guides/ma.mp4";
        GuideGenerationJobService service = new GuideGenerationJobService(generator, Runnable::run);

        var job = service.submit("마", VideoType.MOUTH, List.of(List.of("https://example.com/a.png", "https://example.com/b.png")));

        assertThat(job.getStatus()).isEqualTo(GuideGenerationJobStatus.COMPLETED);
        assertThat(job.getResultUrl()).isEqualTo("https://cdn.example.com/guides/ma.mp4");
        assertThat(job.getCacheKey()).isNotBlank();
        assertThat(service.find(job.getJobId()).orElseThrow().getStatus()).isEqualTo(GuideGenerationJobStatus.COMPLETED);
    }

    @Test
    void submitReusesExistingJobForSameCacheKey() {
        AtomicInteger calls = new AtomicInteger();
        VideoGenerator generator = (urlPairs, syllable, type) -> {
            calls.incrementAndGet();
            return "https://cdn.example.com/guides/ma.mp4";
        };
        GuideGenerationJobService service = new GuideGenerationJobService(generator, Runnable::run);
        List<List<String>> urlPairs = List.of(List.of("https://example.com/a.png", "https://example.com/b.png"));

        var first = service.submit("마", VideoType.MOUTH, urlPairs);
        var second = service.submit("마", VideoType.MOUTH, urlPairs);

        assertThat(second.getJobId()).isEqualTo(first.getJobId());
        assertThat(calls).hasValue(1);
    }

    @Test
    void submitMarksJobFailedWhenGeneratorFails() {
        VideoGenerator generator = (urlPairs, syllable, type) -> {
            throw new IllegalStateException("replicate timeout");
        };
        GuideGenerationJobService service = new GuideGenerationJobService(generator, Runnable::run);

        var job = service.submit("마", VideoType.MOUTH, List.of(List.of("https://example.com/a.png", "https://example.com/b.png")));

        assertThat(job.getStatus()).isEqualTo(GuideGenerationJobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("Guide generation failed");
    }

    @Test
    void validatesEverySourceUrlBeforeRegisteringOrExecutingJob() {
        AtomicInteger generationCalls = new AtomicInteger();
        VideoGenerator generator = (urlPairs, syllable, type) -> {
            generationCalls.incrementAndGet();
            return "https://cdn.example.com/guides/ma.mp4";
        };
        GuideSourceUrlPolicy policy = url -> {
            if (url.contains("private")) {
                throw new IllegalArgumentException("private URL");
            }
        };
        GuideGenerationJobService service = new GuideGenerationJobService(
                generator,
                Runnable::run,
                policy,
                1
        );

        assertThatThrownBy(() -> service.submit(
                "마",
                VideoType.MOUTH,
                List.of(List.of("https://replicate.delivery/private/a.png"))
        )).isInstanceOf(IllegalArgumentException.class);
        assertThat(generationCalls).hasValue(0);
    }

    @Test
    void rejectsDistinctJobWhileConcurrencySlotIsOccupiedAndReleasesItAfterCompletion() {
        AtomicReference<Runnable> pending = new AtomicReference<>();
        GuideGenerationJobService service = new GuideGenerationJobService(
                (urlPairs, syllable, type) -> "https://cdn.example.com/guides/" + syllable + ".mp4",
                pending::set,
                url -> { },
                1
        );

        service.submit("마", VideoType.MOUTH, List.of(List.of("https://replicate.delivery/a.png")));
        assertThatThrownBy(() -> service.submit(
                "나",
                VideoType.MOUTH,
                List.of(List.of("https://replicate.delivery/b.png"))
        )).isInstanceOf(GuideJobCapacityExceededException.class);

        pending.get().run();
        assertThat(service.submit(
                "나",
                VideoType.MOUTH,
                List.of(List.of("https://replicate.delivery/b.png"))
        ).getStatus()).isEqualTo(GuideGenerationJobStatus.PENDING);
    }
}
