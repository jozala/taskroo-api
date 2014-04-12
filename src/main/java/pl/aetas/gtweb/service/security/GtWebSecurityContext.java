package pl.aetas.gtweb.service.security;

import pl.aetas.gtweb.domain.Role;
import pl.aetas.gtweb.domain.User;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class GtWebSecurityContext implements SecurityContext {

    private final Session session;
    private final User user;

    public GtWebSecurityContext(Session session, User user) {
        this.session = session;
        this.user = user;
    }

    @Override
    public Principal getUserPrincipal() {
        return user;
    }

    @Override
    public boolean isUserInRole(String role) {
        if (null == session) {
            // TODO find more about returning WWW-Authenticate: Basic realm="insert realm"
            Response denied = Response.status(Response.Status.FORBIDDEN).build();
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
        return SecurityContext.BASIC_AUTH;
    }
}
