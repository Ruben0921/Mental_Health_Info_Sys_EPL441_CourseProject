package com.skillonnet.automation.api;

/**
 * Simple JSON error payload ({@code error} field) for API failures.
 */
public class ApiError {

    private String error;

    /** Default constructor for JSON deserialization. */
    public ApiError() {
    }

    /** @param error short machine-readable error code or message */
    public ApiError(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
