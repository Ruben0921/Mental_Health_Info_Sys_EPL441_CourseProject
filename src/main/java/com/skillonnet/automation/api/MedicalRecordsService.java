package com.skillonnet.automation.api;

import com.skillonnet.automation.dao.AppointmentDAO;
import com.skillonnet.automation.dao.ChangeRequestDAO;
import com.skillonnet.automation.dao.ClinicDAO;
import com.skillonnet.automation.dao.PatientDAO;
import com.skillonnet.automation.dao.UserDAO;
import com.skillonnet.automation.model.Appointment;
import com.skillonnet.automation.model.ChangeRequest;
import com.skillonnet.automation.model.Clinic;
import com.skillonnet.automation.model.MedicationPrescriptionStat;
import com.skillonnet.automation.model.Patient;
import com.skillonnet.automation.model.User;
import com.skillonnet.automation.model.ConditionPatientStat;
import com.skillonnet.automation.dao.ReportingDAO;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
// import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import java.time.LocalDate;
import java.util.List;

/**
 * Medical Records REST API — all endpoints require {@link Roles#MEDICAL_RECORDS}
 * unless noted otherwise.
 *
 * Routes:
 *   GET /records/clinics                  → List&lt;Clinic&gt;         (Clinical+Receptionist+Medical_Records)
 *   GET /records/users                    → List&lt;User&gt;           (Medical_Records — passwords redacted)
 *   GET /records/change-requests          → List&lt;ChangeRequest&gt;  (Medical_Records)
 *   PUT /records/patients/{id}/deceased   → Patient               (Medical_Records)
 *   GET /records/audit                    → List&lt;Appointment&gt;    (Medical_Records)
 */
@Path("records")
public class MedicalRecordsService {

    private final PatientDAO       patientDAO       = new PatientDAO();
    private final ClinicDAO        clinicDAO        = new ClinicDAO();
    private final UserDAO          userDAO          = new UserDAO();
    private final ChangeRequestDAO changeRequestDAO = new ChangeRequestDAO();
    private final AppointmentDAO   appointmentDAO   = new AppointmentDAO();
    private final ReportingDAO reportingDAO = new ReportingDAO();

    // ── Clinics ──────────────────────────────────────────────────────

    /**
     * Returns all clinics — used to populate clinic names in reports and the
     * new-appointment modal.
     * Accessible to: Clinical, Receptionist, Medical_Records.
     */
    @GET
    @Path("clinics")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Clinic> listClinics(@Context SecurityContext sc) {
        Authz.require(sc, Roles.CLINICAL, Roles.RECEPTIONIST, Roles.MEDICAL_RECORDS);
        return clinicDAO.findAll();
    }

    // ── Users ─────────────────────────────────────────────────────────

    /**
     * Returns all system users with passwords redacted.
     * Accessible to: Medical_Records only.
     */
    @GET
    @Path("users")
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> listUsers(@Context SecurityContext sc) {
        Authz.require(sc, Roles.MEDICAL_RECORDS);
        List<User> users = userDAO.findAll();
        users.forEach(u -> u.setPasswordHash("[redacted]"));
        return users;
    }

    // ── Change requests (list) ────────────────────────────────────────

    /**
     * Returns all change requests ordered newest-first.
     * Accessible to: Medical_Records only.
     */
    @GET
    @Path("change-requests")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ChangeRequest> listChangeRequests(@Context SecurityContext sc) {
        Authz.require(sc, Roles.MEDICAL_RECORDS);
        return changeRequestDAO.findAll();
    }

    // ── Deceased lock ─────────────────────────────────────────────────

    /**
     * Marks a patient as deceased and locks the record as read-only.
     * Uses {@link PatientDAO#lockDeceased(int)} which bypasses the
     * deceased-check guard in {@link PatientDAO#update} — that guard exists
     * to prevent editing a deceased record, not to prevent locking it.
     * Accessible to: Medical_Records only.
     * (General req. 18)
     */
    @PUT
    @Path("patients/{id}/deceased")
    @Produces(MediaType.APPLICATION_JSON)
    public Patient lockDeceased(@Context SecurityContext sc, @PathParam("id") int id) {
        Authz.require(sc, Roles.MEDICAL_RECORDS);
        // lockDeceased() throws DatabaseException (→ 500) if patient not found
        return patientDAO.lockDeceased(id);
    }


    // // ── Audit log ─────────────────────────────────────────────────────
 
    // /**
    //  * Returns ALL appointments in an optional date range as a transaction log.
    //  * When {@code from} and {@code to} are omitted, returns all appointments.
    //  * Accessible to: Medical_Records only.
    //  * (Medical Records req. 3)
    //  */
    // @GET
    // @Path("audit")
    // @Produces(MediaType.APPLICATION_JSON)
    // public List<Appointment> auditLog(
    //         @Context SecurityContext sc,
    //         @QueryParam("from") String from,
    //         @QueryParam("to")   String to) {
    //     Authz.require(sc, Roles.MEDICAL_RECORDS);
    //     if (from != null && !from.isBlank() && to != null && !to.isBlank()) {
    //         return appointmentDAO.findByDateRange(
    //                 java.time.LocalDate.parse(from),
    //                 java.time.LocalDate.parse(to));
    //     }
    //     // No date filter — return all appointments ordered by date desc
    //     // We don't have a findAll() on AppointmentDAO, so fall back to
    //     // a very wide date range covering the full realistic dataset
    //     return appointmentDAO.findByDateRange(
    //             java.time.LocalDate.of(2000, 1, 1),
    //             java.time.LocalDate.of(2099, 12, 31));
    // }
 
