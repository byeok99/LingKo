package com.lingko.lingko.core.domain.legal;

import com.lingko.lingko.core.domain.legal.entity.AiProcessingConsent;
import com.lingko.lingko.core.domain.legal.repository.AiProcessingConsentRepository;
import com.lingko.lingko.core.domain.legal.service.AiProcessingConsentService;
import com.lingko.lingko.core.domain.legal.service.AiConsentRequiredException;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 미동의·철회·고지 개정·재동의가 이전 작업의 전송 권한을 되살리지 않는지 검증한다. */
class AiProcessingConsentServiceTest {
    private final AiProcessingConsentRepository repository = mock(AiProcessingConsentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final Instant now = Instant.parse("2026-09-12T00:00:00Z");
    private final User user = User.builder().userIdx(7L).build();
    private AiProcessingConsentService service;

    @BeforeEach
    void setUp() {
        service = new AiProcessingConsentService(repository, users, Clock.fixed(now, ZoneOffset.UTC));
        when(users.existsById(7L)).thenReturn(true);
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(repository.findFirstByUserUserIdxOrderByIdDesc(7L)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void missingConsentDeniesTransmission() {
        assertThat(service.getStatus(7L).granted()).isFalse();
        assertThatThrownBy(() -> service.requireGranted(7L)).isInstanceOf(AiConsentRequiredException.class);
    }

    @Test
    void explicitGrantUsesServerTimeAndCurrentVersion() {
        var status = service.record(7L, true, AiProcessingConsentService.CURRENT_VERSION);
        assertThat(status.granted()).isTrue();
        assertThat(status.recordedAt()).isEqualTo(now);
        verify(repository).saveAndFlush(argThat(record -> record.isGranted()
                && record.getNoticeVersion().equals(AiProcessingConsentService.CURRENT_VERSION)));
    }

    @Test
    void obsoleteNoticeCannotBeAccepted() {
        assertThatThrownBy(() -> service.record(7L, true, "2026-08-07"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void withdrawalDeniesTransmission() {
        when(repository.findFirstByUserUserIdxOrderByIdDesc(7L)).thenReturn(Optional.of(
                AiProcessingConsent.record(user, AiProcessingConsentService.CURRENT_VERSION, false, now)));
        assertThatThrownBy(() -> service.requireGranted(7L)).isInstanceOf(AiConsentRequiredException.class);
    }

    @Test
    void newGrantDoesNotAuthorizeAnOldJob() {
        var latest = mock(AiProcessingConsent.class);
        when(latest.isGranted()).thenReturn(true);
        when(latest.getNoticeVersion()).thenReturn(AiProcessingConsentService.CURRENT_VERSION);
        when(latest.getId()).thenReturn(22L);
        when(repository.findFirstByUserUserIdxOrderByIdDesc(7L)).thenReturn(Optional.of(latest));
        assertThat(service.requireGranted(7L)).isEqualTo(22L);
        assertThatThrownBy(() -> service.requireJobConsent(7L, 21L)).isInstanceOf(AiConsentRequiredException.class);
        assertThatThrownBy(() -> service.requireJobConsent(7L, null)).isInstanceOf(AiConsentRequiredException.class);
        assertThatCode(() -> service.requireJobConsent(7L, 22L)).doesNotThrowAnyException();
    }
}
