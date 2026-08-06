package com.lingko.lingko.api.evaluation;

import com.lingko.lingko.api.evaluation.dto.PracticeHistoryResponse;
import com.lingko.lingko.api.evaluation.dto.WeakWordListResponse;
import com.lingko.lingko.api.evaluation.dto.WordDetailResponse;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationHistoryService;
import com.lingko.lingko.core.domain.evaluation.service.WeakWordService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final WeakWordService weakWordService;
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

    /**
     * 반복해서 틀리는 어절을 평균 점수가 낮은 순으로 반환한다.
     *
     * Home은 상위 3개만 쓰지만 Word detail 진입 등 다른 화면이 더 넓은 목록을 필요로 할 수 있어
     * 개수를 요청 측이 정하게 두고 상한만 고정한다.
     */
    @GetMapping("/me/weak-words")
    public WeakWordListResponse getMyWeakWords(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "3") @Min(1) @Max(20) int limit
    ) {
        return weakWordService.findWeakWords(
                activeSessionAuthenticator.authenticateBearer(authorization),
                limit
        );
    }

    /**
     * 어절 하나의 누적 성적과 과거 시도·다음 후보를 한 번에 반환한다.
     *
     * 세 자료를 따로 조회하면 시점이 어긋나 머리말의 평균·횟수와 아래 목록이 맞지 않게 보인다.
     */
    @GetMapping("/me/words/{wordText}")
    public WordDetailResponse getMyWordDetail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable @NotBlank @Size(max = 100) String wordText
    ) {
        return weakWordService.findWordDetail(
                activeSessionAuthenticator.authenticateBearer(authorization),
                wordText
        );
    }
}
