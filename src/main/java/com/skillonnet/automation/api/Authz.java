package com.skillonnet.automation.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Role checks for JAX-RS resources; throws 403 if the caller lacks any of the allowed roles.
 */
public final class Authz {

    private Authz() {
    }

    /**
     * @param sc security context from the request
     * @param roles allowed role names (caller must match at least one)
     * @throws jakarta.ws.rs.WebApplicationException with status 403 if unauthorized
     */
    public static void require(SecurityContext sc, String... roles) {
        for (String r : roles) {
            if (sc.isUserInRole(r)) {
                return;
            }
        }
        throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiError("forbidden"))
                .build());
    }
}
