package com.skillonnet.automation.api;

import com.skillonnet.automation.dao.AppointmentDAO;
import com.skillonnet.automation.model.Appointment;
import com.skillonnet.automation.model.MissedPatientRow;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
 * Receptionist REST API for appointments ({@link Roles#RECEPTIONIST}).
 */
@Path("appointments")
public class AppointmentService {

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();

    /** Creates an appointment; server assigns {@link Appointment#getAppointmentId()}. */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Appointment create(@Context SecurityContext sc, Appointment body) {
        Authz.require(sc, Roles.RECEPTIONIST);
        appointmentDAO.insert(body);
        return appointmentDAO.findById(body.getAppointmentId()).orElseThrow();
    }

    /** Updates attendance status for an appointment. */
    @PUT
    @Path("{id}/attendance")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateAttendance(@Context SecurityContext sc, @PathParam("id") int id, AttendanceUpdate body) {
        Authz.require(sc, Roles.RECEPTIONIST);
        appointmentDAO.updateAttendance(id, body.getStatus());
    }

	@PUT
    @Path("{id}/update")
    @Consumes(MediaType.APPLICATION_JSON)
    public void markUpdated(@Context SecurityContext sc, @PathParam("id") int id, AppointmentRecordUpdated body) {
        Authz.require(sc, Roles.RECEPTIONIST, Roles.CLINICAL);
        appointmentDAO.markUpdated(id, body.getValue());
    }

    /** Patients who missed appointments on the given calendar date. */
    @GET
    @Path("missed")
    @Produces(MediaType.APPLICATION_JSON)
    public List<MissedPatientRow> missed(@Context SecurityContext sc, @QueryParam("date") String date) {
        Authz.require(sc, Roles.RECEPTIONIST);
        LocalDate d = LocalDate.parse(date);
        return appointmentDAO.findMissedPatientsByDate(d);
    }

    /** Appointments where clinical records have not been updated after attendance. */
    @GET
    @Path("pending-records")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Appointment> pendingRecords(@Context SecurityContext sc) {
        Authz.require(sc, Roles.RECEPTIONIST);
        return appointmentDAO.findAppointmentsWithRecordsNotUpdated();
    }
}
