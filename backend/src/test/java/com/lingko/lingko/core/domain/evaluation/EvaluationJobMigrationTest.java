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
 * 비동기 평가 작업 migration이 재시작·중복 방지에 필요한 구조를 생성하는지 검증한다.
 */
class EvaluationJobMigrationTest {

    @Test
    @DisplayName("평가 작업 migration은 상태·lease·결과와 사용자별 Idempotency 제약을 추가한다")
    void createsDurableEvaluationJobs() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:evaluation_job_migration;MODE=MySQL;DATABASE_TO_UPPER=false"
        )) {
            runMigration(connection, "V1__baseline_schema.sql");
            runMigration(connection, "V8__add_evaluation_jobs.sql");
            runMigration(connection, "V9__add_evaluation_job_cleanup_index.sql");
            runMigration(connection, "V10__add_evaluation_job_queue_dispatch.sql");
            runMigration(connection, "V11__remove_evaluation_job_queue_dispatch.sql");

            assertColumn(connection, "evaluation_jobs", "status");
            assertColumn(connection, "evaluation_jobs", "lease_expires_at");
            assertColumn(connection, "evaluation_jobs", "result_payload");
            assertColumnMissing(connection, "evaluation_jobs", "enqueued_at");
            assertUniqueConstraint(
                    connection,
                    "evaluation_jobs",
                    "uk_evaluation_jobs_user_idempotency"
            );
            assertUniqueConstraint(
                    connection,
                    "evaluation_jobs",
                    "uk_evaluation_jobs_audio_object"
            );
            assertIndex(connection, "evaluation_jobs", "idx_evaluation_jobs_claim");
            assertIndex(connection, "evaluation_jobs", "idx_evaluation_jobs_cleanup");
            assertIndexMissing(connection, "evaluation_jobs", "idx_evaluation_jobs_dispatch");
        }
    }

    private void runMigration(Connection connection, String filename) throws Exception {
        RunScript.execute(
                connection,
                new FileReader(
                        "src/main/resources/db/migration/" + filename,
                        StandardCharsets.UTF_8
                )
        );
    }

    private void assertColumn(
            Connection connection,
            String tableName,
            String columnName
    ) throws Exception {
        try (ResultSet columns = connection.getMetaData()
                .getColumns(null, null, tableName, columnName)) {
            assertThat(columns.next())
                    .as("column %s.%s exists", tableName, columnName)
                    .isTrue();
        }
    }

    private void assertIndex(
            Connection connection,
            String tableName,
            String indexName
    ) throws Exception {
        try (ResultSet indexes = connection.getMetaData()
                .getIndexInfo(null, null, tableName, false, false)) {
            boolean found = false;
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    found = true;
                }
            }
            assertThat(found)
                    .as("index %s.%s exists", tableName, indexName)
                    .isTrue();
        }
    }

    private void assertColumnMissing(
            Connection connection,
            String tableName,
            String columnName
    ) throws Exception {
        try (ResultSet columns = connection.getMetaData()
                .getColumns(null, null, tableName, columnName)) {
            assertThat(columns.next())
                    .as("column %s.%s does not exist", tableName, columnName)
                    .isFalse();
        }
    }

    private void assertIndexMissing(
            Connection connection,
            String tableName,
            String indexName
    ) throws Exception {
        try (ResultSet indexes = connection.getMetaData()
                .getIndexInfo(null, null, tableName, false, false)) {
            while (indexes.next()) {
                assertThat(indexes.getString("INDEX_NAME"))
                        .isNotEqualToIgnoringCase(indexName);
            }
        }
    }

    private void assertUniqueConstraint(
            Connection connection,
            String tableName,
            String constraintName
    ) throws Exception {
        try (var statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                WHERE LOWER(TABLE_NAME) = LOWER(?)
                  AND LOWER(CONSTRAINT_NAME) = LOWER(?)
                  AND CONSTRAINT_TYPE = 'UNIQUE'
                """)) {
            statement.setString(1, tableName);
            statement.setString(2, constraintName);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1))
                        .as("constraint %s.%s exists", tableName, constraintName)
                        .isEqualTo(1);
            }
        }
    }
}