    // // ── Weekly report ─────────────────────────────────────────────────
 
    // /**
    //  * Returns all appointments in a Mon–Fri week date range for weekly
    //  * report generation. Both {@code from} and {@code to} are required.
    //  * Accessible to: Medical_Records only.
    //  * (Management viewpoint req. 2)
    //  */
    // @GET
    // @Path("weekly")
    // @Produces(MediaType.APPLICATION_JSON)
    // public List<Appointment> weeklyReport(
    //         @Context SecurityContext sc,
    //         @QueryParam("from") String from,
    //         @QueryParam("to")   String to) {
    //     Authz.require(sc, Roles.MEDICAL_RECORDS);
    //     if (from == null || from.isBlank() || to == null || to.isBlank()) {
    //         return List.of();
    //     }
    //     return appointmentDAO.findByDateRange(
    //             java.time.LocalDate.parse(from),
    //             java.time.LocalDate.parse(to));
    // }


    // ── Audit log ─────────────────────────────────────────────────────
 
    /**
     * Returns all appointments in an optional date range as a clinic session
     * transaction log. Optionally filtered by clinicId and staffId.
     * (Medical Records req. 3)
     */
    @GET
    @Path("audit")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Appointment> auditLog(
            @Context SecurityContext sc,
            @QueryParam("from")     String from,
            @QueryParam("to")       String to,
            @QueryParam("clinicId") @DefaultValue("0") int clinicId,
            @QueryParam("staffId")  @DefaultValue("0") int staffId) {
        Authz.require(sc, Roles.MEDICAL_RECORDS);
        LocalDate f = (from != null && !from.isBlank()) ? LocalDate.parse(from) : LocalDate.of(2000,1,1);
        LocalDate t = (to   != null && !to.isBlank())   ? LocalDate.parse(to)   : LocalDate.of(2099,12,31);
        List<Appointment> all = appointmentDAO.findByDateRange(f, t);
        // Client-side clinic/staff filter (avoids adding another DAO method for now)
        return all.stream()
                .filter(a -> clinicId == 0 || a.getClinicId() == clinicId)
                .filter(a -> staffId  == 0 || a.getStaffId()  == staffId)
                .toList();
    }
 
    // ── Weekly appointments ───────────────────────────────────────────
 
    /**
     * Returns appointments in a date range, optionally filtered by clinic.
     * (Management viewpoint req. 2)
     */
    @GET
    @Path("weekly")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Appointment> weeklyReport(
            @Context SecurityContext sc,
            @QueryParam("from")     String from,
            @QueryParam("to")       String to,
            @QueryParam("clinicId") @DefaultValue("0") int clinicId) {
        Authz.require(sc, Roles.MEDICAL_RECORDS);
        if (from == null || from.isBlank() || to == null || to.isBlank()) return List.of();
        List<Appointment> all = appointmentDAO.findByDateRange(LocalDate.parse(from), LocalDate.parse(to));
        if (clinicId > 0) {
            return all.stream().filter(a -> a.getClinicId() == clinicId).toList();
        }
        return all;
    }
 
    // ── Weekly prescription stats ─────────────────────────────────────
 
    /**
     * Prescription totals by medication for a date range and optional clinic.
     * Used for weekly report drug totals. (Management viewpoint req. 2)
     */
    @GET
    @Path("weekly/prescriptions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<MedicationPrescriptionStat> weeklyPrescriptions(
            @Context SecurityContext sc,
            @QueryParam("from")     String from,
            @QueryParam("to")       String to,
            @QueryParam("clinicId") @DefaultValue("0") int clinicId) {
        Authz.require(sc, Roles.MEDICAL_RECORDS);
        if (from == null || from.isBlank() || to == null || to.isBlank()) return List.of();
        return reportingDAO.prescriptionStatsByDateRange(
                LocalDate.parse(from), LocalDate.parse(to), clinicId);
    }
 
    // ── Weekly condition stats ────────────────────────────────────────
 
    /**
     * Patient counts by condition for a date range and optional clinic.
     * Used for weekly report condition summary. (Management viewpoint req. 2)
     */
    @GET
    @Path("weekly/conditions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ConditionPatientStat> weeklyConditions(
            @Context SecurityContext sc,
            @QueryParam("from")     String from,
            @QueryParam("to")       String to,
            @QueryParam("clinicId") @DefaultValue("0") int clinicId) {
        Authz.require(sc, Roles.MEDICAL_RECORDS);
        if (from == null || from.isBlank() || to == null || to.isBlank()) return List.of();
        return reportingDAO.conditionStatsByDateRange(
                LocalDate.parse(from), LocalDate.parse(to), clinicId);
    }
}