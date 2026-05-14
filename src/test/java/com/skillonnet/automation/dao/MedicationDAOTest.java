package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.model.Medication;
import com.skillonnet.automation.support.TestDbSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MedicationDAOTest {

    private MedicationDAO medicationDAO;

    @BeforeEach
    void setUp() {
        TestDbSupport.resetSchema();
        medicationDAO = new MedicationDAO();
    }

    @AfterEach
    void tearDown() {
        DBConnection.getInstance().closeConnection();
    }

    private Medication buildMedication(String name) {
        Medication m = new Medication();
        m.setName(name);
        return m;
    }

    @Test
    void insert_returnsGeneratedId() {
        Medication m = buildMedication("Paracetamol");
        int id = medicationDAO.insert(m);

        assertTrue(id > 0);
        assertEquals(id, m.getMedicationId());
    }

    @Test
    void findAll_returnsInsertedMedications() {
        medicationDAO.insert(buildMedication("Paracetamol"));
        medicationDAO.insert(buildMedication("Ibuprofen"));

        List<Medication> results = medicationDAO.findAll();

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(m -> m.getName().equals("Paracetamol")));
        assertTrue(results.stream().anyMatch(m -> m.getName().equals("Ibuprofen")));
    }

    @Test
    void findAll_emptyWhenNone() {
        List<Medication> results = medicationDAO.findAll();
        assertTrue(results.isEmpty());
    }

    @Test
    void findAll_orderedAlphabetically() {
        medicationDAO.insert(buildMedication("Paracetamol"));
        medicationDAO.insert(buildMedication("Aspirin"));
        medicationDAO.insert(buildMedication("Ibuprofen"));

        List<Medication> results = medicationDAO.findAll();

        assertEquals("Aspirin",     results.get(0).getName());
        assertEquals("Ibuprofen",   results.get(1).getName());
        assertEquals("Paracetamol", results.get(2).getName());
    }
}