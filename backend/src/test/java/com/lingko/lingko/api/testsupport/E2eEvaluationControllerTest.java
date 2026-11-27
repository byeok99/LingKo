package com.lingko.lingko.api.testsupport;

import com.lingko.lingko.core.domain.evaluation.service.E2eEvaluationSeedService;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 고정 E2E 사용자가 없을 때 연습 기록 시드가 409로 응답해, 프로필이 꺼진 경우의 404와 구분되는지 검증한다.
 * 통합 테스트는 H2 DB를 다른 클래스와 공유해 "사용자 없음" 상태를 확실히 만들 수 없으므로 단위 테스트로 둔다.
 */
@ExtendWith(MockitoExtension.class)
class E2eEvaluationControllerTest {

    @Mock private UserRepository userRepository;
    @Mock private E2eEvaluationSeedService seedService;
    @InjectMocks private E2eEvaluationController controller;

    @Test
    @DisplayName("고정 사용자가 없으면 기록 비우기와 넣기 모두 409이고 기록을 건드리지 않는다")
    void returnsConflictWhenE2eUserMissing() {
        when(userRepository.findBySocialIdAndSocialType(any(), any(User.SocialType.class)))
                .thenReturn(Optional.empty());
        E2eEvaluationController.SeedRequest request = new E2eEvaluationController.SeedRequest(List.of(
                new E2eEvaluationController.SeedRecordRequest(null, "감사합니다", null, 60, 60, 60, 60, List.of(60))));

        assertThat(controller.clear().getStatusCode().value()).isEqualTo(409);
        assertThat(controller.seed(request).getStatusCode().value()).isEqualTo(409);
        verify(seedService, never()).clear(anyLong());
        verify(seedService, never()).seed(anyLong(), anyList());
    }
}
