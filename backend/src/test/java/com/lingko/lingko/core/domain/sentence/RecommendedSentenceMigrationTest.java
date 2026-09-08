package com.lingko.lingko.core.domain.sentence;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.lingko.lingko.core.util.PracticeSentenceNormalizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Recommended Sentence Migration Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
class RecommendedSentenceMigrationTest {

    @Test
    @DisplayName("확장 seed는 기존 문장을 보존하고 여섯 주제별 8개를 중복 없이 제공한다")
    void expandedCatalogPreservesExistingSentencesAndBalancesCategories() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:expanded_sentences;MODE=MySQL;DATABASE_TO_UPPER=false")) {
            applyMigration(connection, "V2__recommended_sentences.sql");
            applyMigration(connection, "V12__remove_recommended_pronunciation.sql");
            List<String> originalRows = snapshot(connection);
            applyMigration(connection, "V22__expand_recommended_sentences.sql");

            assertThat(snapshot(connection).subList(0, originalRows.size())).isEqualTo(originalRows);
            assertThat(snapshot(connection)).hasSize(48);
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("""
                         SELECT category_code, COUNT(*) AS total FROM recommended_sentences
                         WHERE active = TRUE GROUP BY category_code
                         """)) {
                var categories = new HashSet<String>();
                while (rows.next()) {
                    categories.add(rows.getString("category_code"));
                    assertThat(rows.getInt("total")).isEqualTo(8);
                }
                assertThat(categories).containsExactlyInAnyOrder(
                        "FOOD", "DAILY", "TRAVEL", "STUDY", "WORK", "HEALTH");
            }
            assertCatalogQuality(connection);
        }
    }

    // 기존 ID·표시 순서·번역·학습 목적이 seed 확장 때문에 달라지지 않는지 비교한다.
    private List<String> snapshot(Connection connection) throws Exception {
        var result = new ArrayList<String>();
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT * FROM recommended_sentences ORDER BY sentence_id")) {
            while (rows.next()) {
                var fields = new ArrayList<String>();
                for (int column = 1; column <= rows.getMetaData().getColumnCount(); column++) {
                    fields.add(rows.getString(column));
                }
                result.add(String.join("|", fields));
            }
        }
        return List.copyOf(result);
    }

    // 실제 API와 같은 정규화로 중복을 판정하고, 모든 콘텐츠가 현재 50개 조회 범위에 들어오는지 보장한다.
    private void assertCatalogQuality(Connection connection) throws Exception {
        var normalized = new HashSet<String>();
        var sortOrders = new HashSet<Integer>();
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT * FROM recommended_sentences")) {
            while (rows.next()) {
                String text = PracticeSentenceNormalizer.normalize(rows.getString("original_text"));
                assertThat(text).isNotBlank().hasSizeLessThanOrEqualTo(120);
                assertThat(normalized.add(text)).as("duplicate sentence: %s", text).isTrue();
                assertThat(rows.getString("translation")).isNotBlank();
                assertThat(rows.getString("learning_point")).isNotBlank();
                assertThat(rows.getString("level_label")).isIn("Beginner 1", "Beginner 2");
                assertThat(rows.getBoolean("active")).isTrue();
                assertThat(sortOrders.add(rows.getInt("sort_order"))).isTrue();
            }
        }
        assertThat(sortOrders).hasSizeLessThanOrEqualTo(50);
    }

    private void applyMigration(Connection connection, String file) throws Exception {
        try (var reader = new FileReader("src/main/resources/db/migration/" + file, StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        }
    }

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
