package com.skillonnet.automation.model;

/** Patient-initiated change request stored for medical records review. */
public class ChangeRequest {

    private int requestId;
    private String rawPatientData;
    private String requestedChanges;
    private String status;

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
