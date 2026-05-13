package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.db.DatabaseException;
import com.skillonnet.automation.model.Appointment;
import com.skillonnet.automation.model.MissedPatientRow;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access for appointments, attendance updates, and receptionist reporting queries.
 */
public class AppointmentDAO {

    private final DBConnection db;

    public AppointmentDAO() {
        this(DBConnection.getInstance());
    }

    public AppointmentDAO(DBConnection db) {
        this.db = db;
    }

    /** Inserts a row and sets {@code appointmentId} on the model. */
    public int insert(Appointment a) {
        String sql = """
                INSERT INTO appointment (patient_id, clinic_id, staff_id, appointment_date, type, status, records_updated)
                VALUES (?, ?, ?, ?, ?, ?, ?)""";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getPatientId());
            ps.setInt(2, a.getClinicId());
            ps.setInt(3, a.getStaffId());
            if (a.getAppointmentDate() == null) {
                ps.setDate(4, null);
            } else {
                ps.setDate(4, Date.valueOf(a.getAppointmentDate()));
            }
            ps.setString(5, a.getType());
            ps.setString(6, a.getStatus());
            ps.setBoolean(7, a.isRecordsUpdated());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("No generated key for appointment insert");
                }
                int id = keys.getInt(1);
                a.setAppointmentId(id);
                return id;
            }
        } catch (SQLException e) {
            throw new DatabaseException("insert appointment failed", e);
        }
    }

    /** Loads an appointment by id. */
    public Optional<Appointment> findById(int appointmentId) {
        String sql = """
                SELECT appointment_id, patient_id, clinic_id, staff_id, appointment_date, type, status, records_updated
                FROM appointment WHERE appointment_id = ?""";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("find appointment failed", e);
        }
    }

	public List<Appointment> findByPatientId(int patientId) {
		String sql = """
				SELECT appointment_id,
					patient_id,
					clinic_id,
					staff_id,
					appointment_date,
					type,
					status,
					records_updated
				FROM appointment
				WHERE patient_id = ?""";

		List<Appointment> appointments = new ArrayList<>();

		try (Connection conn = db.newConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, patientId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					appointments.add(mapRow(rs));
				}
			}

			return appointments;

		} catch (SQLException e) {
			throw new DatabaseException("find appointments failed", e);
		}
	}

    /** Returns all appointments whose {@code appointment_date} falls within
     * [{@code from}, {@code to}] inclusive, ordered by date ascending.
     * Used for weekly report generation (Management viewpoint req. 2).
     */
    public List<Appointment> findByDateRange(LocalDate from, LocalDate to) {
        String sql = """
                SELECT appointment_id, patient_id, clinic_id, staff_id, appointment_date, type, status, records_updated
                FROM appointment
                WHERE appointment_date >= ? AND appointment_date <= ?
                ORDER BY appointment_date ASC, appointment_id ASC""";
        List<Appointment> out = new ArrayList<>();
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
            }
            return out;
        } catch (SQLException e) {
            throw new DatabaseException("findByDateRange failed", e);
        }
    }

    /** Sets the appointment {@code status} (e.g. Attended, Missed). */
    public void updateAttendance(int appointmentId, String status) {
        String sql = "UPDATE appointment SET status = ? WHERE appointment_id = ?";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, appointmentId);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new DatabaseException("Appointment not found: " + appointmentId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("update appointment attendance failed", e);
        }
    }

	public void markUpdated(int appointmentId, boolean value) {
		String sql = "UPDATE appointment SET records_updated = ? WHERE appointment_id = ?";
		try (Connection conn = db.newConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setBoolean(1, value);
			ps.setInt(2, appointmentId);
			int n = ps.executeUpdate();
			if (n == 0) {
				throw new DatabaseException("Appointment not found: " + appointmentId);
			}
		} catch (SQLException e) {
			throw new DatabaseException("update appointment failed", e);
		}
    }

    /** Missed appointments on a given local date ({@code status = 'Missed'}). */
    public List<MissedPatientRow> findMissedPatientsByDate(LocalDate date) {
        String sql = """
                SELECT a.appointment_id, p.patient_id, p.first_name, p.last_name
                FROM appointment a
                JOIN patients p ON a.patient_id = p.patient_id
                WHERE a.appointment_date = ? AND a.status = 'Missed'""";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                List<MissedPatientRow> out = new ArrayList<>();
                while (rs.next()) {
                    MissedPatientRow row = new MissedPatientRow();
                    row.setAppointmentId(rs.getInt("appointment_id"));
                    row.setPatientId(rs.getInt("patient_id"));
                    row.setFirstName(rs.getString("first_name"));
                    row.setLastName(rs.getString("last_name"));
                    out.add(row);
                }
                return out;
            }
        } catch (SQLException e) {
            throw new DatabaseException("find missed appointments failed", e);
        }
    }

    /** Appointments whose clinical records flag is still false. */
    public List<Appointment> findAppointmentsWithRecordsNotUpdated() {
        String sql = """
                SELECT appointment_id, patient_id, clinic_id, staff_id, appointment_date, type, status, records_updated
                FROM appointment WHERE records_updated = FALSE""";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            List<Appointment> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapRow(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new DatabaseException("find pending records failed", e);
        }
    }

    private static Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setAppointmentId(rs.getInt("appointment_id"));
        a.setPatientId(rs.getInt("patient_id"));
        a.setClinicId(rs.getInt("clinic_id"));
        a.setStaffId(rs.getInt("staff_id"));
        Date d = rs.getDate("appointment_date");
        a.setAppointmentDate(d == null ? null : d.toLocalDate());
        a.setType(rs.getString("type"));
        a.setStatus(rs.getString("status"));
        a.setRecordsUpdated(rs.getBoolean("records_updated"));
        return a;
    }
}
