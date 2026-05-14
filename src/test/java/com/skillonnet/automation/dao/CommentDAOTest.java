package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.model.Comment;
import com.skillonnet.automation.support.TestDbSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommentDAOTest {

    private CommentDAO commentDAO;

    @BeforeEach
    void setUp() {
        TestDbSupport.resetSchema();
        commentDAO = new CommentDAO();
    }

    @AfterEach
    void tearDown() {
        DBConnection.getInstance().closeConnection();
    }

    private Comment buildComment(int patientId, int clinicianId) {
        Comment c = new Comment();
        c.setPatientId(patientId);
        c.setClinicianId(clinicianId);
        c.setFreeFormText("Test comment");
        c.setCommentDate(LocalDate.of(2024, 6, 1));
        return c;
    }

    @Test
    void insertAndFindByPatientId() {
        Comment c = buildComment(1, 1);
        int id = commentDAO.insert(c);

        assertTrue(id > 0);
        assertEquals(id, c.getCommentId());

        List<Comment> results = commentDAO.findByPatientId(1);
        assertEquals(1, results.size());
        assertEquals("Test comment", results.get(0).getFreeFormText());
    }

    @Test
    void findByPatientId_returnsOnlyForThatPatient() {
        commentDAO.insert(buildComment(1, 1));
        commentDAO.insert(buildComment(1, 1));
        commentDAO.insert(buildComment(2, 1)); // different patient

        List<Comment> results = commentDAO.findByPatientId(1);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(c -> c.getPatientId() == 1));
    }

    @Test
    void findByPatientId_emptyWhenNone() {
        List<Comment> results = commentDAO.findByPatientId(99999);
        assertTrue(results.isEmpty());
    }

    @Test
    void insert_defaultsToTodayWhenDateIsNull() {
        Comment c = buildComment(1, 1);
        c.setCommentDate(null);
        commentDAO.insert(c);

        Comment loaded = commentDAO.findByPatientId(1).get(0);
        assertEquals(LocalDate.now(), loaded.getCommentDate());
    }

    @Test
    void findByPatientId_orderedByDateDescending() {
        Comment older = buildComment(1, 1);
        older.setCommentDate(LocalDate.of(2024, 1, 1));
        commentDAO.insert(older);

        Comment newer = buildComment(1, 1);
        newer.setCommentDate(LocalDate.of(2024, 6, 1));
        commentDAO.insert(newer);

        List<Comment> results = commentDAO.findByPatientId(1);

        // Most recent should come first
        assertTrue(results.get(0).getCommentDate()
                           .isAfter(results.get(1).getCommentDate()));
    }
}