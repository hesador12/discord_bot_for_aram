package com.aram.bot.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ PostgreSQL JDBC 드라이버 클래스를 찾을 수 없습니다.");
            e.printStackTrace();
        }

        String dbUrl = System.getenv("DATABASE_URL");

        if (dbUrl != null && !dbUrl.trim().isEmpty()) {
            dbUrl = dbUrl.trim();
            
            if (dbUrl.startsWith("postgresql://")) {
                dbUrl = dbUrl.replace("postgresql://", "jdbc:postgresql://");
            } 
            else if (!dbUrl.startsWith("jdbc:postgresql://")) {
                dbUrl = "jdbc:postgresql://" + dbUrl;
            }
        } else {
            dbUrl = "jdbc:postgresql://postgres.khkvirrpbvyybvgwjvuc:skekrhdudgml@aws-0-ap-northeast-1.pooler.supabase.com:6543/postgres?pgbouncer=true";
        }

        System.out.println("🔗 [DB 접속 시도 URL]: " + dbUrl);

        return DriverManager.getConnection(dbUrl);
    }

    public static void initDatabase() {
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