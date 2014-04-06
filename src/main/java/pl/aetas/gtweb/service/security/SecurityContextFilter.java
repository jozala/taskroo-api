package pl.aetas.gtweb.service.security;

import pl.aetas.gtweb.data.SessionDao;
import pl.aetas.gtweb.data.UserDao;
import pl.aetas.gtweb.domain.User;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Priority(Priorities.AUTHENTICATION)
@Provider
public class SecurityContextFilter implements ContainerRequestFilter {

    private final SessionDao sessionDao;

    private final UserDao userDao;

    @Inject
    public SecurityContextFilter(SessionDao sessionDao, UserDao userDao) {
        this.sessionDao = sessionDao;
        this.userDao = userDao;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final String sessionId = requestContext.getHeaderString("session-id");

        User user = null;
        Session session = null;

        if (sessionId != null && sessionId.length() > 0) {
            session = sessionDao.findOne(sessionId);

            if (null != session) {
                user = userDao.findOne(session.getUserId());
            }
        }

        requestContext.setSecurityContext(new GtWebSecurityContext(session, user));
    }
}
