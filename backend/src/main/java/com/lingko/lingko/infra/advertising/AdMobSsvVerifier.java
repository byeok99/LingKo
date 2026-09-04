package com.lingko.lingko.infra.advertising;

import com.lingko.lingko.core.domain.quota.exception.AdMobSsvVerificationException;
import com.lingko.lingko.core.domain.quota.service.VerifiedAdRewardCallback;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** 원문 AdMob query를 변경하지 않고 ECDSA-SHA256 서명을 검증한다. */
@Component
public class AdMobSsvVerifier {

    private static final int MAX_QUERY_LENGTH = 4096;
    private static final String SIGNATURE_MARKER = "&signature=";
    private static final String KEY_ID_MARKER = "&key_id=";

    private final AdMobPublicKeyProvider publicKeyProvider;

    public AdMobSsvVerifier(AdMobPublicKeyProvider publicKeyProvider) {
        this.publicKeyProvider = publicKeyProvider;
    }

    /** signature 앞의 raw substring만 검증해야 percent encoding과 parameter 순서가 보존된다. */
    public VerifiedAdRewardCallback verify(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank() || rawQuery.length() > MAX_QUERY_LENGTH) {
            throw invalid("Invalid SSV query");
        }

        int signatureIndex = rawQuery.lastIndexOf(SIGNATURE_MARKER);
        int keyIdIndex = rawQuery.lastIndexOf(KEY_ID_MARKER);
        if (signatureIndex <= 0
                || keyIdIndex <= signatureIndex + SIGNATURE_MARKER.length()
                || rawQuery.indexOf(SIGNATURE_MARKER) != signatureIndex
                || rawQuery.indexOf(KEY_ID_MARKER) != keyIdIndex) {
            throw invalid("Invalid SSV signature parameters");
        }

        String signedContent = rawQuery.substring(0, signatureIndex);
        String encodedSignature = rawQuery.substring(
                signatureIndex + SIGNATURE_MARKER.length(),
                keyIdIndex
        );
        String rawKeyId = rawQuery.substring(keyIdIndex + KEY_ID_MARKER.length());
        if (encodedSignature.isBlank() || rawKeyId.isBlank() || rawKeyId.indexOf('&') >= 0) {
            throw invalid("Invalid SSV signature parameters");
        }

        long keyId;
        byte[] signature;
        try {
            keyId = Long.parseUnsignedLong(rawKeyId);
            signature = Base64.getUrlDecoder().decode(encodedSignature);
        } catch (IllegalArgumentException exception) {
            throw new AdMobSsvVerificationException("Invalid SSV signature encoding", exception);
        }

        verifySignature(signedContent, signature, publicKeyProvider.getKey(keyId));
        Map<String, String> values = parseUniqueValues(signedContent);

        return new VerifiedAdRewardCallback(
                required(values, "ad_unit"),
                required(values, "custom_data"),
                parsePositiveInt(required(values, "reward_amount"), "reward_amount"),
                required(values, "reward_item"),
                parsePositiveLong(required(values, "timestamp"), "timestamp"),
                required(values, "transaction_id")
        );
    }

    private void verifySignature(String content, byte[] rawSignature, PublicKey publicKey) {
        if (publicKey == null) {
            throw invalid("Unknown SSV key");
        }
        try {
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(publicKey);
            verifier.update(content.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(rawSignature)) {
                throw invalid("Invalid SSV signature");
            }
        } catch (GeneralSecurityException exception) {
            throw new AdMobSsvVerificationException("Unable to verify SSV signature", exception);
        }
    }

    private Map<String, String> parseUniqueValues(String rawContent) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : rawContent.split("&", -1)) {
            int separator = pair.indexOf('=');
            if (separator <= 0) {
                throw invalid("Invalid SSV parameter");
            }
            String name = decode(pair.substring(0, separator));
            String value = decode(pair.substring(separator + 1));
            if (values.putIfAbsent(name, value) != null) {
                throw invalid("Duplicate SSV parameter");
            }
        }
        return values;
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new AdMobSsvVerificationException("Invalid SSV percent encoding", exception);
        }
    }

    private String required(Map<String, String> values, String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw invalid("Missing SSV parameter");
        }
        return value;
    }

    private int parsePositiveInt(String value, String name) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new NumberFormatException(name);
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new AdMobSsvVerificationException("Invalid SSV numeric parameter", exception);
        }
    }

    private long parsePositiveLong(String value, String name) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                throw new NumberFormatException(name);
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new AdMobSsvVerificationException("Invalid SSV numeric parameter", exception);
        }
    }

    private AdMobSsvVerificationException invalid(String message) {
        return new AdMobSsvVerificationException(message);
    }
}
