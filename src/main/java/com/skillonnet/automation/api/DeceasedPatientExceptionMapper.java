package com.skillonnet.automation.api;

import com.skillonnet.automation.exception.DeceasedPatientException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps {@link com.skillonnet.automation.exception.DeceasedPatientException} to HTTP 409.
 */
@Provider
public class DeceasedPatientExceptionMapper implements ExceptionMapper<DeceasedPatientException> {

    /** {@inheritDoc} */
    @Override
    public Response toResponse(DeceasedPatientException e) {
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiError("patient record locked (deceased)"))
                .build();
    }
}
