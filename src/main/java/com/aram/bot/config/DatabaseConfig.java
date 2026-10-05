package com.aram.bot.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {
    public static final String DB_URL = "jdbc:sqlite:settlement.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initDatabase() {
        String createTableSql = "CREATE TABLE IF NOT EXISTS settlement (" +
                "name TEXT PRIMARY KEY, " +
                "amount INTEGER NOT NULL DEFAULT 0" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
            System.out.println("✅ SQLite 데이터베이스 테이블이 준비되었습니다.");
        } catch (SQLException e) {
            System.err.println("❌ 데이터베이스 초기화 실패: " + e.getMessage());
        }
    }
}