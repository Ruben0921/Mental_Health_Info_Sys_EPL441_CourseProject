package com.skillonnet.automation.api;

import com.skillonnet.automation.dao.*;
import com.skillonnet.automation.model.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

/**
 * Clinical REST API extensions for consultations, prescriptions, conditions,
 * comments, and medication lookup.
 *
 * All endpoints require {@link Roles#CLINICAL}.
 *
 * New routes:
 *   GET  /clinical/patients/{id}/appointments        → appointments for a patient
 *   GET  /clinical/appointments/{id}/prescription    → prescription for an appointment
 *   POST /clinical/appointments/{id}/prescription    → create/replace prescription
 *   GET  /clinical/appointments/{id}/condition       → condition recorded on appointment
 *   PUT  /clinical/appointments/{id}/condition       → set/update condition
 *   GET  /clinical/patients/{id}/comments            → all comments on a patient
 *   POST /clinical/patients/{id}/comments            → add a free-form comment
 *   GET  /clinical/medications                       → full medication list (for dropdowns)
 *   GET  /clinical/conditions                        → full condition list (for dropdowns)
 */
@Path("clinical")
public class ClinicalService {

    private final AppointmentDAO appointmentDAO       = new AppointmentDAO();
    private final PrescriptionDAO prescriptionDAO     = new PrescriptionDAO();
    private final PatientConditionDAO conditionDAO    = new PatientConditionDAO();
    private final CommentDAO commentDAO               = new CommentDAO();
    private final MedicationDAO medicationDAO         = new MedicationDAO();
    private final ConditionRefDAO conditionRefDAO     = new ConditionRefDAO();

    // ── Appointments for a patient ────────────────────────────────────

    @GET
    @Path("patients/{id}/appointments")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Appointment> patientAppointments(@Context SecurityContext sc,
                                                  @PathParam("id") int patientId) {
        Authz.require(sc, Roles.CLINICAL);
        return appointmentDAO.findByPatientId(patientId);
    }

    // ── Prescription for an appointment ──────────────────────────────

    @GET
    @Path("appointments/{id}/prescription")
    @Produces(MediaType.APPLICATION_JSON)
    public Prescription getPrescription(@Context SecurityContext sc,
                                         @PathParam("id") int appointmentId) {
        Authz.require(sc, Roles.CLINICAL);
        return prescriptionDAO.findByAppointmentId(appointmentId);
    }

    @POST
    @Path("appointments/{id}/prescription")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Prescription savePrescription(@Context SecurityContext sc,
                                          @PathParam("id") int appointmentId,
                                          Prescription body) {
        Authz.require(sc, Roles.CLINICAL);
        body.setAppointmentId(appointmentId);
        prescriptionDAO.upsert(body);
        return prescriptionDAO.findByAppointmentId(appointmentId);
    }

    // ── Condition for an appointment (patient_condition) ─────────────

    @GET
    @Path("appointments/{id}/condition")
    @Produces(MediaType.APPLICATION_JSON)
    public PatientCondition getCondition(@Context SecurityContext sc,
                                          @PathParam("id") int appointmentId) {
        Authz.require(sc, Roles.CLINICAL);
        return conditionDAO.findByAppointmentId(appointmentId);
    }

    @PUT
    @Path("appointments/{id}/condition")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public PatientCondition saveCondition(@Context SecurityContext sc,
                                           @PathParam("id") int appointmentId,
                                           PatientCondition body) {
        Authz.require(sc, Roles.CLINICAL);
        body.setAppointmentId(appointmentId);
        conditionDAO.upsert(body);
        return conditionDAO.findByAppointmentId(appointmentId);
    }

    // ── Comments on a patient ─────────────────────────────────────────

    @GET
    @Path("patients/{id}/comments")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Comment> getComments(@Context SecurityContext sc,
                                      @PathParam("id") int patientId) {
        Authz.require(sc, Roles.CLINICAL);
        return commentDAO.findByPatientId(patientId);
    }

    @POST
    @Path("patients/{id}/comments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addComment(@Context SecurityContext sc,
                                @PathParam("id") int patientId,
                                Comment body) {
        Authz.require(sc, Roles.CLINICAL);
        body.setPatientId(patientId);
        int id = commentDAO.insert(body);
        return Response.status(Response.Status.CREATED)
                       .entity(new CreatedId(id)).build();
    }

    // ── Reference data lookups ────────────────────────────────────────

    @GET
    @Path("medications")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Medication> listMedications(@Context SecurityContext sc) {
        Authz.require(sc, Roles.CLINICAL);
        return medicationDAO.findAll();
    }

    @GET
    @Path("conditions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Condition> listConditions(@Context SecurityContext sc) {
        Authz.require(sc, Roles.CLINICAL);
        return conditionRefDAO.findAll();
    }
}