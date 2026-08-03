package com.lingko.lingko.core.domain.user;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 사용자 설정 migration이 사용되지 않는 목표 레벨을 제거하고 언어 설정은 보존하는지 검증한다.
 */
class UserPreferencesMigrationTest {

    @Test
    @DisplayName("사용자 설정 migration은 언어 컬럼을 보존하고 target_level을 제거한다")
    void userPreferencesMigrationRemovesTargetLevel() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:user_preferences_migration;MODE=MySQL;DATABASE_TO_UPPER=false"
        )) {
            runMigration(connection, "V1__baseline_schema.sql");
            runMigration(connection, "V4__add_user_learning_preferences.sql");
            runMigration(connection, "V13__remove_user_target_level.sql");

            assertThat(hasColumn(connection, "display_language")).isTrue();
            assertThat(hasColumn(connection, "native_language")).isTrue();
            assertThat(hasColumn(connection, "target_level")).isFalse();
        }
    }

    private void runMigration(Connection connection, String fileName) throws Exception {
        RunScript.execute(
                connection,
                new FileReader(
                        "src/main/resources/db/migration/" + fileName,
                        StandardCharsets.UTF_8
                )
        );
    }

    private boolean hasColumn(Connection connection, String columnName) throws Exception {
        try (var columns = connection.getMetaData().getColumns(
                null,
                null,
                "users",
                columnName
        )) {
            return columns.next();
        }
    }
}
