package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.db.DatabaseException;
import com.skillonnet.automation.model.Prescription;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Prescription rows linked to appointments.
 */
public class PrescriptionDAO {

    private final DBConnection db;

    public PrescriptionDAO() {
        this(DBConnection.getInstance());
    }

    public PrescriptionDAO(DBConnection db) {
        this.db = db;
    }

    /**
     * @return generated prescription_id
     */
    public int insert(Prescription p) {
        String sql = """
                INSERT INTO prescription
                (appointment_id, medication_id, prescriber_id, issue_date, repeat_presc)
                VALUES (?, ?, ?, ?, ?)""";

        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, p.getAppointmentId());
            ps.setInt(2, p.getMedicationId());
            ps.setInt(3, p.getPrescriberId());

            if (p.getIssueDate() == null) {
                ps.setDate(4, null);
            } else {
                ps.setDate(4, Date.valueOf(p.getIssueDate()));
            }

            ps.setBoolean(5, p.isRepeat());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("No generated key for prescription insert");
                }

                int id = keys.getInt(1);
                p.setPrescriptionId(id);

                return id;
            }

        } catch (SQLException e) {
            throw new DatabaseException("insert prescription failed", e);
        }
    }

    /**
     * Updates an existing prescription by appointment_id.
     */
    public void update(Prescription p) {
        String sql = """
                UPDATE prescription
                SET medication_id = ?,
                    prescriber_id = ?,
                    issue_date = ?,
                    repeat_presc = ?
                WHERE appointment_id = ?""";

        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, p.getMedicationId());
            ps.setInt(2, p.getPrescriberId());

            if (p.getIssueDate() == null) {
                ps.setDate(3, null);
            } else {
                ps.setDate(3, Date.valueOf(p.getIssueDate()));
            }

            ps.setBoolean(4, p.isRepeat());
            ps.setInt(5, p.getAppointmentId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("update prescription failed", e);
        }
    }

    /**
     * Insert or update prescription for an appointment.
     */
    public void upsert(Prescription p) {
        Prescription existing = findByAppointmentId(p.getAppointmentId());

        if (existing != null) {
            update(p);
        } else {
            insert(p);
        }
    }

    /**
     * Finds prescription by appointment_id.
     */
    public Prescription findByAppointmentId(int appointmentId) {
        String sql = """
                SELECT p.prescription_id,
                       p.appointment_id,
                       p.medication_id,
                       m.name AS medication_name,
                       p.prescriber_id,
                       p.issue_date,
                       p.repeat_presc
                FROM prescription p
                JOIN medication m
                    ON m.medication_id = p.medication_id
                WHERE p.appointment_id = ?""";

        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, appointmentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException("find prescription failed", e);
        }
    }

    /**
     * Counts matching prescriptions joined through the appointment's patient.
     */
    public long countByPatientMedicationPrescriber(int patientId, int medicationId, int prescriberId) {
        String sql = """
                SELECT COUNT(*)
                FROM prescription pr
                JOIN appointment a
                    ON pr.appointment_id = a.appointment_id
                WHERE a.patient_id = ?
                  AND pr.medication_id = ?
                  AND pr.prescriber_id = ?""";

        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, patientId);
            ps.setInt(2, medicationId);
            ps.setInt(3, prescriberId);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }

        } catch (SQLException e) {
            throw new DatabaseException("count prescription failed", e);
        }
    }

    private Prescription map(ResultSet rs) throws SQLException {
        Prescription p = new Prescription();

        p.setPrescriptionId(rs.getInt("prescription_id"));
        p.setAppointmentId(rs.getInt("appointment_id"));
        p.setMedicationId(rs.getInt("medication_id"));
        p.setPrescriberId(rs.getInt("prescriber_id"));

        Date issueDate = rs.getDate("issue_date");
        if (issueDate != null) {
            p.setIssueDate(issueDate.toLocalDate());
        }

        p.setRepeat(rs.getBoolean("repeat_presc"));

        return p;
    }
}