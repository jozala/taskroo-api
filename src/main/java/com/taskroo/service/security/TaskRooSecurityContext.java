package com.taskroo.service.security;

import com.taskroo.domain.Role;
import com.taskroo.domain.User;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class TaskRooSecurityContext implements SecurityContext {

    private final Session session;
    private final User user;
    private final String authenticationServiceUrl;

    public TaskRooSecurityContext(Session session, User user, String authenticationServiceUrl) {
        this.session = session;
        this.user = user;
        this.authenticationServiceUrl = authenticationServiceUrl;
    }

    @Override
    public Principal getUserPrincipal() {
        return user;
    }

    @Override
    public boolean isUserInRole(String role) {
        if (null == session) {
            Response denied = Response.status(Response.Status.FORBIDDEN)
                    .header("WWW-Authenticate", "TaskRooAuth realm=\"taskroo@aetas.pl\",domain=\"" + authenticationServiceUrl + "\"")
                    .build();
            throw new WebApplicationException(denied);
        }
        return user.getRoles().contains(Role.valueOf(role.toUpperCase()));
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getAuthenticationScheme() {
        return "TaskRooAuth";
    }
}
