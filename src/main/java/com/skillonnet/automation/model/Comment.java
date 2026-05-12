package com.skillonnet.automation.model;

import java.time.LocalDate;

/** Clinician free-text comment on a patient record (domain model). */
public class Comment {

    private int commentId;
    private int patientId;
    private int clinicianId;
    private String freeFormText;
    private LocalDate commentDate;

    public int getCommentId() {
        return commentId;
    }

    public void setCommentId(int commentId) {
        this.commentId = commentId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public int getClinicianId() {
        return clinicianId;
    }

    public void setClinicianId(int clinicianId) {
        this.clinicianId = clinicianId;
    }

    public String getFreeFormText() {
        return freeFormText;
    }

    public void setFreeFormText(String freeFormText) {
        this.freeFormText = freeFormText;
    }

    public LocalDate getCommentDate() {
        return commentDate;
    }

    public void setCommentDate(LocalDate commentDate) {
        this.commentDate = commentDate;
    }
}
