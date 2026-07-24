package com.lingko.lingko.api.evaluation;

import com.lingko.lingko.api.evaluation.dto.PracticeHistoryResponse;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationHistoryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 인증된 활성 세션 사용자가 소유한 평가 기록을 page 단위로 제공한다.
 */
@Validated
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationHistoryController {

    private final EvaluationHistoryService historyService;
    private final ActiveSessionAuthenticator activeSessionAuthenticator;

    /**
     * 활성 Bearer 세션 사용자의 기록을 제한된 크기의 한 page로 반환한다.
     */
    @GetMapping("/me")
    public PracticeHistoryResponse getMyEvaluationHistory(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        return historyService.findHistory(
                activeSessionAuthenticator.authenticateBearer(authorization),
                page,
                size
        );
    }
}
