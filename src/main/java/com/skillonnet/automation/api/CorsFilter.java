package com.skillonnet.automation.api;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Handles CORS for all requests
 */
@Provider
@PreMatching
public class CorsFilter implements ContainerResponseFilter {

    private static final String ALLOW_ORIGIN  = "Access-Control-Allow-Origin";
    private static final String ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String MAX_AGE       = "Access-Control-Max-Age";

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) throws IOException {
        // Add CORS headers to every response (including error responses)
        res.getHeaders().putSingle(ALLOW_ORIGIN,  "*");
        res.getHeaders().putSingle(ALLOW_HEADERS, "Authorization, Content-Type, Accept");
        res.getHeaders().putSingle(ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
        res.getHeaders().putSingle(MAX_AGE,       "86400");

        // Short-circuit preflight: respond 200 immediately
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            res.setStatus(Response.Status.OK.getStatusCode());
        }
    }
}
