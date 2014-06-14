package pl.aetas.gtweb.service.security;

import pl.aetas.gtweb.domain.Role;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

public class Session implements Serializable {

    private static final long serialVersionUID = -7483170872697362182L;

    private final String sessionId;
    private final String userId;
    private final Set<Role> roles;

    private final Date createTime;
    private final Date lastAccessedTime;

    public Session(String sessionId, String userId, Set<Role> roles, Date createTime, Date lastAccessedTime) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.roles = roles;
        this.createTime = createTime;
        this.lastAccessedTime = lastAccessedTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public Date getLastAccessedTime() {
        return lastAccessedTime;
    }

    public Set<Role> getRoles() {
        return roles;
    }
}
