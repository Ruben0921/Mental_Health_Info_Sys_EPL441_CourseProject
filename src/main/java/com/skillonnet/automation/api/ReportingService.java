package com.skillonnet.automation.api;

import com.skillonnet.automation.dao.ChangeRequestDAO;
import com.skillonnet.automation.dao.ReportingDAO;
import com.skillonnet.automation.model.ClinicPatientCount;
import com.skillonnet.automation.model.MedicationPrescriptionStat;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

/**
 * Medical records REST API for aggregate reports and change requests ({@link Roles#MEDICAL_RECORDS}).
 */
@Path("reports")
public class ReportingService {

    private final ReportingDAO reportingDAO = new ReportingDAO();
    private final ChangeRequestDAO changeRequestDAO = new ChangeRequestDAO();

    /** Distinct patient counts per clinic (via appointments). */
    @GET
    @Path("patients-per-clinic")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ClinicPatientCount> patientsPerClinic(@Context SecurityContext sc) {
        Authz.require(sc, Roles.MEDICAL_RECORDS);
        return reportingDAO.countDistinctPatientsPerClinic();
    }

    /** Prescription row counts grouped by medication. */
    @GET
    @Path("prescription-stats")
    @Produces(MediaType.APPLICATION_JSON)
    public List<MedicationPrescriptionStat> prescriptionStats(@Context SecurityContext sc) {
        Authz.require(sc, Roles.MEDICAL_RECORDS);
        return reportingDAO.prescriptionTotalsByMedication();
    }

    /** Stores a patient data change request for medical records review. */
    @POST
    @Path("change-requests")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createChangeRequest(@Context SecurityContext sc, ChangeRequestPayload body) {
        Authz.require(sc, Roles.MEDICAL_RECORDS);
        int id = changeRequestDAO.insert(body.getRawPatientData(), body.getRequestedChanges());
        return Response.status(Response.Status.CREATED).entity(new CreatedId(id)).build();
    }
}
