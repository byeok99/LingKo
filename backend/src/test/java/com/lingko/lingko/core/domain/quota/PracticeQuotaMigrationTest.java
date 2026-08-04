package com.lingko.lingko.core.domain.quota;

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
 * 연습 에너지 migration이 예약 lifecycle과 시간 충전에 필요한 컬럼을 실제 스키마에 추가하는지 검증한다.
 */
class PracticeQuotaMigrationTest {

    @Test
    @DisplayName("쿼터 예약 migration은 무료·보상 예약 계수를 추가한다")
    void addsQuotaReservationColumns() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:practice_quota_migration;MODE=MySQL;DATABASE_TO_UPPER=false"
        )) {
            runMigration(connection, "V1__baseline_schema.sql");
            runMigration(connection, "V5__add_daily_practice_quota.sql");
            runMigration(connection, "V7__add_practice_quota_reservations.sql");

            assertColumnDefault(connection, "free_reserved", "0");
            assertColumnDefault(connection, "rewarded_reserved", "0");
        }
    }

    @Test
    @DisplayName("시간 충전 migration은 다음 자연 충전 시각을 추가한다")
    void addsNextRefillAtColumn() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:practice_energy_migration;MODE=MySQL;DATABASE_TO_UPPER=false"
        )) {
            runMigration(connection, "V1__baseline_schema.sql");
            runMigration(connection, "V5__add_daily_practice_quota.sql");
            runMigration(connection, "V7__add_practice_quota_reservations.sql");
            runMigration(connection, "V14__add_hourly_practice_refill.sql");

            try (ResultSet columns = connection.getMetaData()
                    .getColumns(null, null, "daily_practice_quota", "next_refill_at")) {
                assertThat(columns.next()).as("column next_refill_at exists").isTrue();
                assertThat(columns.getInt("NULLABLE")).isEqualTo(1);
            }
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

    private void assertColumnDefault(
            Connection connection,
            String columnName,
            String expectedDefault
    ) throws Exception {
        try (ResultSet columns = connection.getMetaData()
                .getColumns(null, null, "daily_practice_quota", columnName)) {
            assertThat(columns.next()).as("column %s exists", columnName).isTrue();
            assertThat(columns.getInt("NULLABLE")).isZero();
            assertThat(columns.getString("COLUMN_DEF")).isEqualTo(expectedDefault);
        }
    }
}
