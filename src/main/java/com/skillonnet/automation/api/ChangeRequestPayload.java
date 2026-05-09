package com.skillonnet.automation.api;

/**
 * JSON body for creating a change request (raw patient snapshot + requested edits).
 */
public class ChangeRequestPayload {

    private String rawPatientData;
    private String requestedChanges;

    public String getRawPatientData() {
        return rawPatientData;
    }

    public void setRawPatientData(String rawPatientData) {
        this.rawPatientData = rawPatientData;
    }

    public String getRequestedChanges() {
        return requestedChanges;
    }

    public void setRequestedChanges(String requestedChanges) {
        this.requestedChanges = requestedChanges;
    }
}
