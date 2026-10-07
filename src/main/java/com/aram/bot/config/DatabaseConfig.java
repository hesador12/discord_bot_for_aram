package com.aram.bot.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {

    public static Connection getConnection() throws SQLException {
        // Render의 DATABASE_URL 환경 변수 읽기
        String dbUrl = System.getenv("DATABASE_URL");

        if (dbUrl != null) {
            // postgresql:// 로 시작하는 주소를 JDBC 표준인 jdbc:postgresql:// 로 변환
            if (dbUrl.startsWith("postgresql://")) {
                dbUrl = dbUrl.replace("postgresql://", "jdbc:postgresql://");
            }
        } else {
            // 로컬 테스트용 (Supabase Connection String의 [YOUR-PASSWORD]를 실제 비밀번호로 교체하여 넣으세요)
            dbUrl = "jdbc:postgresql://postgres.khkvirrpbvyybvgwjvuc:실제비밀번호@aws-0-ap-northeast-1.pooler.supabase.com:6543/postgres?pgbouncer=true";
        }

        return DriverManager.getConnection(dbUrl);
    }

    public static void initDatabase() {
        // PostgreSQL 테이블 생성 SQL (TEXT -> VARCHAR(100))
        String createTableSql = "CREATE TABLE IF NOT EXISTS settlement (" +
                "name VARCHAR(100) PRIMARY KEY, " +
                "amount INTEGER NOT NULL DEFAULT 0" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
            System.out.println("✅ Supabase PostgreSQL 데이터베이스 테이블이 성공적으로 연동되었습니다.");
        } catch (SQLException e) {
            System.err.println("❌ 데이터베이스 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}