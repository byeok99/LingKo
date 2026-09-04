package com.lingko.lingko.api.evaluation;

import com.lingko.lingko.api.evaluation.dto.EvaluationJobRequest;
import com.lingko.lingko.api.evaluation.dto.EvaluationJobResponse;
import com.lingko.lingko.api.evaluation.dto.EvaluationUploadRequest;
import com.lingko.lingko.api.evaluation.dto.EvaluationUploadResponse;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API 서버를 거치지 않는 음성 업로드와 영속 비동기 평가 작업 경계를 제공한다.
 */
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationJobController {

    private final EvaluationJobService jobService;
    private final ActiveSessionAuthenticator activeSessionAuthenticator;

    @PostMapping("/uploads")
    public ResponseEntity<EvaluationUploadResponse> prepareUpload(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody EvaluationUploadRequest request
    ) {
        Long userId = activeSessionAuthenticator.authenticateBearer(authorization);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.prepareUpload(userId, request));
    }

    @PostMapping("/jobs")
    public ResponseEntity<EvaluationJobResponse> createJob(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody EvaluationJobRequest request
    ) {
        Long userId = activeSessionAuthenticator.authenticateBearer(authorization);
        return ResponseEntity.accepted()
                .body(jobService.createJob(userId, idempotencyKey, request));
    }

    @GetMapping("/jobs/{jobId}")
    public EvaluationJobResponse getJob(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String jobId
    ) {
        return jobService.getJob(
                activeSessionAuthenticator.authenticateBearer(authorization),
                jobId
        );
    }
}
