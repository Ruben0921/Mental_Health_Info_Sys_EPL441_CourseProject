package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.db.DatabaseException;
import com.skillonnet.automation.model.PatientCondition;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Patient condition rows linked to appointments.
 */
public class PatientConditionDAO {

    private final DBConnection db;

    public PatientConditionDAO() {
        this(DBConnection.getInstance());
    }

    public PatientConditionDAO(DBConnection db) {
        this.db = db;
    }

    public PatientCondition findByAppointmentId(int appointmentId) {
        String sql = """
                SELECT pc.patient_condition_id,
                       pc.patient_id,
                       pc.condition_id,
                       c.name AS condition_name,
                       pc.appointment_id,
                       pc.diagnosis_date,
                       pc.notes
                FROM patient_condition pc
                JOIN `condition` c ON c.condition_id = pc.condition_id
                WHERE pc.appointment_id = ?""";

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
            throw new DatabaseException("find patient condition failed", e);
        }
    }

    public void upsert(PatientCondition pc) {
        PatientCondition existing = findByAppointmentId(pc.getAppointmentId());

        if (existing != null) {
            update(pc);
        } else {
            insert(pc);
        }
    }

    public void insert(PatientCondition pc) {
        String sql = """
                INSERT INTO patient_condition
                (patient_id, condition_id, appointment_id, diagnosis_date, notes)
                VALUES (?, ?, ?, ?, ?)""";

        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, pc.getPatientId());
            ps.setInt(2, pc.getConditionId());
            ps.setInt(3, pc.getAppointmentId());

            if (pc.getDiagnosisDate() == null) {
                ps.setDate(4, null);
            } else {
                ps.setDate(4, Date.valueOf(pc.getDiagnosisDate()));
            }

            ps.setString(5, pc.getNotes());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("insert patient condition failed", e);
        }
    }

    public void update(PatientCondition pc) {
        String sql = """
                UPDATE patient_condition
                SET condition_id = ?,
                    diagnosis_date = ?,
                    notes = ?
                WHERE appointment_id = ?""";

        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, pc.getConditionId());

            if (pc.getDiagnosisDate() == null) {
                ps.setDate(2, null);
            } else {
                ps.setDate(2, Date.valueOf(pc.getDiagnosisDate()));
            }

            ps.setString(3, pc.getNotes());
            ps.setInt(4, pc.getAppointmentId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("update patient condition failed", e);
        }
    }

    private PatientCondition map(ResultSet rs) throws SQLException {
        PatientCondition pc = new PatientCondition();

        pc.setPatientConditionId(rs.getInt("patient_condition_id"));
        pc.setPatientId(rs.getInt("patient_id"));
        pc.setConditionId(rs.getInt("condition_id"));
        pc.setConditionName(rs.getString("condition_name"));
        pc.setAppointmentId(rs.getInt("appointment_id"));

        Date diagnosisDate = rs.getDate("diagnosis_date");
        if (diagnosisDate != null) {
            pc.setDiagnosisDate(diagnosisDate.toLocalDate().toString());
        }

        pc.setNotes(rs.getString("notes"));

        return pc;
    }
}