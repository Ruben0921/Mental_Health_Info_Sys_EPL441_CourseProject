package com.skillonnet.automation.support;

import com.skillonnet.automation.db.DBConnection;
import org.h2.tools.RunScript;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

public final class TestDbSupport {

    private TestDbSupport() {
    }

    public static void resetSchema() {
        DBConnection db = DBConnection.getInstance();
        db.closeConnection();
        try (Connection c = db.newConnection(); Statement st = c.createStatement()) {
            st.execute("DROP ALL OBJECTS");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            db.closeConnection();
        }
        try (Connection c = db.newConnection();
                var reader = new InputStreamReader(
                        Objects.requireNonNull(
                                TestDbSupport.class.getResourceAsStream("/schema.sql"),
                                "schema.sql"),
                        StandardCharsets.UTF_8)) {
            RunScript.execute(c, reader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
