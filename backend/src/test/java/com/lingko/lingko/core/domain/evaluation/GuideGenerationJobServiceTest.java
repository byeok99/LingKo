package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.core.domain.evaluation.dto.GuideGenerationJobStatus;
import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.domain.evaluation.service.GuideGenerationJobService;
import com.lingko.lingko.core.domain.evaluation.service.VideoGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

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
}
