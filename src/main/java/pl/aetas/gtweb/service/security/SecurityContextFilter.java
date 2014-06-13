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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
        // Authorization: GTWebAuth realm="gtweb@aetas.pl",tokenKey="=SessionIDString=",cnonce="uniqueValue"

        String authorizationHeader = requestContext.getHeaderString("Authorization");
        if (authorizationHeader == null) {
            requestContext.setSecurityContext(new GtWebSecurityContext(null, null));
            return;
        }

        Map<String, String> authHeaderMap = parseAuthorizationHeader(authorizationHeader);
        String tokenKey = authHeaderMap.get("tokenKey");

        User user = null;
        Session session = null;

        if (tokenKey != null && !tokenKey.isEmpty()) {
            session = sessionDao.findOne(tokenKey);

            if (null != session) {
                // TODO temporary solution to set roles. Roles should be saved in the session (whole User object should be saved there).
                Set<Role> roles = new HashSet<>();
                roles.add(Role.USER);
                user = new User(session.getUserId(), roles);
            }
        }

        requestContext.setSecurityContext(new GtWebSecurityContext(session, user));
    }

    private Map<String, String> parseAuthorizationHeader(String authorizationHeader) {
        Map<String, String> authHeaderMap = new HashMap<>();

        authorizationHeader = authorizationHeader.replace("GTWebAuth ", "");
        String[] authorizationHeaderElements = authorizationHeader.split(",");
        for (String authorizationHeaderElement : authorizationHeaderElements) {
            int equalIndex = authorizationHeaderElement.indexOf('=');
            String authHeaderElementKey = authorizationHeaderElement.substring(0, equalIndex).trim();
            String authHeaderElementValue = authorizationHeaderElement.substring(equalIndex+1).replaceAll("\"", "").trim();
            authHeaderMap.put(authHeaderElementKey, authHeaderElementValue);
        }
        return authHeaderMap;
    }
}
