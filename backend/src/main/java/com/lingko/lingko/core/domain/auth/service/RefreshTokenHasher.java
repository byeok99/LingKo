package com.lingko.lingko.core.domain.auth.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 갱신 토큰을 DB 저장 전에 복원 불가능한 fingerprint로 변환한다.
 */
@Component
public class RefreshTokenHasher {

    /**
     * 갱신 Session row에 저장할 안정적인 SHA-256 값을 생성한다.
     */
    public String hash(String refreshToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * 데이터에 따라 조기 종료하지 않는 방식으로 토큰 fingerprint를 비교한다.
     */
    public boolean matches(String refreshToken, String expectedHash) {
        byte[] actual = hash(refreshToken).getBytes(StandardCharsets.US_ASCII);
        byte[] expected = expectedHash.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(actual, expected);
    }
}
