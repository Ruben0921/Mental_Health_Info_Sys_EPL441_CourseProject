package com.skillonnet.automation.api;

import com.skillonnet.automation.dao.AdverseReactionDAO;
import com.skillonnet.automation.dao.PatientDAO;
import com.skillonnet.automation.model.AdverseReaction;
import com.skillonnet.automation.model.Patient;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

/**
 * Clinical REST API for patient records ({@link Roles#CLINICAL}).
 */
@Path("patients")
public class PatientService {

    private final PatientDAO patientDAO = new PatientDAO();

	/**
     * Returns all patients.
     *
     * @param sc security context - must contain the {@code CLINICAL} or {@code RECEPTIONIST} role.
     * @return a (possibly empty) list of all {@link Patient} records.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Patient> list(@Context SecurityContext sc) {
        Authz.require(sc, Roles.CLINICAL, Roles.RECEPTIONIST);
        return patientDAO.findAll();
    }

    /**
     * Returns a single patient by id.
     *
     * @param sc security context - must contain the {@code CLINICAL} or {@code RECEPTIONIST} role.
     * @param id the primary key of the patient.
     * @return the matching {@link Patient}.
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Patient get(@Context SecurityContext sc, @PathParam("id") int id) {
        Authz.require(sc, Roles.CLINICAL, Roles.RECEPTIONIST);
        return patientDAO.findById(id).orElseThrow(NotFoundException::new);
    }

   /**
     * Updates an existing patient record.
	 * 
     * @param sc   security context - must contain the {@code CLINICAL} role.
     * @param id   the primary key of the patient to update.
     * @param body the updated patient data.
     * @return the updated {@link Patient}.
     */    
	@PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Patient put(@Context SecurityContext sc, @PathParam("id") int id, Patient body) {
        Authz.require(sc, Roles.CLINICAL);
        if (body.getPatientId() != 0 && body.getPatientId() != id) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        body.setPatientId(id);
        patientDAO.update(body);
        return patientDAO.findById(id).orElseThrow(NotFoundException::new);
    }
	
    /**
     * Creates a new patient record.
     *
     * @param sc  security context - must contain the {@code CLINICAL} role.
     * @param body the patient data to persist.
     * @return the newly created {@link Patient}.
     */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Patient create(@Context SecurityContext sc, Patient body) {
		Authz.require(sc, Roles.CLINICAL);
		int id = patientDAO.insert(body);
		return patientDAO.findById(id).orElseThrow(NotFoundException::new);
	}

	private final AdverseReactionDAO adverseReactionDAO = new AdverseReactionDAO();

    /**
     * Returns all adverse reactions recorded for a patient.
     *
     * @param sc security context - must contain the {@code CLINICAL} or {@code RECEPTIONIST} role.
     * @param id the primary key of the patient.
     * @return a (possibly empty) list of {@link AdverseReaction} records.
     */	
	@GET
	@Path("{id}/adverse-reactions")
	@Produces(MediaType.APPLICATION_JSON)
	public List<AdverseReaction> getAdverseReactions(@Context SecurityContext sc, @PathParam("id") int id) {
		Authz.require(sc, Roles.CLINICAL, Roles.RECEPTIONIST);
		return adverseReactionDAO.findByPatientId(id);
	}

    /**
     * Adds an adverse reaction for a patient.
     *
     * @param sc  security context - must contain the {@code CLINICAL} role.
     * @param patientId the primary key of the patient.
     * @param body the adverse reaction data to persist
     * @return the persisted {@link AdverseReaction} with its generated id.
     */
	@POST
	@Path("{id}/adverse-reactions")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public AdverseReaction addAdverseReaction(@Context SecurityContext sc, @PathParam("id") int patientId, AdverseReaction body) {
		Authz.require(sc, Roles.CLINICAL);
		body.setPatientId(patientId);
		int id = adverseReactionDAO.insert(body);
		body.setReactionId(id);
		return body;
	}

    /**
     * Removes an adverse reaction by id.
     *
     * @param sc security context - must contain the {@code CLINICAL} role.
     * @param patientId  the primary key of the patient.
     * @param reactionId the primary key of the adverse reaction to delete.
     */
	@DELETE
	@Path("{id}/adverse-reactions/{reactionId}")
	public void removeAdverseReaction(@Context SecurityContext sc, @PathParam("id") int patientId, @PathParam("reactionId") int reactionId) {
		Authz.require(sc, Roles.CLINICAL);
		adverseReactionDAO.delete(reactionId);
	}
}
