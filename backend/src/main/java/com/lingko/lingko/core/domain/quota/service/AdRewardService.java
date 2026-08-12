package com.lingko.lingko.core.domain.quota.service;

import com.lingko.lingko.api.quota.dto.AdRewardSessionResponse;
import com.lingko.lingko.api.quota.dto.AdRewardSessionStatusResponse;
import com.lingko.lingko.core.config.AdMobSsvSettings;
import com.lingko.lingko.core.domain.quota.entity.AdRewardSession;
import com.lingko.lingko.core.domain.quota.entity.AdRewardSessionStatus;
import com.lingko.lingko.core.domain.quota.exception.AdMobSsvVerificationException;
import com.lingko.lingko.core.domain.quota.exception.AdRewardSessionNotFoundException;
import com.lingko.lingko.core.domain.quota.exception.AdRewardUnavailableException;
import com.lingko.lingko.core.domain.quota.repository.AdRewardReceiptRepository;
import com.lingko.lingko.core.domain.quota.repository.AdRewardSessionRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;

/** 1회성 앱 세션과 검증된 Google SSV callback 사이의 보상 상태 전이를 조율한다. */
@Service
public class AdRewardService {

    private static final int TOKEN_BYTES = 32;

    private final AdRewardSessionRepository sessionRepository;
    private final AdRewardReceiptRepository receiptRepository;
    private final PracticeQuotaService quotaService;
    private final AdMobSsvSettings settings;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public AdRewardService(
            AdRewardSessionRepository sessionRepository,
            AdRewardReceiptRepository receiptRepository,
            PracticeQuotaService quotaService,
            AdMobSsvSettings settings,
            ObjectProvider<Clock> clockProvider
    ) {
        this.sessionRepository = sessionRepository;
        this.receiptRepository = receiptRepository;
        this.quotaService = quotaService;
        this.settings = settings;
        this.clock = clockProvider.getIfAvailable(Clock::systemUTC);
    }

    /** 새 광고를 열기 직전에만 raw token을 반환하고 저장소에는 hash만 남긴다. */
    @Transactional
    public AdRewardSessionResponse createSession(Long userId) {
        validateOperationalSettings();
        // callback도 session→quota 순서로 잠그므로 생성 경로도 같은 순서를 지켜 deadlock을 피한다.
        sessionRepository.deleteAllByUserIdAndStatus(userId, AdRewardSessionStatus.PENDING);
        if (quotaService.getTodayQuota(userId).remainingPractices()
                >= PracticeQuotaService.MAX_NATURAL_PRACTICES) {
            throw new AdRewardUnavailableException("Ad reward is not available at maximum quota");
        }

        String token = newToken();
        Instant expiresAt = clock.instant().plus(Duration.ofMinutes(settings.getSessionExpiryMinutes()));
        sessionRepository.save(AdRewardSession.pending(userId, hash(token), expiresAt));
        return new AdRewardSessionResponse(
                token,
                OffsetDateTime.ofInstant(expiresAt, PracticeQuotaService.SERVICE_ZONE)
        );
    }

    @Transactional
    public AdRewardSessionStatusResponse getSessionStatus(Long userId, String token) {
        if (token == null || token.isBlank() || token.length() > 128) {
            throw new AdRewardSessionNotFoundException();
        }
        AdRewardSession session = sessionRepository.findByUserIdAndSessionTokenHash(userId, hash(token))
                .orElseThrow(AdRewardSessionNotFoundException::new);
        if (session.isExpiredAt(clock.instant())) {
            session.expire();
        }
        return new AdRewardSessionStatusResponse(session.getStatus(), session.getCredited());
    }

    /** 서명 검증 이후에도 LingKo가 허용한 광고 단위·보상 정책과 session 소유권을 확인한다. */
    @Transactional
    public void processVerifiedCallback(VerifiedAdRewardCallback callback) {
        validateCallbackPolicy(callback);
        AdRewardSession session = sessionRepository.findByTokenHashForUpdate(hash(callback.customData()))
                .orElse(null);
        if (session == null || session.getStatus() != AdRewardSessionStatus.PENDING) {
            return;
        }
        if (session.isExpiredAt(clock.instant())) {
            session.expire();
            return;
        }
        if (receiptRepository.existsByProviderTransactionId(callback.transactionId())) {
            session.expire();
            return;
        }

        boolean credited = quotaService.grantVerifiedAdReward(
                session.getUserId(),
                callback.transactionId()
        );
        session.complete(callback.transactionId(), credited, clock.instant());
    }

    private void validateOperationalSettings() {
        if (!settings.isEnabled()
                || settings.allowedAdUnitIdSet().isEmpty()
                || settings.getRewardItem() == null
                || settings.getRewardItem().isBlank()) {
            throw new AdRewardUnavailableException("AdMob SSV is not configured");
        }
    }

    private void validateCallbackPolicy(VerifiedAdRewardCallback callback) {
        validateOperationalSettings();
        if (callback == null
                || !settings.allowedAdUnitIdSet().contains(callback.adUnitId())
                || settings.getRewardAmount() != callback.rewardAmount()
                || !settings.getRewardItem().equals(callback.rewardItem())
                || callback.transactionId().length() > 80
                || callback.customData().length() > 128) {
            throw new AdMobSsvVerificationException("SSV reward policy mismatch");
        }
    }

    private String newToken() {
        byte[] token = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
