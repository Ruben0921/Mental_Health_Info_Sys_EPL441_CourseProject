package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.db.DatabaseException;
import com.skillonnet.automation.model.Clinic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC DAO for the {@code clinics} table.
 */
public class ClinicDAO {

    private final DBConnection db;

    public ClinicDAO() {
        this(DBConnection.getInstance());
    }

    public ClinicDAO(DBConnection db) {
        this.db = db;
    }

    /** Returns all clinics ordered by name. */
    public List<Clinic> findAll() {
        String sql = "SELECT clinic_id, name, location_type FROM clinics ORDER BY name";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            List<Clinic> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapRow(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new DatabaseException("ClinicDAO.findAll failed", e);
        }
    }

    /** Returns a single clinic by id. */
    public Optional<Clinic> findById(int clinicId) {
        String sql = "SELECT clinic_id, name, location_type FROM clinics WHERE clinic_id = ?";
        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("ClinicDAO.findById failed", e);
        }
        return Optional.empty();
    }

    private static Clinic mapRow(ResultSet rs) throws SQLException {
        Clinic c = new Clinic();
        c.setClinicId(rs.getInt("clinic_id"));
        c.setName(rs.getString("name"));
        c.setLocationType(rs.getString("location_type"));
        return c;
    }
}
