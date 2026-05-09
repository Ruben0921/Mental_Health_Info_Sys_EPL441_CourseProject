package com.skillonnet.automation.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC access configured from {@value #CONFIG_RESOURCE} on the classpath (singleton).
 */
public final class DBConnection {

    private static final Logger LOG = Logger.getLogger(DBConnection.class.getName());
    private static final String CONFIG_RESOURCE = "config.properties";
    private static final String KEY_URL = "db.url";
    private static final String KEY_USER = "db.username";
    private static final String KEY_PASSWORD = "db.password";

    private static final class Holder {
        private static final DBConnection INSTANCE = new DBConnection();
    }

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private volatile Connection connection;

    private DBConnection() {
        Properties props = loadProperties();
        this.jdbcUrl = require(props, KEY_URL);
        this.username = require(props, KEY_USER);
        this.password = require(props, KEY_PASSWORD);
    }

    /** @return the shared instance */
    public static DBConnection getInstance() {
        return Holder.INSTANCE;
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
            if (in == null) {
                throw new DatabaseException("Missing classpath resource: " + CONFIG_RESOURCE);
            }
            props.load(in);
        } catch (IOException e) {
            throw new DatabaseException("Failed to read " + CONFIG_RESOURCE, e);
        }
        return props;
    }

    private static String require(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new DatabaseException("Missing or empty property: " + key);
        }
        return value.trim();
    }

    /** Opens a new JDBC connection (caller must close). */
    public Connection newConnection() {
        try {
            return DriverManager.getConnection(jdbcUrl, username, password);
        } catch (SQLException e) {
            logSqlException("open connection", e);
            throw new DatabaseException("Unable to connect to the database.", e);
        }
    }

    /** Returns a shared connection, opening one if needed. */
    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(jdbcUrl, username, password);
            }
        } catch (SQLException e) {
            logSqlException("open connection", e);
            throw new DatabaseException("Unable to connect to the database.", e);
        }
        return connection;
    }

    /** Closes the shared connection if present. */
    public synchronized void closeConnection() {
        if (connection == null) {
            return;
        }
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logSqlException("close connection", e);
            throw new DatabaseException("Unable to close the database connection.", e);
        } finally {
            connection = null;
        }
    }

    private static void logSqlException(String action, SQLException e) {
        LOG.log(Level.WARNING, action + " failed: SQLState={0}, errorCode={1}",
                new Object[] { e.getSQLState(), e.getErrorCode() });
        LOG.log(Level.FINER, "SQLException details", e);
    }
}
