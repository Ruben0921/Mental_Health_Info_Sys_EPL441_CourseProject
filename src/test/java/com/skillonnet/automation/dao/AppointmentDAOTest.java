package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.db.DatabaseException;
import com.skillonnet.automation.model.Appointment;
import com.skillonnet.automation.model.MissedPatientRow;
import com.skillonnet.automation.support.TestDbSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentDAOTest {

    private AppointmentDAO appointmentDAO;

    @BeforeEach
    void setUp() {
        TestDbSupport.resetSchema();
        appointmentDAO = new AppointmentDAO();
    }

    @AfterEach
    void tearDown() {
        DBConnection.getInstance().closeConnection();
    }

    // Helper to build a minimal valid appointment
    private Appointment buildAppointment(int patientId) {
        Appointment a = new Appointment();
        a.setPatientId(patientId);
        a.setClinicId(1);
        a.setStaffId(1);
        a.setAppointmentDate(LocalDate.of(2024, 6, 1));
        a.setType("General");
        a.setStatus("Scheduled");
        a.setRecordsUpdated(false);
        return a;
    }

    @Test
    void insertAndFindById() {
        Appointment a = buildAppointment(1);
        int id = appointmentDAO.insert(a);

        assertEquals(id, a.getAppointmentId());

        Optional<Appointment> loaded = appointmentDAO.findById(id);
        assertTrue(loaded.isPresent());
        assertEquals("General", loaded.get().getType());
        assertEquals("Scheduled", loaded.get().getStatus());
        assertFalse(loaded.get().isRecordsUpdated());
    }

    @Test
    void findById_emptyWhenNotFound() {
        Optional<Appointment> result = appointmentDAO.findById(99999);
        assertTrue(result.isEmpty());
    }

    @Test
    void findByPatientId_returnsAllForPatient() {
        int patientId = 1;
        appointmentDAO.insert(buildAppointment(patientId));
        appointmentDAO.insert(buildAppointment(patientId));
        appointmentDAO.insert(buildAppointment(2)); // different patient, should not appear

        List<Appointment> results = appointmentDAO.findByPatientId(patientId);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(a -> a.getPatientId() == patientId));
    }

    @Test
    void findByPatientId_emptyWhenNone() {
        List<Appointment> results = appointmentDAO.findByPatientId(99999);
        assertTrue(results.isEmpty());
    }

    @Test
    void updateAttendance_changesStatus() {
        Appointment a = buildAppointment(1);
        int id = appointmentDAO.insert(a);

        appointmentDAO.updateAttendance(id, "Missed");

        assertEquals("Missed", appointmentDAO.findById(id).orElseThrow().getStatus());
    }

    @Test
    void updateAttendance_throwsWhenNotFound() {
        assertThrows(DatabaseException.class,
                () -> appointmentDAO.updateAttendance(99999, "Missed"));
    }

    @Test
    void markUpdated_setsFlag() {
        int id = appointmentDAO.insert(buildAppointment(1));

        appointmentDAO.markUpdated(id, true);

        assertTrue(appointmentDAO.findById(id).orElseThrow().isRecordsUpdated());
    }

    @Test
    void markUpdated_throwsWhenNotFound() {
        assertThrows(DatabaseException.class,
                () -> appointmentDAO.markUpdated(99999, true));
    }

    @Test
    void findMissedPatientsByDate_returnsOnlyMissedOnDate() {
        LocalDate date = LocalDate.of(2024, 6, 1);

        Appointment missed = buildAppointment(1);
        missed.setStatus("Missed");
        missed.setAppointmentDate(date);
        appointmentDAO.insert(missed);

        Appointment attended = buildAppointment(2);
        attended.setStatus("Attended");
        attended.setAppointmentDate(date);
        appointmentDAO.insert(attended);

        List<MissedPatientRow> results = appointmentDAO.findMissedPatientsByDate(date);

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getPatientId());
    }

    @Test
    void findAppointmentsWithRecordsNotUpdated_returnsOnlyPending() {
        Appointment pending = buildAppointment(1);
        pending.setRecordsUpdated(false);
        appointmentDAO.insert(pending);

        Appointment done = buildAppointment(2);
        done.setRecordsUpdated(true);
        appointmentDAO.insert(done);

        List<Appointment> results = appointmentDAO.findAppointmentsWithRecordsNotUpdated();

        assertTrue(results.stream().noneMatch(Appointment::isRecordsUpdated));
        assertTrue(results.stream().anyMatch(a -> a.getPatientId() == 1));
        assertTrue(results.stream().noneMatch(a -> a.getPatientId() == 2));
    }

    @Test
    void insertHandlesNullDate() {
        Appointment a = buildAppointment(1);
        a.setAppointmentDate(null);
        int id = appointmentDAO.insert(a);

        Appointment loaded = appointmentDAO.findById(id).orElseThrow();
        assertNull(loaded.getAppointmentDate());
    }
}