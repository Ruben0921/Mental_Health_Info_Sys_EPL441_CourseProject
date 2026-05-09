package com.skillonnet.automation.model;

/** DTO: prescription totals grouped by medication. */
public class MedicationPrescriptionStat {

    private int medicationId;
    private String medicationName;
    private long prescriptionCount;

    public int getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(int medicationId) {
        this.medicationId = medicationId;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public long getPrescriptionCount() {
        return prescriptionCount;
    }

    public void setPrescriptionCount(long prescriptionCount) {
        this.prescriptionCount = prescriptionCount;
    }
}
