package com.skillonnet.automation.api;

import com.skillonnet.automation.dao.UserDAO;
import com.skillonnet.automation.model.User;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.Optional;

/**
 * HTTP Basic authentication: validates credentials against {@link com.skillonnet.automation.dao.UserDAO}
 * and installs a {@link SecurityContext} with the user's role.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class BasicAuthFilter implements ContainerRequestFilter {

    /**
     * @param ctx request context; aborted with 401 if credentials are missing or invalid
     */
    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String auth = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth == null || auth.length() < 6 || !auth.substring(0, 6).equalsIgnoreCase("Basic ")) {
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"mental_health\"")
                    .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
                    .entity(new ApiError("unauthorized"))
                    .build());
            return;
        }
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(auth.substring(6).trim()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"mental_health\"")
                    .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
                    .entity(new ApiError("unauthorized"))
                    .build());
            return;
        }
        int colon = decoded.indexOf(':');
        if (colon < 0) {
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"mental_health\"")
                    .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
                    .entity(new ApiError("unauthorized"))
                    .build());
            return;
        }
        String username = decoded.substring(0, colon);
        String password = decoded.substring(colon + 1);
        Optional<User> user = new UserDAO().authenticate(username, password);
        if (user.isEmpty()) {
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"mental_health\"")
                    .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
                    .entity(new ApiError("unauthorized"))
                    .build());
            return;
        }
        User u = user.get();
        SecurityContext sc = new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return u::getUsername;
            }

            @Override
            public boolean isUserInRole(String role) {
                return role != null && role.equals(u.getRole());
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public String getAuthenticationScheme() {
                return SecurityContext.BASIC_AUTH;
            }
        };
        ctx.setSecurityContext(sc);
    }
}
