package com.lingko.lingko.infra.advertising;

import com.lingko.lingko.core.domain.quota.exception.AdMobSsvVerificationException;
import com.lingko.lingko.core.domain.quota.service.VerifiedAdRewardCallback;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Google SSV 원문 보존·ECDSA 검증·필수 parameter 계약을 고정한다. */
class AdMobSsvVerifierTest {

    @Test
    @DisplayName("원문 query의 ECDSA-SHA256 서명이 유효하면 검증된 callback만 반환한다")
    void verifiesSignedCallback() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String content = "ad_network=5450213213286189855&ad_unit=3927267131"
                + "&custom_data=session%2Dtoken&reward_amount=1"
                + "&reward_item=pronunciation_chance&timestamp=1786500000000"
                + "&transaction_id=18fa792de1bca816048293fc71035638";
        String rawQuery = signedQuery(content, 1234L, keyPair);
        AdMobSsvVerifier verifier = new AdMobSsvVerifier(keyId -> keyPair.getPublic());

        VerifiedAdRewardCallback callback = verifier.verify(rawQuery);

        assertThat(callback.adUnitId()).isEqualTo("3927267131");
        assertThat(callback.customData()).isEqualTo("session-token");
        assertThat(callback.rewardAmount()).isEqualTo(1);
        assertThat(callback.rewardItem()).isEqualTo("pronunciation_chance");
        assertThat(callback.transactionId()).isEqualTo("18fa792de1bca816048293fc71035638");
    }

    @Test
    @DisplayName("서명된 query의 값이나 순서를 바꾸면 callback을 거부한다")
    void rejectsTamperedCallback() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String content = "ad_network=1&ad_unit=3927267131&custom_data=session-token"
                + "&reward_amount=1&reward_item=pronunciation_chance"
                + "&timestamp=1786500000000&transaction_id=abc123";
        String tampered = signedQuery(content, 1234L, keyPair)
                .replace("reward_amount=1", "reward_amount=9");
        AdMobSsvVerifier verifier = new AdMobSsvVerifier(keyId -> keyPair.getPublic());

        assertThatThrownBy(() -> verifier.verify(tampered))
                .isInstanceOf(AdMobSsvVerificationException.class);
    }

    @Test
    @DisplayName("signature와 key_id가 마지막 순서가 아니거나 필수 값이 중복되면 거부한다")
    void rejectsAmbiguousQuery() throws Exception {
        KeyPair keyPair = generateKeyPair();
        AdMobSsvVerifier verifier = new AdMobSsvVerifier(keyId -> keyPair.getPublic());
        String content = "ad_unit=3927267131&custom_data=a&custom_data=b&reward_amount=1"
                + "&reward_item=pronunciation_chance&timestamp=1786500000000&transaction_id=abc";

        assertThatThrownBy(() -> verifier.verify(signedQuery(content, 1234L, keyPair)))
                .isInstanceOf(AdMobSsvVerificationException.class);
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        return generator.generateKeyPair();
    }

    private String signedQuery(String content, long keyId, KeyPair keyPair) throws Exception {
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(content.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
        return content + "&signature=" + signature + "&key_id=" + keyId;
    }
}
