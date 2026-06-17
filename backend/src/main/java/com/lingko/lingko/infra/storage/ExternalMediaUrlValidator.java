package com.lingko.lingko.infra.storage;

import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.Set;
import java.util.function.Function;

@Component
public class ExternalMediaUrlValidator {

    public static final int CONNECT_TIMEOUT_MS = 5_000;
    public static final int READ_TIMEOUT_MS = 10_000;
    public static final long MAX_DOWNLOAD_BYTES = 25L * 1024 * 1024;

    private static final Set<String> EXACT_ALLOWED_HOSTS = Set.of(
            "lingko.s3.ap-northeast-2.amazonaws.com",
            "replicate.delivery"
    );

    private final Function<String, InetAddress[]> addressResolver;

    public ExternalMediaUrlValidator() {
        this(ExternalMediaUrlValidator::resolveAddresses);
    }

    ExternalMediaUrlValidator(Function<String, InetAddress[]> addressResolver) {
        this.addressResolver = addressResolver;
    }

    public void validate(String rawUrl) {
        URI uri = parse(rawUrl);
        validateScheme(uri);
        validateHost(uri.getHost());
        validateResolvedAddresses(uri.getHost());
    }

    public HttpURLConnection openConnection(String rawUrl) {
        validate(rawUrl);
        try {
            URL url = parse(rawUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);
            int responseCode = connection.getResponseCode();
            if (responseCode >= 300 && responseCode < 400) {
                throw new VideoGenerationException("외부 미디어 URL 리다이렉트는 허용되지 않음: " + rawUrl);
            }
            if (responseCode < 200 || responseCode >= 300) {
                throw new VideoGenerationException("외부 미디어 다운로드 실패: HTTP " + responseCode);
            }
            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_DOWNLOAD_BYTES) {
                throw new VideoGenerationException("외부 미디어 크기 제한 초과: " + contentLength);
            }
            return connection;
        } catch (IOException e) {
            throw new VideoGenerationException("외부 미디어 URL 연결 실패: " + rawUrl, e);
        }
    }

    private URI parse(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("URL이 비어있음");
        }
        try {
            return URI.create(rawUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 URL: " + rawUrl, e);
        }
    }

    private void validateScheme(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("HTTPS URL만 허용됨: " + uri);
        }
    }

    private void validateHost(String host) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL host가 비어있음");
        }
        String normalizedHost = host.toLowerCase();
        if (EXACT_ALLOWED_HOSTS.contains(normalizedHost)) {
            return;
        }
        if (normalizedHost.endsWith(".replicate.delivery")) {
            return;
        }
        throw new IllegalArgumentException("허용되지 않은 외부 미디어 host: " + host);
    }

    private void validateResolvedAddresses(String host) {
        for (InetAddress address : addressResolver.apply(host)) {
            if (isPrivateAddress(address)) {
                throw new IllegalArgumentException("내부망 외부 미디어 URL은 허용되지 않음: " + host);
            }
        }
    }

    private static InetAddress[] resolveAddresses(String host) {
        try {
            return InetAddress.getAllByName(host);
        } catch (IOException e) {
            throw new IllegalArgumentException("URL host를 확인할 수 없음: " + host, e);
        }
    }

    private boolean isPrivateAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (bytes.length == 16) {
            int firstByte = bytes[0] & 0xff;
            return (firstByte & 0xfe) == 0xfc;
        }
        return false;
    }
}
