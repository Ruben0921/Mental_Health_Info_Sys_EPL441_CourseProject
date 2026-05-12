package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.db.DatabaseException;
import com.skillonnet.automation.model.Comment;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Comment rows linked to patients.
 */
public class CommentDAO {

    private final DBConnection db;

    public CommentDAO() {
        this(DBConnection.getInstance());
    }

    public CommentDAO(DBConnection db) {
        this.db = db;
    }

    public List<Comment> findByPatientId(int patientId) {
        String sql = """
                SELECT cm.comment_id,
                       cm.patient_id,
                       cm.clinician_id,
                       u.username AS clinician_name,
                       cm.free_form_text,
                       cm.comment_date
                FROM comment cm
                LEFT JOIN users u
                    ON u.user_id = cm.clinician_id
                WHERE cm.patient_id = ?
                ORDER BY cm.comment_date DESC,
                         cm.comment_id DESC""";

        List<Comment> comments = new ArrayList<>();

        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, patientId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comments.add(map(rs));
                }
            }

            return comments;

        } catch (SQLException e) {
            throw new DatabaseException("find comments failed", e);
        }
    }

    /**
     * @return generated comment_id
     */
    public int insert(Comment c) {
        String sql = """
                INSERT INTO comment
                (patient_id, clinician_id, free_form_text, comment_date)
                VALUES (?, ?, ?, ?)""";

        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, c.getPatientId());
            ps.setInt(2, c.getClinicianId());
            ps.setString(3, c.getFreeFormText());

            if (c.getCommentDate() == null) {
                ps.setDate(4, Date.valueOf(LocalDate.now()));
            } else {
                ps.setDate(4, Date.valueOf(c.getCommentDate()));
            }

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("No generated key for comment insert");
                }

                int id = keys.getInt(1);
                c.setCommentId(id);

                return id;
            }

        } catch (SQLException e) {
            throw new DatabaseException("insert comment failed", e);
        }
    }

    private Comment map(ResultSet rs) throws SQLException {
        Comment c = new Comment();

        c.setCommentId(rs.getInt("comment_id"));
        c.setPatientId(rs.getInt("patient_id"));
        c.setClinicianId(rs.getInt("clinician_id"));
        c.setFreeFormText(rs.getString("free_form_text"));

        Date commentDate = rs.getDate("comment_date");
        if (commentDate != null) {
            c.setCommentDate(commentDate.toLocalDate());
        }

        return c;
    }
}