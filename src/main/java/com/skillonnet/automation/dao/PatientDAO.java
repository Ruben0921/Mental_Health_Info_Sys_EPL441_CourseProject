package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.db.DatabaseException;
import com.skillonnet.automation.exception.DeceasedPatientException;
import com.skillonnet.automation.model.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD access to the {@code patients} table; enforces read-only semantics for deceased patients.
 */
public class PatientDAO {

    private final DBConnection db;

    public PatientDAO() {
        this(DBConnection.getInstance());
    }

    public PatientDAO(DBConnection db) {
        this.db = db;
    }

    /** @return generated {@code patient_id} */
    public int insert(Patient p) {
        String sql = """
                INSERT INTO patients (first_name, last_name, address, homeless, risk_status, deceased, self_harm_history)
                VALUES (?, ?, ?, ?, ?, ?, ?)""";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getFirstName());
            ps.setString(2, p.getLastName());
            ps.setString(3, p.getAddress());
            ps.setBoolean(4, p.isHomeless());
            ps.setString(5, p.getRiskStatus());
            ps.setBoolean(6, p.isDeceased());
            ps.setBoolean(7, p.isSelfHarmHistory());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("No generated key for patient insert");
                }
                int id = keys.getInt(1);
                p.setPatientId(id);
                return id;
            }
        } catch (SQLException e) {
            throw new DatabaseException("insert patient failed", e);
        }
    }

    /** Loads a patient by primary key. */
    public Optional<Patient> findById(int patientId) {
        String sql = """
                SELECT patient_id, first_name, last_name, address, homeless, risk_status, deceased, self_harm_history
                FROM patients WHERE patient_id = ?""";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("find patient failed", e);
        }
    }

    /** Lists all patients ordered by id. */
    public List<Patient> findAll() {
        String sql = """
                SELECT patient_id, first_name, last_name, address, homeless, risk_status, deceased, self_harm_history
                FROM patients ORDER BY patient_id""";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            List<Patient> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapRow(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new DatabaseException("list patients failed", e);
        }
    }

    /**
     * Updates mutable patient fields.
     *
     * @throws DeceasedPatientException when the patient is deceased
     */
    public void update(Patient p) {
        Optional<Patient> existing = findById(p.getPatientId());
        if (existing.isEmpty()) {
            throw new DatabaseException("Patient not found: " + p.getPatientId());
        }
        if (existing.get().isDeceased()) {
            throw new DeceasedPatientException();
        }
        String sql = """
                UPDATE patients SET first_name = ?, last_name = ?, address = ?, homeless = ?, risk_status = ?, deceased = ?, self_harm_history = ?
                WHERE patient_id = ? AND deceased = FALSE""";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getFirstName());
            ps.setString(2, p.getLastName());
            ps.setString(3, p.getAddress());
            ps.setBoolean(4, p.isHomeless());
            ps.setString(5, p.getRiskStatus());
            ps.setBoolean(6, p.isDeceased());
            ps.setBoolean(7, p.isSelfHarmHistory());
            ps.setInt(8, p.getPatientId());
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new DeceasedPatientException();
            }
        } catch (SQLException e) {
            throw new DatabaseException("update patient failed", e);
        }
    }

    /** Updates the self-harm history flag only. */
    public void updateSelfHarmHistory(int patientId, boolean selfHarmHistory) {
        String sql = "UPDATE patients SET self_harm_history = ? WHERE patient_id = ?";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, selfHarmHistory);
            ps.setInt(2, patientId);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new DatabaseException("Patient not found: " + patientId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("update self_harm_history failed", e);
        }
    }

    /**
     * Deletes a living patient row.
     *
     * @throws DeceasedPatientException when the patient is deceased (delete not allowed)
     */
    public void deleteById(int patientId) {
        String sql = "DELETE FROM patients WHERE patient_id = ? AND deceased = FALSE";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            int n = ps.executeUpdate();
            if (n > 0) {
                return;
            }
        } catch (SQLException e) {
            throw new DatabaseException("delete patient failed", e);
        }
        Optional<Patient> p = findById(patientId);
        if (p.isEmpty()) {
            throw new DatabaseException("Patient not found: " + patientId);
        }
        throw new DeceasedPatientException();
    }

    private static Patient mapRow(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.setPatientId(rs.getInt("patient_id"));
        p.setFirstName(rs.getString("first_name"));
        p.setLastName(rs.getString("last_name"));
        p.setAddress(rs.getString("address"));
        p.setHomeless(rs.getBoolean("homeless"));
        p.setRiskStatus(rs.getString("risk_status"));
        p.setDeceased(rs.getBoolean("deceased"));
        p.setSelfHarmHistory(rs.getBoolean("self_harm_history"));
        return p;
    }
}
