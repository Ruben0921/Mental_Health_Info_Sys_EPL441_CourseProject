package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.db.DatabaseException;
import com.skillonnet.automation.model.AdverseReaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Stores and queries patient adverse reactions to medications.
 */
public class AdverseReactionDAO {

    private final DBConnection db;

    public AdverseReactionDAO() {
        this(DBConnection.getInstance());
    }

    public AdverseReactionDAO(DBConnection db) {
        this.db = db;
    }

    /** @return true if a reaction row exists for the patient and medication */
    public boolean existsByPatientAndMedication(int patientId, int medicationId) {
        String sql = """
                SELECT 1 FROM adverse_reaction WHERE patient_id = ? AND medication_id = ?""";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setInt(2, medicationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("exists adverse reaction failed", e);
        }
    }
	

    /** @return generated {@code reaction_id} */
    public int insert(AdverseReaction r) {
        String sql = "INSERT INTO adverse_reaction (patient_id, medication_id, description) VALUES (?, ?, ?)";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getPatientId());
            ps.setInt(2, r.getMedicationId());
            ps.setString(3, r.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("No generated key for adverse_reaction insert");
                }
                int id = keys.getInt(1);
                r.setReactionId(id);
                return id;
            }
        } catch (SQLException e) {
            throw new DatabaseException("insert adverse_reaction failed", e);
        }
    }

	/** @return all adverse reactions for a patient */
	public List<AdverseReaction> findByPatientId(int patientId) {
    String sql = """
            SELECT ar.reaction_id, ar.patient_id, ar.medication_id,
                   m.name AS medication_name, ar.description
            FROM adverse_reaction ar
            JOIN medication m ON m.medication_id = ar.medication_id
            WHERE ar.patient_id = ?""";
    try (Connection conn = db.newConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, patientId);
        try (ResultSet rs = ps.executeQuery()) {
            List<AdverseReaction> list = new java.util.ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        }
    } catch (SQLException e) {
        throw new DatabaseException("find adverse reactions failed", e);
    }
}

	public void delete(int reactionId) {
		String sql = "DELETE FROM adverse_reaction WHERE reaction_id = ?";
		try (Connection conn = db.newConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, reactionId);
			int n = ps.executeUpdate();
			if (n == 0) throw new DatabaseException("Adverse reaction not found: " + reactionId);
		} catch (SQLException e) {
			throw new DatabaseException("delete adverse reaction failed", e);
		}
	}

	private AdverseReaction map(ResultSet rs) throws SQLException {
		AdverseReaction r = new AdverseReaction();
		r.setReactionId(rs.getInt("reaction_id"));
		r.setPatientId(rs.getInt("patient_id"));
		r.setMedicationId(rs.getInt("medication_id"));
		r.setMedicationName(rs.getString("medication_name"));
		r.setDescription(rs.getString("description"));
		return r;
	}
	
}
