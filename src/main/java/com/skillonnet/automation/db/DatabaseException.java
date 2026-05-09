package com.skillonnet.automation.db;

/**
 * Unchecked wrapper for JDBC and configuration errors.
 */
public class DatabaseException extends RuntimeException {

    /** @param message error description */
    public DatabaseException(String message) {
        super(message);
    }

    /** @param message error description
     * @param cause underlying cause (often {@link java.sql.SQLException}) */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
