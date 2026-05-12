package com.skillonnet.automation.exception;

/**
 * Prescription blocked because the patient has a recorded adverse reaction to the medication.
 */
public class WarningException extends Exception {

    private final int patientId;
    private final int medicationId;

    /**
     * @param patientId patient identifier
     * @param medicationId medication identifier
     */
    public WarningException(int patientId, int medicationId) {
        super("Patient has recorded adverse reaction for this medication");
        this.patientId = patientId;
        this.medicationId = medicationId;
    }

    public int getPatientId() {
        return patientId;
    }

    public int getMedicationId() {
        return medicationId;
    }
}
