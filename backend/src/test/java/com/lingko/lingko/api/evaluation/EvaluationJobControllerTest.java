package com.lingko.lingko.api.evaluation;

import com.lingko.lingko.api.evaluation.dto.EvaluationJobRequest;
import com.lingko.lingko.api.evaluation.dto.EvaluationJobResponse;
import com.lingko.lingko.api.evaluation.dto.EvaluationUploadRequest;
import com.lingko.lingko.api.evaluation.dto.EvaluationUploadResponse;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 음성 직접 업로드와 비동기 평가 작업 API의 인증·상태 계약을 검증한다.
 */
@WebMvcTest(EvaluationJobController.class)
class EvaluationJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EvaluationJobService jobService;

    @MockitoBean
    private ActiveSessionAuthenticator activeSessionAuthenticator;

    @Test
    @DisplayName("인증 사용자는 WAV 직접 업로드용 Presigned URL을 발급받는다")
    void createsUploadTicket() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer("Bearer access-token")).thenReturn(7L);
        when(jobService.prepareUpload(any(), any())).thenReturn(new EvaluationUploadResponse(
                "evaluation-audio/7/audio-id.wav",
                "https://signed.example/upload",
                Instant.parse("2026-07-27T01:10:00Z")
        ));

        mockMvc.perform(post("/api/evaluations/uploads")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileName": "recording.wav",
                                  "contentType": "audio/wav",
                                  "contentLength": 32044
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.objectKey").value("evaluation-audio/7/audio-id.wav"))
                .andExpect(jsonPath("$.uploadUrl").value("https://signed.example/upload"));

        verify(jobService).prepareUpload(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(new EvaluationUploadRequest(
                        "recording.wav",
                        "audio/wav",
                        32044
                ))
        );
    }

    @Test
    @DisplayName("업로드된 object key로 평가 작업을 만들고 즉시 202를 반환한다")
    void createsEvaluationJob() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer("Bearer access-token")).thenReturn(7L);
        when(jobService.createJob(any(), any(), any())).thenReturn(new EvaluationJobResponse(
                "job-id",
                EvaluationJob.Status.PENDING,
                null,
                null,
                Instant.parse("2026-07-27T01:00:00Z"),
                Instant.parse("2026-07-27T01:00:00Z")
        ));

        mockMvc.perform(post("/api/evaluations/jobs")
                        .header("Authorization", "Bearer access-token")
                        .header("Idempotency-Key", "evaluation-request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "objectKey": "evaluation-audio/7/audio-id.wav",
                                  "sentenceId": 12
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("job-id"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(jobService).createJob(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq("evaluation-request-1"),
                org.mockito.ArgumentMatchers.eq(new EvaluationJobRequest(
                        "evaluation-audio/7/audio-id.wav",
                        12L,
                        null
                ))
        );
    }

    @Test
    @DisplayName("인증 사용자는 자신이 만든 평가 작업 상태를 조회한다")
    void getsEvaluationJob() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer("Bearer access-token")).thenReturn(7L);
        when(jobService.getJob(7L, "job-id")).thenReturn(new EvaluationJobResponse(
                "job-id",
                EvaluationJob.Status.PROCESSING,
                null,
                null,
                Instant.parse("2026-07-27T01:00:00Z"),
                Instant.parse("2026-07-27T01:00:01Z")
        ));

        mockMvc.perform(get("/api/evaluations/jobs/job-id")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }
}
