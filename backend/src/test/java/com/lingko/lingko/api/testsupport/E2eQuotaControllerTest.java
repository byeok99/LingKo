package com.lingko.lingko.api.testsupport;

import com.lingko.lingko.core.domain.quota.service.E2eQuotaSetupService;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 고정 E2E 사용자가 없을 때 409로 응답해, 프로필이 꺼진 경우의 404와 구분되는지 검증한다.
 * 통합 테스트는 H2 DB를 다른 클래스와 공유해서 "사용자 없음" 상태를 확실히 만들 수 없으므로 단위 테스트로 둔다.
 */
@ExtendWith(MockitoExtension.class)
class E2eQuotaControllerTest {

    @Mock private UserRepository userRepository;
    @Mock private E2eQuotaSetupService quotaSetupService;
    @InjectMocks private E2eQuotaController controller;

    @Test
    @DisplayName("세션을 발급한 적 없어 고정 사용자가 없으면 409이고 quota를 바꾸지 않는다")
    void returnsConflictWhenE2eUserMissing() {
        when(userRepository.findBySocialIdAndSocialType(any(), any(User.SocialType.class)))
                .thenReturn(Optional.empty());

        assertThat(controller.setQuota(3).getStatusCode().value()).isEqualTo(409);
        verify(quotaSetupService, never()).setRemaining(anyLong(), anyInt());
    }

    @Test
    @DisplayName("고정 사용자가 없으면 충전 시각 이동도 409이고 quota를 바꾸지 않는다")
    void moveNextRefillReturnsConflictWhenE2eUserMissing() {
        when(userRepository.findBySocialIdAndSocialType(any(), any(User.SocialType.class)))
                .thenReturn(Optional.empty());

        assertThat(controller.moveNextRefill(-1).getStatusCode().value()).isEqualTo(409);
        verify(quotaSetupService, never()).moveNextRefill(anyLong(), any());
    }
}
