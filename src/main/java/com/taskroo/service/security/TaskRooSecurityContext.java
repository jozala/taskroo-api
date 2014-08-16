package com.taskroo.service.security;

import com.taskroo.domain.Role;
import com.taskroo.domain.User;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class TaskRooSecurityContext implements SecurityContext {

    private final SecurityToken securityToken;
    private final User user;
    private final String authenticationServiceUrl;

    public TaskRooSecurityContext(SecurityToken securityToken, User user, String authenticationServiceUrl) {
        this.securityToken = securityToken;
        this.user = user;
        this.authenticationServiceUrl = authenticationServiceUrl;
    }

    @Override
    public Principal getUserPrincipal() {
        return user;
    }

    @Override
    public boolean isUserInRole(String role) {
        if (null == securityToken) {
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
