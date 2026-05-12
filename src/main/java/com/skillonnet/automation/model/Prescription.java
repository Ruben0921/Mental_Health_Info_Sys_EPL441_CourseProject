package com.skillonnet.automation.model;

import java.time.LocalDate;

/** Medication issued in context of an appointment (maps to {@code prescription} table). */
public class Prescription {

    private int prescriptionId;
    private int appointmentId;
    private int medicationId;
    private int prescriberId;
    private LocalDate issueDate;
    private boolean repeat;

    public int getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(int prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public int getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(int appointmentId) {
        this.appointmentId = appointmentId;
    }

    public int getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(int medicationId) {
        this.medicationId = medicationId;
    }

    public int getPrescriberId() {
        return prescriberId;
    }

    public void setPrescriberId(int prescriberId) {
        this.prescriberId = prescriberId;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }
}
