package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.db.DatabaseException;
import com.skillonnet.automation.model.ClinicPatientCount;
import com.skillonnet.automation.model.ConditionPatientStat;
import com.skillonnet.automation.model.MedicationPrescriptionStat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.sql.Date;

/**
 * Aggregate reporting queries for medical records dashboards.
 */
public class ReportingDAO {

    private final DBConnection db;

    public ReportingDAO() {
        this(DBConnection.getInstance());
    }

    public ReportingDAO(DBConnection db) {
        this.db = db;
    }

    /** Distinct patients seen per clinic (via appointments). */
    public List<ClinicPatientCount> countDistinctPatientsPerClinic() {
        String sql = """
                SELECT c.clinic_id, c.name, COUNT(DISTINCT a.patient_id) AS cnt
                FROM clinics c
                LEFT JOIN appointment a ON c.clinic_id = a.clinic_id
                GROUP BY c.clinic_id, c.name
                ORDER BY c.clinic_id""";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            List<ClinicPatientCount> out = new ArrayList<>();
            while (rs.next()) {
                ClinicPatientCount row = new ClinicPatientCount();
                row.setClinicId(rs.getInt("clinic_id"));
                row.setClinicName(rs.getString("name"));
                row.setPatientCount(rs.getLong("cnt"));
                out.add(row);
            }
            return out;
        } catch (SQLException e) {
            throw new DatabaseException("report patients per clinic failed", e);
        }
    }

    /** Counts prescription rows grouped by medication. */
    public List<MedicationPrescriptionStat> prescriptionTotalsByMedication() {
        String sql = """
                SELECT m.medication_id, m.name, COUNT(*) AS cnt
                FROM prescription pr
                JOIN medication m ON pr.medication_id = m.medication_id
                GROUP BY m.medication_id, m.name
                ORDER BY m.medication_id""";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            List<MedicationPrescriptionStat> out = new ArrayList<>();
            while (rs.next()) {
                MedicationPrescriptionStat row = new MedicationPrescriptionStat();
                row.setMedicationId(rs.getInt("medication_id"));
                row.setMedicationName(rs.getString("name"));
                row.setPrescriptionCount(rs.getLong("cnt"));
                out.add(row);
            }
            return out;
        } catch (SQLException e) {
            throw new DatabaseException("report prescription stats failed", e);
        }
    }

    /**
     * Distinct patients per condition — all-time.
     * Used for the main Reports page condition breakdown.
     */
    public List<ConditionPatientStat> conditionTotals() {
        String sql = """
                SELECT c.condition_id, c.name, COUNT(DISTINCT pc.patient_id) AS cnt
                FROM `condition` c
                LEFT JOIN patient_condition pc ON pc.condition_id = c.condition_id
                GROUP BY c.condition_id, c.name
                ORDER BY cnt DESC""";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            List<ConditionPatientStat> out = new ArrayList<>();
            while (rs.next()) {
                ConditionPatientStat row = new ConditionPatientStat();
                row.setConditionId(rs.getInt("condition_id"));
                row.setConditionName(rs.getString("name"));
                row.setPatientCount(rs.getLong("cnt"));
                out.add(row);
            }
            return out;
        } catch (SQLException e) {
            throw new DatabaseException("conditionTotals failed", e);
        }
    }

    /**
     * Prescription totals grouped by medication for a date range and optional clinic.
     * Joins prescription → appointment (for date and clinic filtering) → medication.
     * Used for weekly report drug totals.
     *
     * @param from      start date (inclusive)
     * @param to        end date (inclusive)
     * @param clinicId  0 = all clinics
     */
    public List<MedicationPrescriptionStat> prescriptionStatsByDateRange(
            LocalDate from, LocalDate to, int clinicId) {
        String sql = """
                SELECT m.medication_id, m.name, COUNT(*) AS cnt
                FROM prescription pr
                JOIN appointment a  ON pr.appointment_id = a.appointment_id
                JOIN medication  m  ON pr.medication_id  = m.medication_id
                WHERE a.appointment_date >= ? AND a.appointment_date <= ?
                """ + (clinicId > 0 ? "AND a.clinic_id = ? " : "") + """
                GROUP BY m.medication_id, m.name
                ORDER BY cnt DESC""";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            if (clinicId > 0) ps.setInt(3, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MedicationPrescriptionStat> out = new ArrayList<>();
                while (rs.next()) {
                    MedicationPrescriptionStat row = new MedicationPrescriptionStat();
                    row.setMedicationId(rs.getInt("medication_id"));
                    row.setMedicationName(rs.getString("name"));
                    row.setPrescriptionCount(rs.getLong("cnt"));
                    out.add(row);
                }
                return out;
            }
        } catch (SQLException e) {
            throw new DatabaseException("prescriptionStatsByDateRange failed", e);
        }
    }
 
    /**
     * Patient counts grouped by condition for a date range and optional clinic.
     * Joins patient_condition → condition → appointment (for date/clinic filtering).
     * Used for weekly report conditions summary.
     *
     * @param from      start date (inclusive)
     * @param to        end date (inclusive)
     * @param clinicId  0 = all clinics
     */
    public List<ConditionPatientStat> conditionStatsByDateRange(
            LocalDate from, LocalDate to, int clinicId) {
        String sql = """
                SELECT c.condition_id, c.name, COUNT(DISTINCT pc.patient_id) AS cnt
                FROM patient_condition pc
                JOIN `condition` c ON pc.condition_id = c.condition_id
                JOIN appointment  a ON pc.appointment_id = a.appointment_id
                WHERE a.appointment_date >= ? AND a.appointment_date <= ?
                """ + (clinicId > 0 ? "AND a.clinic_id = ? " : "") + """
                GROUP BY c.condition_id, c.name
                ORDER BY cnt DESC""";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            if (clinicId > 0) ps.setInt(3, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ConditionPatientStat> out = new ArrayList<>();
                while (rs.next()) {
                    ConditionPatientStat row = new ConditionPatientStat();
                    row.setConditionId(rs.getInt("condition_id"));
                    row.setConditionName(rs.getString("name"));
                    row.setPatientCount(rs.getLong("cnt"));
                    out.add(row);
                }
                return out;
            }
        } catch (SQLException e) {
            throw new DatabaseException("conditionStatsByDateRange failed", e);
        }
    }
}
