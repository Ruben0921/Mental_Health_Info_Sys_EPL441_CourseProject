package com.skillonnet.automation.api;

/**
 * Response wrapper returning a generated id (e.g. change request id).
 */
public class CreatedId {

    private int requestId;

    public CreatedId() {
    }

    /** @param requestId generated entity id (e.g. change request) */
    public CreatedId(int requestId) {
        this.requestId = requestId;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }
}
