package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.db.DatabaseException;
import com.skillonnet.automation.model.WarningOverride;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Persists prescriber overrides when warnings are acknowledged.
 */
public class WarningOverrideDAO {

    private final DBConnection db;

    public WarningOverrideDAO() {
        this(DBConnection.getInstance());
    }

    public WarningOverrideDAO(DBConnection db) {
        this.db = db;
    }

    /** @return generated {@code override_id} */
    public int insert(WarningOverride o) {
        String sql = """
                INSERT INTO warning_override (prescriber_id, medication_id, warning_details, override_date)
                VALUES (?, ?, ?, ?)""";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, o.getPrescriberId());
            ps.setInt(2, o.getMedicationId());
            ps.setString(3, o.getWarningDetails());
            if (o.getOverrideDate() == null) {
                ps.setDate(4, null);
            } else {
                ps.setDate(4, Date.valueOf(o.getOverrideDate()));
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("No generated key for warning_override insert");
                }
                int id = keys.getInt(1);
                o.setOverrideId(id);
                return id;
            }
        } catch (SQLException e) {
            throw new DatabaseException("insert warning_override failed", e);
        }
    }

    /** Counts override rows for a prescriber/medication pair. */
    public long countByPrescriberAndMedication(int prescriberId, int medicationId) {
        String sql = "SELECT COUNT(*) FROM warning_override WHERE prescriber_id = ? AND medication_id = ?";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, prescriberId);
            ps.setInt(2, medicationId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("count warning_override failed", e);
        }
    }
}
