package com.skillonnet.automation.exception;

/**
 * Thrown when a deceased patient's record must not be modified or deleted.
 */
public class DeceasedPatientException extends RuntimeException {

    public DeceasedPatientException() {
        super("Patient record is read-only (deceased)");
    }
}
