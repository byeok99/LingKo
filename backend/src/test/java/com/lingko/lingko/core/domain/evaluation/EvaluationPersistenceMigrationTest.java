package com.lingko.lingko.core.domain.evaluation;

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
 * Evaluation Persistence Migration Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
class EvaluationPersistenceMigrationTest {

    @Test
    @DisplayName("평가 저장 확장 migration은 평가 snapshot과 글자별 feedback 컬럼을 추가한다")
    void evaluationPersistenceMigrationAddsSnapshotColumns() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:evaluation_persistence;MODE=MySQL;DATABASE_TO_UPPER=false"
        )) {
            RunScript.execute(
                    connection,
                    new FileReader(
                            "src/main/resources/db/migration/V1__baseline_schema.sql",
                            StandardCharsets.UTF_8
                    )
            );
            RunScript.execute(
                    connection,
                    new FileReader(
                            "src/main/resources/db/migration/V3__extend_evaluation_persistence_model.sql",
                            StandardCharsets.UTF_8
                    )
            );

            assertColumn(connection, "evaluation_log", "source");
            assertColumn(connection, "evaluation_log", "sentence_id");
            assertColumn(connection, "evaluation_log", "standard_pronunciation");
            assertColumn(connection, "evaluation_log", "recognized_text");
            assertColumn(connection, "evaluation_log", "accuracy_score");
            assertColumn(connection, "evaluation_log", "fluency_score");
            assertColumn(connection, "evaluation_log", "completeness_score");
            assertColumn(connection, "evaluation_log", "pronunciation_score");
            assertColumn(connection, "evaluation_log", "audio_url");
            assertColumn(connection, "evaluation_syllable", "position_no");
            assertColumn(connection, "evaluation_syllable", "feedback");
            assertColumn(connection, "evaluation_syllable", "mouth_guide_url");
            assertColumn(connection, "evaluation_syllable", "tongue_guide_url");

            assertIndex(connection, "evaluation_log", "idx_evaluation_log_source_created");
            assertIndex(connection, "evaluation_log", "idx_evaluation_log_sentence_created");
            assertIndex(connection, "evaluation_syllable", "uk_evaluation_syllable_log_position");
        }
    }

    private void assertColumn(Connection connection, String tableName, String columnName) throws Exception {
        try (ResultSet resultSet = connection.getMetaData()
                .getColumns(null, null, tableName, columnName)) {
            assertThat(resultSet.next())
                    .as("column %s.%s exists", tableName, columnName)
                    .isTrue();
        }
    }

    private void assertIndex(Connection connection, String tableName, String indexName) throws Exception {
        try (ResultSet resultSet = connection.getMetaData()
                .getIndexInfo(null, null, tableName, false, false)) {
            boolean found = false;
            while (resultSet.next()) {
                if (indexName.equalsIgnoreCase(resultSet.getString("INDEX_NAME"))) {
                    found = true;
                    break;
                }
            }
            assertThat(found)
                    .as("index %s.%s exists", tableName, indexName)
                    .isTrue();
        }
    }
}
