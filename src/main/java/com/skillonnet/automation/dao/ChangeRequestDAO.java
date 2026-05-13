package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.db.DatabaseException;
import com.skillonnet.automation.model.ChangeRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Returns all change requests ordered by request_id descending (newest first).
     */
    public List<ChangeRequest> findAll() {
        String sql = "SELECT request_id, raw_patient_data, requested_changes, status "
                   + "FROM change_requests ORDER BY request_id DESC";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            List<ChangeRequest> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapRow(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new DatabaseException("ChangeRequestDAO.findAll failed", e);
        }
    }
 
    /**
     * Updates the status of a change request (Accepted or Rejected).
     * (General req. 16 — accept/reject procedure)
     */
    public void updateStatus(int requestId, String status) {
        String sql = "UPDATE change_requests SET status = ? WHERE request_id = ?";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, requestId);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new DatabaseException("Change request not found: " + requestId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("ChangeRequestDAO.updateStatus failed", e);
        }
    }
 
    private static ChangeRequest mapRow(ResultSet rs) throws SQLException {
        ChangeRequest cr = new ChangeRequest();
        cr.setRequestId(rs.getInt("request_id"));
        cr.setRawPatientData(rs.getString("raw_patient_data"));
        cr.setRequestedChanges(rs.getString("requested_changes"));
        cr.setStatus(rs.getString("status"));
        return cr;
    }
}
