package com.skillonnet.automation.api;

/**
 * JSON body for updating appointment attendance status (receptionist {@code PUT} on {@code appointments/{id}/attendance}).
 */
public class AttendanceUpdate {

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
