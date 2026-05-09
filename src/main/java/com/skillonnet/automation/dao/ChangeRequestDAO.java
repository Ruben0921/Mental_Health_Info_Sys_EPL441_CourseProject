package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.db.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Persists patient change requests (status defaults to Pending).
 */
public class ChangeRequestDAO {

    private final DBConnection db;

    public ChangeRequestDAO() {
        this(DBConnection.getInstance());
    }

    public ChangeRequestDAO(DBConnection db) {
        this.db = db;
    }

    /** @return generated {@code request_id} */
    public int insert(String rawPatientData, String requestedChanges) {
        String sql = "INSERT INTO change_requests (raw_patient_data, requested_changes, status) VALUES (?, ?, 'Pending')";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, rawPatientData);
            ps.setString(2, requestedChanges);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("No generated key for change_requests insert");
                }
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("insert change_requests failed", e);
        }
    }
}
