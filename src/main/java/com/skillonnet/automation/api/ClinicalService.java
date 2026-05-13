package com.skillonnet.automation.api;

import com.skillonnet.automation.dao.*;
import com.skillonnet.automation.model.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

/**
 * Endpoints specific to the clinical role
 * All endpoints require {@link Roles#CLINICAL}.
 */
@Path("clinical")
public class ClinicalService {

    private final AppointmentDAO appointmentDAO       = new AppointmentDAO();
    private final PrescriptionDAO prescriptionDAO     = new PrescriptionDAO();
    private final PatientConditionDAO conditionDAO    = new PatientConditionDAO();
    private final CommentDAO commentDAO               = new CommentDAO();
    private final MedicationDAO medicationDAO         = new MedicationDAO();
    private final ConditionRefDAO conditionRefDAO     = new ConditionRefDAO();

	/**
     * Returns all appointments belonging to a specific patient.
	 * 
     * @param sc security context - must contain the {@code CLINICAL} role.
     * @param patientId primary key of the patient
     * @return a (possibly empty) list of {@link Appointment} objects serialised as JSON.
     */
    @GET
    @Path("patients/{id}/appointments")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Appointment> patientAppointments(@Context SecurityContext sc,
                                                  @PathParam("id") int patientId) {
        Authz.require(sc, Roles.CLINICAL);
        return appointmentDAO.findByPatientId(patientId);
    }

	/**
     * Retrieves the prescription associated with the specified appointment.
	 * 
     * @param sc security context - must contain the {@code CLINICAL} role.
     * @param appointmentId primary key of the appointment.
     * @return the {@link Prescription} for the appointment, or {@code null} when none exists.
     */
    @GET
    @Path("appointments/{id}/prescription")
    @Produces(MediaType.APPLICATION_JSON)
    public Prescription getPrescription(@Context SecurityContext sc,
                                         @PathParam("id") int appointmentId) {
        Authz.require(sc, Roles.CLINICAL);
        return prescriptionDAO.findByAppointmentId(appointmentId);
    }

	/**
     * Creates or replaces the prescription for the specified appointment.
     *
     * @param sc security context - must contain the {@code CLINICAL} role.
     * @param appointmentId the surrogate primary key of the appointment.
     * @param body          the prescription data to persist
     * @return the persisted {@link Prescription} from the database.
    */
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

    /**
     * Retrieves the clinical condition recorded of the specified appointment.
     *
     * @param sc security context - must contain the {@code CLINICAL} role.
     * @param appointmentId the surrogate primary key of the appointment.
     * @return the {@link PatientCondition} for the appointment, or {@code null} if none
     * 
     */
    @GET
    @Path("appointments/{id}/condition")
    @Produces(MediaType.APPLICATION_JSON)
    public PatientCondition getCondition(@Context SecurityContext sc,
                                          @PathParam("id") int appointmentId) {
        Authz.require(sc, Roles.CLINICAL);
        return conditionDAO.findByAppointmentId(appointmentId);
    }

	/**
     * Sets or updates the clinical condition for the specified appointment.
     *
     * @param sc security context - must contain the {@code CLINICAL} role.
     * @param appointmentId the surrogate primary key of the appointment.
     * @param body the condition data to persist.
     * @return the persisted {@link PatientCondition}
     */
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


	/**
     * Returns all free-form clinical comments of a patient.
	 * 
     * @param sc security context - must contain the {@code CLINICAL} role.
     * @param patientId the primary key of the patient.
     * @return a (possibly empty) list of {@link Comment} objects.
     */
    @GET
    @Path("patients/{id}/comments")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Comment> getComments(@Context SecurityContext sc,
                                      @PathParam("id") int patientId) {
        Authz.require(sc, Roles.CLINICAL);
        return commentDAO.findByPatientId(patientId);
    }

	/**
     * Adds a new free-form clinical comment to a patient's record.
	 * 
     * @param sc security context - must contain the {@code CLINICAL} role.
     * @param patientId the primary key of the patient.
     * @param body      the comment to persist
     * @return HTTP 201 Created with a {@link CreatedId} body containing the new record id.
     */
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


	/**
     * Returns the full list of available medications.
	 * 
     * @param sc  security context - must contain the {@code CLINICAL} role.
     * @return the complete list of {@link Medication} records.
     */
    @GET
    @Path("medications")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Medication> listMedications(@Context SecurityContext sc) {
        Authz.require(sc, Roles.CLINICAL);
        return medicationDAO.findAll();
    }

	/**
     * Returns the full list of recognised clinical conditions.
     *
     * @param sc security context - must contain the {@code CLINICAL} role.
     * @return the complete list of {@link Condition} records.
	*/
    @GET
    @Path("conditions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Condition> listConditions(@Context SecurityContext sc) {
        Authz.require(sc, Roles.CLINICAL);
        return conditionRefDAO.findAll();
    }
}