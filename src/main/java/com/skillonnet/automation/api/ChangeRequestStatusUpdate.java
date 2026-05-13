package com.skillonnet.automation.api;

/**
 * JSON body for updating a change request status
 * ({@code PUT /reports/change-requests/{id}/status}).
 */
public class ChangeRequestStatusUpdate {

    private String status; // Accepted | Rejected

    public String getStatus()             { return status; }
    public void   setStatus(String status){ this.status = status; }
}
