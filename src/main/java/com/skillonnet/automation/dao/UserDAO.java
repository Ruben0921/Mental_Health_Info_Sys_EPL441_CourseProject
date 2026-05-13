package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.db.DatabaseException;
import com.skillonnet.automation.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Loads users for authentication; passwords compared to {@code users.password_hash} as stored (demo plain-text).
 */
public class UserDAO {

    private static final Set<String> ROLES = Set.of("Clinical", "Receptionist", "Medical_Records");

    private final DBConnection db;

    /** Uses the shared {@link DBConnection}. */
    public UserDAO() {
        this(DBConnection.getInstance());
    }

    /** @param db JDBC configuration source */
    public UserDAO(DBConnection db) {
        this.db = db;
    }

    /**
     * @param username login name
     * @param password password to verify
     * @return user when credentials match and role is allowed
     */
    public Optional<User> authenticate(String username, String password) {
        String sql = "SELECT user_id, username, password_hash, role FROM users WHERE username = ?";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                if (!password.equals(rs.getString("password_hash"))) {
                    return Optional.empty();
                }
                String role = rs.getString("role");
                if (!ROLES.contains(role)) {
                    return Optional.empty();
                }
                User u = new User();
                u.setUserId(rs.getInt("user_id"));
                u.setUsername(rs.getString("username"));
                u.setPasswordHash(rs.getString("password_hash"));
                u.setRole(role);
                return Optional.of(u);
            }
        } catch (SQLException e) {
            throw new DatabaseException("authenticate failed", e);
        }
    }

    /** Returns all system users ordered by role then username.
     * Used by Medical Records staff for access-control review.
     * Callers must strip {@code passwordHash} before serialising to the client.
     */
    public List<User> findAll() {
        String sql = "SELECT user_id, username, password_hash, role FROM users ORDER BY role, username";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            List<User> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapRow(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new DatabaseException("UserDAO.findAll failed", e);
        }
    }
 
    private static User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        return u;
    }
}
