package com.skillonnet.automation.api;

import com.skillonnet.automation.db.DatabaseException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps {@link DatabaseException} to HTTP 500 with an {@link ApiError} body.
 */
@Provider
public class DatabaseExceptionMapper implements ExceptionMapper<DatabaseException> {

    /** {@inheritDoc} */
    @Override
    public Response toResponse(DatabaseException e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiError(e.getMessage()))
                .build();
    }
}
