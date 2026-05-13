package com.skillonnet.automation.model;

import java.time.LocalDate;

/** Scheduled consultation (maps to {@code appointment} table). */
public class Appointment {

    private int appointmentId;
    private int patientId;
    private int clinicId;
    private int staffId;
    private LocalDate appointmentDate;
    private String type;
    private String status;
    private boolean recordsUpdated;

    public int getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(int appointmentId) {
        this.appointmentId = appointmentId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public int getClinicId() {
        return clinicId;
    }

    public void setClinicId(int clinicId) {
        this.clinicId = clinicId;
    }

    public int getStaffId() {
        return staffId;
    }

    public void setStaffId(int staffId) {
        this.staffId = staffId;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isRecordsUpdated() {
        return recordsUpdated;
    }

    public void setRecordsUpdated(boolean recordsUpdated) {
        this.recordsUpdated = recordsUpdated;
    }
}
