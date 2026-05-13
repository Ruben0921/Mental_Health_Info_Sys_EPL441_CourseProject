package com.skillonnet.automation.model;

/** Clinic or health centre (maps to {@code clinics} table). */
public class Clinic {

    private int clinicId;
    private String name;
    private String locationType;

    public int getClinicId() {
        return clinicId;
    }

    public void setClinicId(int clinicId) {
        this.clinicId = clinicId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }
}
