package com.lingko.lingko.api.evaluation;

import com.lingko.lingko.api.common.ErrorResponse;
import com.lingko.lingko.api.evaluation.dto.GuideGenerationJobRequest;
import com.lingko.lingko.api.evaluation.dto.GuideGenerationJobResponse;
import com.lingko.lingko.core.domain.evaluation.service.GuideGenerationJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Guide Generation Job 기능의 HTTP 진입점을 제공한다.
 *
 * 컨트롤러는 전송 형식 검증과 응답 변환만 담당하고 업무 결정은 도메인 서비스에 위임한다.
 */
@RestController
@RequestMapping("/api/pronunciation/guide-jobs")
@RequiredArgsConstructor
public class GuideGenerationJobController {

    private final GuideGenerationJobService guideGenerationJobService;

    @PostMapping
    public ResponseEntity<GuideGenerationJobResponse> create(
            @Valid @RequestBody GuideGenerationJobRequest request
    ) {
        GuideGenerationJobResponse response = guideGenerationJobService.submit(
                request.trimmedSyllable(),
                request.getType(),
                request.getUrlPairs()
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<?> get(@PathVariable String jobId) {
        return guideGenerationJobService.find(jobId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ErrorResponse.of("GUIDE_JOB_NOT_FOUND", "Guide generation job not found")));
    }
}
