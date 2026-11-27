package com.lingko.lingko.api.testsupport;

import com.lingko.lingko.core.domain.evaluation.service.E2eEvaluationSeedService;
import com.lingko.lingko.core.domain.evaluation.service.E2eEvaluationSeedService.SeedRecord;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * E2E 테스트가 고정 테스트 사용자({@code e2e-user-1})의 연습 기록을 비우거나 "평가가 끝난" 기록을 미리 넣게 한다.
 *
 * <p>기록 탭·상세·취약 음절·그래프 TC는 과거 기록이 있어야 하는데, 로컬 E2E는 S3·Azure가 없어 실제 평가로 기록을 만들 수 없다.
 * 실제 평가 흐름(업로드·평가 작업·기회 차감)은 거치지 않고 결과 행만 만든다. 그래서 이 기능으로는 기록을 "읽는" 화면만
 * 검증할 수 있고, 평가 자체는 검증하지 않는다. 세션·quota endpoint와 같은 이유로 {@code e2e} 프로필에서만 bean으로
 * 등록되며, 프로필이 없으면 경로가 404다. 고정 사용자가 없으면(세션을 발급한 적 없음) 409다.</p>
 */
@Profile("e2e")
@RestController
@RequestMapping("/test-support/evaluations")
@RequiredArgsConstructor
class E2eEvaluationController {

    private final UserRepository userRepository;
    private final E2eEvaluationSeedService seedService;

    /** 고정 사용자의 연습 기록을 모두 지운다. 평가 기회와 평가 작업은 그대로다. */
    @DeleteMapping
    public ResponseEntity<ClearResponse> clear() {
        return findE2eUser()
                .map(user -> ResponseEntity.ok(new ClearResponse(seedService.clear(user.getUserIdx()))))
                .orElseGet(() -> ResponseEntity.status(409).build());
    }

    /**
     * records를 오래된 것부터 최신 순으로 받아 기록을 만들고, 만든 기록 id를 같은 순서로 돌려준다.
     * 기존 기록은 지우지 않으므로 정확한 상태가 필요하면 먼저 {@link #clear()}를 부른다.
     * 비활성·없는 sentenceId, 표준 발음 어절 수와 다른 wordScores 개수는 400이다.
     */
    @PostMapping
    public ResponseEntity<SeedResponse> seed(@Valid @RequestBody SeedRequest request) {
        return findE2eUser()
                .map(user -> ResponseEntity.ok(new SeedResponse(seedService.seed(user.getUserIdx(),
                        request.records().stream().map(SeedRecordRequest::toRecord).toList()))))
                .orElseGet(() -> ResponseEntity.status(409).build());
    }

    private java.util.Optional<User> findE2eUser() {
        return userRepository.findBySocialIdAndSocialType(E2eSessionController.E2E_SOCIAL_ID, User.SocialType.GOOGLE);
    }

    /** 한 번에 만들 기록 목록. 오래된 것부터 최신 순이다. */
    record SeedRequest(@NotEmpty @Size(max = 50) List<@Valid @NotNull SeedRecordRequest> records) {
    }

    /**
     * 기록 하나. sentenceId가 있으면 추천 문장 기록이고 text는 무시한다. 없으면 text로 만든 자유 문장 기록이다.
     * standardPronunciation이 null이면 현재 규칙으로 계산하고, 값을 주면 과거 snapshot처럼 그대로 저장한다.
     * wordScores는 표준 발음을 공백으로 나눈 어절마다의 점수이며, null 원소는 "신뢰할 수 없는 점수"(화면 '—')다.
     */
    record SeedRecordRequest(
            Long sentenceId,
            @Size(max = 100) String text,
            @Size(max = 300) String standardPronunciation,
            @Min(0) @Max(100) int score,
            @Min(0) @Max(100) int accuracy,
            @Min(0) @Max(100) int fluency,
            @Min(0) @Max(100) int completeness,
            @NotEmpty List<@Min(0) @Max(100) Integer> wordScores
    ) {
        SeedRecord toRecord() {
            if (sentenceId == null && (text == null || text.isBlank())) {
                throw new IllegalArgumentException("sentenceId or text is required");
            }
            return new SeedRecord(sentenceId, text, standardPronunciation,
                    score, accuracy, fluency, completeness, wordScores);
        }
    }

    record ClearResponse(int deleted) {
    }

    record SeedResponse(List<Long> evaluationLogIds) {
    }
}
