package com.lingko.lingko.infra.storage;

import com.lingko.lingko.core.config.AwsSettings;
import com.lingko.lingko.core.config.EvaluationJobSettings;
import com.lingko.lingko.core.config.S3Config;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/** EC2 Instance Role로 presigned PUT·Worker 다운로드·탈퇴 삭제 전체 S3 계약을 검증한다. */
@Tag("s3-external")
class S3InstanceRoleExternalIntegrationTest {

    @Test
    void completesAudioLifecycleWithDefaultCredentialChain() throws Exception {
        AwsSettings settings = new AwsSettings();
        AwsSettings.S3 s3 = new AwsSettings.S3();
        s3.setBucket(System.getenv("AWS_S3_BUCKET"));
        s3.setRegion(System.getenv("AWS_S3_REGION"));
        settings.setS3(s3);
        EvaluationJobSettings jobSettings = new EvaluationJobSettings();
        jobSettings.setUploadUrlExpireMinutes(2);
        long userId = ThreadLocalRandom.current().nextLong(1_000_000_000L, Long.MAX_VALUE);
        byte[] wav = new byte[44];

        try (S3Client client = new S3Config(settings).s3Client();
             S3Presigner presigner = new S3Config(settings).s3Presigner()) {
            S3EvaluationAudioStorage storage = new S3EvaluationAudioStorage(
                    client, presigner, settings, jobSettings, Clock.systemUTC());
            var ticket = storage.prepareUpload(userId, "probe.wav", "audio/wav", wav.length);
            HttpRequest upload = HttpRequest.newBuilder(URI.create(ticket.uploadUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "audio/wav")
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(wav))
                    .build();

            HttpResponse<Void> response = HttpClient.newHttpClient()
                    .send(upload, HttpResponse.BodyHandlers.discarding());
            assertThat(response.statusCode()).isBetween(200, 299);
            storage.validateUploaded(userId, ticket.objectKey());
            var downloaded = storage.download(ticket.objectKey());
            try {
                assertThat(Files.readAllBytes(downloaded)).containsExactly(wav);
            } finally {
                storage.deleteLocal(downloaded);
            }
            assertThat(storage.deleteAllForUser(userId)).isGreaterThanOrEqualTo(1);
        }
    }
}
