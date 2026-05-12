package com.skillonnet.automation.model;

/** Mental health patient demographics and risk flags (maps to {@code patients} table). */
public class Patient {

    private int patientId;
    private String firstName;
    private String lastName;
    private String address;
    private boolean homeless;
    private String riskStatus;
    private boolean deceased;
    private boolean selfHarmHistory;

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isHomeless() {
        return homeless;
    }

    public void setHomeless(boolean homeless) {
        this.homeless = homeless;
    }

    public String getRiskStatus() {
        return riskStatus;
    }

    public void setRiskStatus(String riskStatus) {
        this.riskStatus = riskStatus;
    }

    public boolean isDeceased() {
        return deceased;
    }

    public void setDeceased(boolean deceased) {
        this.deceased = deceased;
    }

    public boolean isSelfHarmHistory() {
        return selfHarmHistory;
    }

    public void setSelfHarmHistory(boolean selfHarmHistory) {
        this.selfHarmHistory = selfHarmHistory;
    }
}
