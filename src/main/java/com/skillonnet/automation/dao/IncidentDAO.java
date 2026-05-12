package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.db.DatabaseException;
import com.skillonnet.automation.model.Incident;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Incident records (e.g. deliberate self-harm) for clinical risk logic.
 */
public class IncidentDAO {

    private static final String DELIBERATE = "Deliberate";

    private final DBConnection db;

    public IncidentDAO() {
        this(DBConnection.getInstance());
    }

    public IncidentDAO(DBConnection db) {
        this.db = db;
    }

    /** @return generated {@code incident_id} */
    public int insert(Incident i) {
        String sql = "INSERT INTO incident (patient_id, type, description, incident_date) VALUES (?, ?, ?, ?)";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, i.getPatientId());
            ps.setString(2, i.getType());
            ps.setString(3, i.getDescription());
            if (i.getIncidentDate() == null) {
                ps.setDate(4, null);
            } else {
                ps.setDate(4, Date.valueOf(i.getIncidentDate()));
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("No generated key for incident insert");
                }
                int id = keys.getInt(1);
                i.setIncidentId(id);
                return id;
            }
        } catch (SQLException e) {
            throw new DatabaseException("insert incident failed", e);
        }
    }

    /** @return true if any incident of type "Deliberate" exists for the patient */
    public boolean hasDeliberateSelfHarm(int patientId) {
        String sql = "SELECT 1 FROM incident WHERE patient_id = ? AND type = ?";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setString(2, DELIBERATE);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("query incident failed", e);
        }
    }
}
