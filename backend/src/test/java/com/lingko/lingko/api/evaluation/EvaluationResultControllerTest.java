package com.lingko.lingko.api.evaluation;

import com.lingko.lingko.api.evaluation.dto.ScoreStatus;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationApplicationService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Evaluation Result 컨트롤러 Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
@WebMvcTest(
        value = EvaluationResultController.class,
        properties = "evaluation.legacy-multipart-enabled=true"
)
class EvaluationResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EvaluationService evaluationService;

    @MockitoBean
    private EvaluationApplicationService evaluationApplicationService;

    @MockitoBean
    private ActiveSessionAuthenticator activeSessionAuthenticator;

    @Test
    @DisplayName("multipart audio와 text를 받아 발음 평가 결과를 반환한다")
    void evaluateWithText() throws Exception {
        MockMultipartFile audio = new MockMultipartFile(
                "audio",
                "recording.wav",
                "audio/wav",
                wavBytes(1)
        );
        PracticeResultResponse response = PracticeResultResponse.builder()
                .overallScore(87)
                .gradeLabel("Good")
                .summary("Good pronunciation.")
                .recognizedText("안녕하세요.")
                .characterScoreStatus(ScoreStatus.UNAVAILABLE)
                .scoreBreakdown(PracticeResultResponse.ScoreBreakdownResponse.builder()
                        .accuracy(88)
                        .fluency(86)
                        .completeness(90)
                        .build())
                .weakCharacters(List.of())
                .characters(List.of())
                .build();
        when(evaluationService.validateAudio(any()))
                .thenReturn(EvaluationService.AudioValidationStatus.VALID);
        when(activeSessionAuthenticator.authenticateBearer("Bearer valid-access-token")).thenReturn(7L);
        when(evaluationApplicationService.evaluate(eq(7L), any(), eq(null), eq("안녕하세요.")))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/evaluations")
                        .file(audio)
                        .param("text", "안녕하세요.")
                        .header("Authorization", "Bearer valid-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").value(87))
                .andExpect(jsonPath("$.gradeLabel").value("Good"))
                .andExpect(jsonPath("$.recognizedText").value("안녕하세요."))
                .andExpect(jsonPath("$.characterScoreStatus").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.scoreBreakdown.accuracy").value(88));
    }

    @Test
    @DisplayName("audio가 없으면 400을 반환한다")
    void audioIsRequired() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer("Bearer valid-access-token")).thenReturn(7L);

        mockMvc.perform(multipart("/api/evaluations")
                        .param("text", "안녕하세요.")
                        .header("Authorization", "Bearer valid-access-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("sentenceId와 text가 모두 없으면 공통 validation 에러를 반환한다")
    void sentenceIdOrTextIsRequired() throws Exception {
        MockMultipartFile audio = new MockMultipartFile(
                "audio",
                "recording.wav",
                "audio/wav",
                wavBytes(1)
        );

        when(activeSessionAuthenticator.authenticateBearer("Bearer valid-access-token")).thenReturn(7L);

        mockMvc.perform(multipart("/api/evaluations")
                        .file(audio)
                        .header("Authorization", "Bearer valid-access-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("sentenceId or text is required"))
                .andExpect(jsonPath("$.details.length()").value(0));
    }

    @Test
    @DisplayName("지원하지 않는 audio 형식이면 415를 반환한다")
    void unsupportedAudio() throws Exception {
        MockMultipartFile audio = new MockMultipartFile(
                "audio",
                "recording.mp3",
                "audio/mpeg",
                new byte[]{1, 2, 3}
        );
        when(activeSessionAuthenticator.authenticateBearer("Bearer valid-access-token")).thenReturn(7L);
        when(evaluationService.validateAudio(any()))
                .thenReturn(EvaluationService.AudioValidationStatus.UNSUPPORTED_TYPE);

        mockMvc.perform(multipart("/api/evaluations")
                        .file(audio)
                        .param("text", "안녕하세요.")
                        .header("Authorization", "Bearer valid-access-token"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    @DisplayName("WAV 확장자와 MIME이 맞아도 헤더가 잘못되면 415를 반환한다")
    void rejectsInvalidWavHeader() throws Exception {
        MockMultipartFile audio = new MockMultipartFile(
                "audio", "recording.wav", "audio/wav", new byte[]{1, 2, 3}
        );
        when(activeSessionAuthenticator.authenticateBearer("Bearer valid-access-token")).thenReturn(7L);
        when(evaluationService.validateAudio(any()))
                .thenReturn(EvaluationService.AudioValidationStatus.INVALID_WAV);

        mockMvc.perform(multipart("/api/evaluations")
                        .file(audio)
                        .param("text", "안녕하세요.")
                        .header("Authorization", "Bearer valid-access-token"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("INVALID_WAV"));
    }

    @Test
    @DisplayName("10 MiB를 초과한 오디오는 평가 전에 413을 반환한다")
    void rejectsOversizedAudio() throws Exception {
        MockMultipartFile audio = new MockMultipartFile(
                "audio",
                "recording.wav",
                "audio/wav",
                new byte[(int) EvaluationService.MAX_AUDIO_BYTES + 1]
        );

        when(activeSessionAuthenticator.authenticateBearer("Bearer valid-access-token")).thenReturn(7L);

        mockMvc.perform(multipart("/api/evaluations")
                        .file(audio)
                        .param("text", "안녕하세요.")
                        .header("Authorization", "Bearer valid-access-token"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("AUDIO_TOO_LARGE"));
    }

    @Test
    @DisplayName("평가 생성은 Authorization bearer token이 필요하다")
    void authorizationIsRequired() throws Exception {
        MockMultipartFile audio = new MockMultipartFile(
                "audio",
                "recording.wav",
                "audio/wav",
                wavBytes(1)
        );
        when(activeSessionAuthenticator.authenticateBearer(null))
                .thenThrow(new AuthException("Authorization header is required"));

        mockMvc.perform(multipart("/api/evaluations")
                        .file(audio)
                        .param("text", "안녕하세요."))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    private byte[] wavBytes(int dataLength) {
        byte[] bytes = new byte[44 + dataLength];
        writeAscii(bytes, 0, "RIFF");
        writeLittleEndianInt(bytes, 4, bytes.length - 8);
        writeAscii(bytes, 8, "WAVE");
        writeAscii(bytes, 12, "fmt ");
        writeLittleEndianInt(bytes, 16, 16);
        bytes[20] = 1;
        bytes[22] = 1;
        writeLittleEndianInt(bytes, 24, 16000);
        writeLittleEndianInt(bytes, 28, 32000);
        bytes[32] = 2;
        bytes[34] = 16;
        writeAscii(bytes, 36, "data");
        writeLittleEndianInt(bytes, 40, dataLength);
        return bytes;
    }

    private void writeAscii(byte[] target, int offset, String value) {
        for (int index = 0; index < value.length(); index++) {
            target[offset + index] = (byte) value.charAt(index);
        }
    }

    private void writeLittleEndianInt(byte[] target, int offset, int value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >>> 8);
        target[offset + 2] = (byte) (value >>> 16);
        target[offset + 3] = (byte) (value >>> 24);
    }
}
