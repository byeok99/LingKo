package com.lingko.lingko.core.domain.legal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * 서빙되는 문서 사본이 저장소 원본과 같은 내용인지 확인한다.
 *
 * <p>Docker 빌드 컨텍스트가 {@code backend/}만 포함하므로 빌드 시점에 {@code docs/legal/}을
 * 참조할 수 없다. 그래서 사본을 리소스에 두는데, 사본은 원본이 개정될 때 조용히 뒤처진다.
 * 이용자에게 옛 약관이 보이는 상황은 문서를 고치지 않은 것보다 나쁘므로 테스트로 막는다.
 *
 * <p>이 테스트는 저장소를 checkout한 환경에서만 의미가 있다. Docker 빌드는 {@code bootJar}만
 * 실행하고 테스트를 돌리지 않으므로 배포를 막지 않는다.
 */
class LegalDocumentSourceSyncTest {

    /** 저장소의 문서 원본 위치다. 테스트는 backend 모듈을 작업 디렉터리로 실행된다. */
    private static final Path SOURCE_DIRECTORY = Path.of("..", "docs", "legal");

    @Test
    void 리소스_사본은_docs_legal_원본과_같아야_한다() throws IOException {
        for (LegalDocument document : LegalDocument.values()) {
            for (LegalLanguage language : LegalLanguage.values()) {
                String resourcePath = document.resourcePath(language);
                String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
                Path source = SOURCE_DIRECTORY.resolve(fileName);

                assertThat(source)
                        .as("원본 문서가 있어야 한다: %s", source)
                        .exists();

                String expected = Files.readString(source, StandardCharsets.UTF_8);
                String actual = readResource(resourcePath);

                assertThat(actual)
                        .as(
                                "%s가 원본과 다르다. docs/legal의 문서를 고쳤다면 "
                                        + "backend/src/main/resources/legal로 복사해야 한다.",
                                resourcePath
                        )
                        .isEqualTo(expected);
            }
        }
    }

    private String readResource(String resourcePath) throws IOException {
        try (InputStream input = new ClassPathResource(resourcePath).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
