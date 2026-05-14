package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.model.Incident;
import com.skillonnet.automation.support.TestDbSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class IncidentDAOTest {

    private IncidentDAO incidentDAO;

    @BeforeEach
    void setUp() {
        TestDbSupport.resetSchema();
        incidentDAO = new IncidentDAO();
    }

    @AfterEach
    void tearDown() {
        DBConnection.getInstance().closeConnection();
    }

    private Incident buildIncident(int patientId, String type) {
        Incident i = new Incident();
        i.setPatientId(patientId);
        i.setType(type);
        i.setDescription("Test incident");
        i.setIncidentDate(LocalDate.of(2024, 6, 1));
        return i;
    }

    @Test
    void insert_returnsGeneratedId() {
        Incident i = buildIncident(1, "Deliberate");
        int id = incidentDAO.insert(i);

        assertTrue(id > 0);
        assertEquals(id, i.getIncidentId());
    }

    @Test
    void hasDeliberateSelfHarm_trueWhenDeliberateIncidentExists() {
        incidentDAO.insert(buildIncident(1, "Deliberate"));

        assertTrue(incidentDAO.hasDeliberateSelfHarm(1));
    }

    @Test
    void hasDeliberateSelfHarm_falseWhenNoIncidents() {
        assertFalse(incidentDAO.hasDeliberateSelfHarm(99999));
    }

    @Test
    void hasDeliberateSelfHarm_falseWhenOnlyAccidentalIncidents() {
        // A non-deliberate incident should NOT trigger the flag
        incidentDAO.insert(buildIncident(1, "Accidental"));

        assertFalse(incidentDAO.hasDeliberateSelfHarm(1));
    }

    @Test
    void hasDeliberateSelfHarm_onlyMatchesCorrectPatient() {
        incidentDAO.insert(buildIncident(1, "Deliberate"));

        // Patient 2 has no incidents — should not be affected by patient 1's record
        assertFalse(incidentDAO.hasDeliberateSelfHarm(2));
    }

    @Test
    void insert_handlesNullDate() {
        Incident i = buildIncident(1, "Deliberate");
        i.setIncidentDate(null);

        // Should not throw
        int id = incidentDAO.insert(i);
        assertTrue(id > 0);
    }
}