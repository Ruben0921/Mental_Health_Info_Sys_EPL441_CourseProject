package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.db.DatabaseException;
import com.skillonnet.automation.model.ClinicPatientCount;
import com.skillonnet.automation.model.MedicationPrescriptionStat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
}
