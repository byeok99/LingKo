package com.lingko.lingko.core.util;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * 발음 가이드의 S3·job cache가 공유하는 내용 기반 식별자를 만든다.
 *
 * 음절 문자는 의도적으로 포함하지 않는다. 서로 다른 음절이라도 타입과 프레임 순서가 같으면
 * 시각 결과도 같으므로 한 영상을 공유해야 외부 생성 비용과 저장 중복을 피할 수 있다.
 */
public final class GuideMediaCacheKey {

    private GuideMediaCacheKey() {
    }

    /** 현재 매핑 버전과 전체 프레임 전이 순서를 포함한 완성 영상 식별자다. */
    public static String sequenceSignature(VideoType type, List<List<String>> urlPairs) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(urlPairs, "urlPairs");
        StringBuilder source = new StringBuilder()
                .append(GuideMediaVersion.CURRENT)
                .append("|sequence|")
                .append(type.name());
        urlPairs.forEach(pair -> source.append('|').append(String.join(",", pair)));
        return sha256(source.toString());
    }

    /** 한 방향의 이미지 보간 clip을 재사용하기 위한 전이 식별자다. */
    public static String segmentSignature(VideoType type, List<String> pair) {
        Objects.requireNonNull(type, "type");
        if (pair == null || pair.size() != 2) {
            throw new IllegalArgumentException("segment pair must contain exactly two frames");
        }
        return sha256(String.join(
                "|",
                GuideMediaVersion.CURRENT,
                "segment",
                type.name(),
                pair.get(0),
                pair.get(1)
        ));
    }

    /** S3 object 이름은 충돌 여유를 유지하면서 로그 가독성을 위해 SHA-256 앞 96bit를 사용한다. */
    public static String objectId(String signature) {
        if (signature == null || signature.length() < 24) {
            throw new IllegalArgumentException("cache signature must contain at least 24 characters");
        }
        return signature.substring(0, 24);
    }

    private static String sha256(String source) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
