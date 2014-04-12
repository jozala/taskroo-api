package pl.aetas.gtweb.service.security;

import pl.aetas.gtweb.data.SessionDao;
import pl.aetas.gtweb.domain.Role;
import pl.aetas.gtweb.domain.User;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Priority(Priorities.AUTHORIZATION)
@Provider
public class SecurityContextFilter implements ContainerRequestFilter {

    private final SessionDao sessionDao;

    @Inject
    public SecurityContextFilter(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // TODO this should use authorization header instead of session-id header
        final String sessionId = requestContext.getHeaderString("Session-Id");

        User user = null;
        Session session = null;

        if (sessionId != null && sessionId.length() > 0) {
            session = sessionDao.findOne(sessionId);

            if (null != session) {
                // TODO temporary solution to set roles. Roles should be saved in the session (whole User object should be saved there).
                Set<Role> roles = new HashSet<>();
                roles.add(Role.USER);
                user = new User(session.getUserId(), roles);
            }
        }

        requestContext.setSecurityContext(new GtWebSecurityContext(session, user));
    }
}
