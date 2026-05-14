package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.exception.DeceasedPatientException;
import com.skillonnet.automation.model.Patient;
import com.skillonnet.automation.support.TestDbSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientDAOTest {

    private PatientDAO patientDAO;

    @BeforeEach
    void setUp() {
        TestDbSupport.resetSchema();
        patientDAO = new PatientDAO();
    }

    @AfterEach
    void tearDown() {
        DBConnection.getInstance().closeConnection();
    }

    @Test
    void crud() {
        Patient p = new Patient();
        p.setFirstName("A");
        p.setLastName("B");
        p.setAddress("1 St");
        p.setHomeless(false);
        p.setRiskStatus("Low");
        p.setDeceased(false);
        p.setSelfHarmHistory(false);
        int id = patientDAO.insert(p);
        assertEquals(id, p.getPatientId());

        Optional<Patient> loaded = patientDAO.findById(id);
        assertTrue(loaded.isPresent());
        assertEquals("A", loaded.get().getFirstName());

        p.setFirstName("Ann");
        patientDAO.update(p);
        assertEquals("Ann", patientDAO.findById(id).orElseThrow().getFirstName());

        patientDAO.deleteById(id);
        assertTrue(patientDAO.findById(id).isEmpty());
    }

    @Test
    void deceasedPatientCannotBeUpdated() {
        Patient p = new Patient();
        p.setFirstName("D");
        p.setLastName("E");
        p.setAddress(null);
        p.setHomeless(false);
        p.setRiskStatus(null);
        p.setDeceased(true);
        p.setSelfHarmHistory(false);
        int id = patientDAO.insert(p);

        p.setPatientId(id);
        p.setFirstName("X");
        assertThrows(DeceasedPatientException.class, () -> patientDAO.update(p));
    }

    @Test
    void selfHarmFlagCanBeSetWhenDeceased() {
        Patient p = new Patient();
        p.setFirstName("D");
        p.setLastName("E");
        p.setAddress(null);
        p.setHomeless(false);
        p.setRiskStatus(null);
        p.setDeceased(true);
        p.setSelfHarmHistory(false);
        int id = patientDAO.insert(p);

        patientDAO.updateSelfHarmHistory(id, true);
        assertTrue(patientDAO.findById(id).orElseThrow().isSelfHarmHistory());
    }
}
