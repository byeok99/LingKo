package com.lingko.lingko.core.domain.evaluation;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이미 생성된 가이드 영상이 기존 syllables 테이블의 초기 데이터로 배포되는 계약을 검증한다.
 */
class SyllableGuideSeedMigrationTest {

    @Test
    @DisplayName("가이드 seed migration은 기존 syllables 테이블에 생성 완료 영상을 넣는다")
    void seedsGeneratedGuideVideos() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:syllable_guide_seed;MODE=MySQL;DATABASE_TO_UPPER=false"
        )) {
            runMigration(connection, "V1__baseline_schema.sql");
            connection.createStatement().executeUpdate("""
                    INSERT INTO syllables (syllable_char, mouth_url, tongue_url)
                    VALUES
                        ('바', 'https://guides/mouth/static.png', NULL),
                        ('각', NULL, 'https://guides/tongue/newer.mp4')
                    """);
            runMigration(connection, "R__seed_generated_syllable_guides.sql");

            try (var statement = connection.prepareStatement("""
                    SELECT mouth_url, tongue_url
                    FROM syllables
                    WHERE syllable_char = ?
                    """)) {
                statement.setString(1, "바");
                try (var result = statement.executeQuery()) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString("mouth_url")).endsWith(".mp4");
                }

                statement.setString(1, "각");
                try (var result = statement.executeQuery()) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString("tongue_url")).endsWith(".mp4");
                    assertThat(result.getString("tongue_url")).endsWith("newer.mp4");
                }
            }
        }
    }

    private void runMigration(Connection connection, String filename) throws Exception {
        RunScript.execute(
                connection,
                new FileReader("src/main/resources/db/migration/" + filename, StandardCharsets.UTF_8)
        );
    }
}
