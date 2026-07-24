package com.lingko.lingko.infra.storage;

import com.lingko.lingko.core.config.AwsSettings;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * 외부 media URL을 애플리케이션이 사용하기 전에 검증하고 정규화한다.
 *
 * scheme·host·내부망 차단 정책을 호출부마다 반복하지 않도록 보안 경계로 분리했다.
 */
@Component
public class ExternalMediaUrlValidator {

    public static final int CONNECT_TIMEOUT_MS = 5_000;
    public static final int READ_TIMEOUT_MS = 10_000;
    public static final long MAX_DOWNLOAD_BYTES = 25L * 1024 * 1024;

    private static final Set<String> EXACT_ALLOWED_HOSTS = Set.of(
            "lingko.s3.ap-northeast-2.amazonaws.com",
            "replicate.delivery"
    );

    private final AwsSettings awsSettings;
    private final Function<String, InetAddress[]> addressResolver;
    private final Function<URL, HttpURLConnection> connectionFactory;

    @Autowired
    public ExternalMediaUrlValidator(AwsSettings awsSettings) {
        this(awsSettings, ExternalMediaUrlValidator::resolveAddresses, ExternalMediaUrlValidator::openHttpConnection);
    }

    ExternalMediaUrlValidator(Function<String, InetAddress[]> addressResolver) {
        this(null, addressResolver, ExternalMediaUrlValidator::openHttpConnection);
    }

    ExternalMediaUrlValidator(
            AwsSettings awsSettings,
            Function<String, InetAddress[]> addressResolver,
            Function<URL, HttpURLConnection> connectionFactory
    ) {
        this.awsSettings = awsSettings;
        this.addressResolver = addressResolver;
        this.connectionFactory = connectionFactory;
    }

    public void validate(String rawUrl) {
        URI uri = parse(rawUrl);
        validateScheme(uri);
        validateHost(uri.getHost());
        // 허용된 형태의 host도 DNS에서 내부 주소로 해석될 수 있어 host allowlist만으로는 충분하지 않다.
        validateResolvedAddresses(uri.getHost());
    }

    public HttpURLConnection openConnection(String rawUrl) {
        validate(rawUrl);
        try {
            URL url = parse(rawUrl).toURL();
            HttpURLConnection connection = connectionFactory.apply(url);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            // 허용 host가 downloader를 내부망으로 redirect하지 못하도록 redirect를 비활성화한다.
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
        if (getExactAllowedHosts().contains(normalizedHost)) {
            return;
        }
        // Replicate는 동적 하위 도메인을 사용하므로 통제된 이 접미사에만 와일드카드를 허용한다.
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

    private Set<String> getExactAllowedHosts() {
        Set<String> hosts = new HashSet<>(EXACT_ALLOWED_HOSTS);
        String configuredS3Host = getConfiguredS3Host();
        if (configuredS3Host != null) {
            hosts.add(configuredS3Host);
        }
        return hosts;
    }

    private String getConfiguredS3Host() {
        if (awsSettings == null
                || awsSettings.getS3() == null
                || isBlank(awsSettings.getS3().getBucket())
                || isBlank(awsSettings.getS3().getRegion())) {
            return null;
        }

        return String.format(
                "%s.s3.%s.amazonaws.com",
                awsSettings.getS3().getBucket(),
                awsSettings.getS3().getRegion()
        ).toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static InetAddress[] resolveAddresses(String host) {
        try {
            return InetAddress.getAllByName(host);
        } catch (IOException e) {
            throw new IllegalArgumentException("URL host를 확인할 수 없음: " + host, e);
        }
    }

    private static HttpURLConnection openHttpConnection(URL url) {
        try {
            return (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new VideoGenerationException("외부 미디어 URL 연결 실패: " + url, e);
        }
    }

    private boolean isPrivateAddress(InetAddress address) {
        // SSRF 변형을 차단하기 위해 IPv4 local range와 IPv6 unique-local 주소를 모두 거부한다.
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
