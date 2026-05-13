package com.skillonnet.automation.dao;

import com.skillonnet.automation.db.DBConnection;
import com.skillonnet.automation.db.DatabaseException;
import com.skillonnet.automation.model.Condition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Condition reference data.
 */
public class ConditionRefDAO {

    private final DBConnection db;

    public ConditionRefDAO() {
        this(DBConnection.getInstance());
    }

    public ConditionRefDAO(DBConnection db) {
        this.db = db;
    }

    public List<Condition> findAll() {
        String sql = """
                SELECT condition_id,
                       name
                FROM `condition`
                ORDER BY name""";

        List<Condition> conditions = new ArrayList<>();

        try (Connection conn = db.newConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                conditions.add(map(rs));
            }

            return conditions;

        } catch (SQLException e) {
            throw new DatabaseException("find conditions failed", e);
        }
    }

    private Condition map(ResultSet rs) throws SQLException {
        Condition c = new Condition();

        c.setConditionId(rs.getInt("condition_id"));
        c.setName(rs.getString("name"));

        return c;
    }
}