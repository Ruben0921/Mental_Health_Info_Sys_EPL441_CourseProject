package com.skillonnet.automation.api;

/**
 * JSON body for setting an appointment record as updated (clinician {@code PUT} on {@code appointments/{id}/update}).
 */
public class AppointmentRecordUpdated {

    private boolean value;

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }
}
