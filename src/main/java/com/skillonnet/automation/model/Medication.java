package com.skillonnet.automation.model;

/** Medication catalog entry (maps to {@code medication} table). */
public class Medication {

    private int medicationId;
    private String name;

    public int getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(int medicationId) {
        this.medicationId = medicationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
