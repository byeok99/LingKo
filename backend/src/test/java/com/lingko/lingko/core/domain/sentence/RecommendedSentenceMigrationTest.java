package com.lingko.lingko.core.domain.sentence;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Recommended Sentence Migration Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
class RecommendedSentenceMigrationTest {

    @Test
    @DisplayName("추천 문장 migration은 테이블과 MVP seed 20개 이상을 생성한다")
    void recommendedSentenceMigrationCreatesSeedData() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:recommended_sentences;MODE=MySQL;DATABASE_TO_UPPER=false"
        )) {
            RunScript.execute(
                    connection,
                    new FileReader(
                            "src/main/resources/db/migration/V2__recommended_sentences.sql",
                            StandardCharsets.UTF_8
                    )
            );
            RunScript.execute(
                    connection,
                    new FileReader(
                            "src/main/resources/db/migration/V12__remove_recommended_pronunciation.sql",
                            StandardCharsets.UTF_8
                    )
            );

            try (ResultSet resultSet = connection.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM recommended_sentences WHERE active = TRUE")) {
                resultSet.next();
                assertThat(resultSet.getInt(1)).isGreaterThanOrEqualTo(20);
            }

            try (ResultSet resultSet = connection.createStatement()
                    .executeQuery("SELECT category_code, original_text FROM recommended_sentences WHERE sentence_id = 1")) {
                resultSet.next();
                assertThat(resultSet.getString("category_code")).isEqualTo("FOOD");
                assertThat(resultSet.getString("original_text")).isEqualTo("맛있겠다.");
            }

            assertThat(connection.getMetaData().getColumns(
                    null,
                    null,
                    "recommended_sentences",
                    "standard_pronunciation"
            ).next()).isFalse();
        }
    }
}
