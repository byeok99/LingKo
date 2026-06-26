package com.lingko.lingko.infra.storage;

import com.lingko.lingko.core.config.AwsSettings;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalMediaUrlValidatorTest {

    @Test
    @DisplayName("허용된 HTTPS host와 공인 IP는 통과한다")
    void validateAllowsKnownHttpsHost() throws Exception {
        ExternalMediaUrlValidator validator = new ExternalMediaUrlValidator(
                host -> new InetAddress[]{uncheckedAddress("203.0.113.10")}
        );

        assertThatCode(() -> validator.validate("https://replicate.delivery/output/video.mp4"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("HTTP URL은 거부한다")
    void validateRejectsHttpUrl() {
        ExternalMediaUrlValidator validator = validatorWithPublicAddress();

        assertThatThrownBy(() -> validator.validate("http://replicate.delivery/output/video.mp4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS URL만 허용");
    }

    @Test
    @DisplayName("allowlist 밖 host는 거부한다")
    void validateRejectsUnknownHost() {
        ExternalMediaUrlValidator validator = validatorWithPublicAddress();

        assertThatThrownBy(() -> validator.validate("https://example.com/video.mp4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("허용되지 않은 외부 미디어 host");
    }

    @Test
    @DisplayName("허용 host라도 private IP로 해석되면 거부한다")
    void validateRejectsPrivateResolvedAddress() throws Exception {
        ExternalMediaUrlValidator validator = new ExternalMediaUrlValidator(
                host -> new InetAddress[]{uncheckedAddress("127.0.0.1")}
        );

        assertThatThrownBy(() -> validator.validate("https://replicate.delivery/output/video.mp4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("내부망 외부 미디어 URL");
    }

    @Test
    @DisplayName("설정된 S3 bucket과 region의 guide URL을 허용한다")
    void validateAllowsConfiguredS3Host() {
        ExternalMediaUrlValidator validator = new ExternalMediaUrlValidator(
                awsSettings("custom-bucket", "us-west-2"),
                host -> new InetAddress[]{uncheckedAddress("203.0.113.10")},
                url -> new FakeHttpURLConnection(url, 200, 1024)
        );

        assertThatCode(() -> validator.validate("https://custom-bucket.s3.us-west-2.amazonaws.com/guides/mouth/vowel-a.png"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("외부 미디어 URL 리다이렉트는 거부한다")
    void openConnectionRejectsRedirect() {
        ExternalMediaUrlValidator validator = new ExternalMediaUrlValidator(
                awsSettings("lingko", "ap-northeast-2"),
                host -> new InetAddress[]{uncheckedAddress("203.0.113.10")},
                url -> new FakeHttpURLConnection(url, 302, 0)
        );

        assertThatThrownBy(() -> validator.openConnection("https://replicate.delivery/output/video.mp4"))
                .isInstanceOf(VideoGenerationException.class)
                .hasMessageContaining("리다이렉트");
    }

    @Test
    @DisplayName("Content-Length가 제한을 초과하면 거부한다")
    void openConnectionRejectsLargeContentLength() {
        ExternalMediaUrlValidator validator = new ExternalMediaUrlValidator(
                awsSettings("lingko", "ap-northeast-2"),
                host -> new InetAddress[]{uncheckedAddress("203.0.113.10")},
                url -> new FakeHttpURLConnection(url, 200, ExternalMediaUrlValidator.MAX_DOWNLOAD_BYTES + 1)
        );

        assertThatThrownBy(() -> validator.openConnection("https://replicate.delivery/output/video.mp4"))
                .isInstanceOf(VideoGenerationException.class)
                .hasMessageContaining("크기 제한 초과");
    }

    private ExternalMediaUrlValidator validatorWithPublicAddress() {
        return new ExternalMediaUrlValidator(host -> new InetAddress[]{uncheckedAddress("203.0.113.10")});
    }

    private AwsSettings awsSettings(String bucket, String region) {
        AwsSettings settings = new AwsSettings();
        AwsSettings.S3 s3 = new AwsSettings.S3();
        s3.setBucket(bucket);
        s3.setRegion(region);
        settings.setS3(s3);
        return settings;
    }

    private static InetAddress uncheckedAddress(String ip) {
        try {
            return InetAddress.getByName(ip);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static class FakeHttpURLConnection extends HttpURLConnection {
        private final int responseCode;
        private final long contentLength;

        protected FakeHttpURLConnection(URL url, int responseCode, long contentLength) {
            super(url);
            this.responseCode = responseCode;
            this.contentLength = contentLength;
        }

        @Override
        public int getResponseCode() throws IOException {
            return responseCode;
        }

        @Override
        public long getContentLengthLong() {
            return contentLength;
        }

        @Override
        public void setRequestMethod(String method) throws ProtocolException {
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() throws IOException {
        }
    }
}
