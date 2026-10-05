package com.aram.bot.repository;

import com.aram.bot.config.DatabaseConfig;
import com.aram.bot.domain.Member;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MemberRepository {

    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM settlement WHERE name = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void save(String name) throws SQLException {
        String sql = "INSERT INTO settlement (name, amount) VALUES (?, 0);";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        }
    }

    public void delete(String name) throws SQLException {
        String sql = "DELETE FROM settlement WHERE name = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        }
    }

    public void updateAmount(String name, int deltaAmount) throws SQLException {
        String sql = "UPDATE settlement SET amount = amount + ? WHERE name = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, deltaAmount);
            pstmt.setString(2, name);
            pstmt.executeUpdate();
        }
    }

    public void setAmountZero(String name) throws SQLException {
        String sql = "UPDATE settlement SET amount = 0 WHERE name = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        }
    }

    public int getAmountByName(String name) {
        String sql = "SELECT amount FROM settlement WHERE name = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("amount");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<Member> findAll() {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT name, amount FROM settlement ORDER BY amount DESC, name ASC;";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                members.add(new Member(rs.getString("name"), rs.getInt("amount")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    public void resetAllAmounts() throws SQLException {
        String sql = "UPDATE settlement SET amount = 0;";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
}