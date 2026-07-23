package com.lingko.lingko.core.domain.auth;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRefreshSessionMigrationTest {

    @Test
    @DisplayName("Refresh Token migration은 해시 기반 세션과 사용자·만료 인덱스를 생성한다")
    void refreshSessionMigrationCreatesSecureSessionTable() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:auth_refresh_session;MODE=MySQL;DATABASE_TO_UPPER=false"
        )) {
            runMigration(connection, "V1__baseline_schema.sql");
            runMigration(connection, "V6__add_auth_refresh_sessions.sql");

            assertColumn(connection, "auth_refresh_sessions", "session_id");
            assertColumn(connection, "auth_refresh_sessions", "current_token_hash");
            assertColumn(connection, "auth_refresh_sessions", "expires_at");
            assertColumn(connection, "auth_refresh_sessions", "revoked_at");
            assertUniqueIndexOnColumn(connection, "auth_refresh_sessions", "current_token_hash");
            assertIndex(connection, "auth_refresh_sessions", "idx_auth_refresh_sessions_user");
            assertIndex(connection, "auth_refresh_sessions", "idx_auth_refresh_sessions_expiry");
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

    private void assertColumn(Connection connection, String tableName, String columnName) throws Exception {
        try (ResultSet resultSet = connection.getMetaData().getColumns(null, null, tableName, columnName)) {
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

    private void assertUniqueIndexOnColumn(
            Connection connection,
            String tableName,
            String columnName
    ) throws Exception {
        try (ResultSet resultSet = connection.getMetaData()
                .getIndexInfo(null, null, tableName, true, false)) {
            boolean found = false;
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("COLUMN_NAME"))) {
                    found = true;
                    break;
                }
            }
            assertThat(found)
                    .as("unique index on %s.%s exists", tableName, columnName)
                    .isTrue();
        }
    }
}
