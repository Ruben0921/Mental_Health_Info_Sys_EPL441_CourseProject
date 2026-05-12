package com.skillonnet.automation.model;

import java.time.LocalDate;

/** Prescriber acknowledgement when overriding an allergy/warning. */
public class WarningOverride {

    private int overrideId;
    private int prescriberId;
    private int medicationId;
    private String warningDetails;
    private LocalDate overrideDate;

    public int getOverrideId() {
        return overrideId;
    }

    public void setOverrideId(int overrideId) {
        this.overrideId = overrideId;
    }

    public int getPrescriberId() {
        return prescriberId;
    }

    public void setPrescriberId(int prescriberId) {
        this.prescriberId = prescriberId;
    }

    public int getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(int medicationId) {
        this.medicationId = medicationId;
    }

    public String getWarningDetails() {
        return warningDetails;
    }

    public void setWarningDetails(String warningDetails) {
        this.warningDetails = warningDetails;
    }

    public LocalDate getOverrideDate() {
        return overrideDate;
    }

    public void setOverrideDate(LocalDate overrideDate) {
        this.overrideDate = overrideDate;
    }
}
