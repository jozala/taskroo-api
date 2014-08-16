package com.taskroo.service.security;

import com.taskroo.domain.Role;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

public class SecurityToken implements Serializable {

    private static final long serialVersionUID = -7483170872697362182L;

    private final String id;
    private final String userId;
    private final Set<Role> roles;

    private final Date createTime;
    private final Date lastAccessedTime;

    public SecurityToken(String id, String userId, Set<Role> roles, Date createTime, Date lastAccessedTime) {
        this.id = id;
        this.userId = userId;
        this.roles = roles;
        this.createTime = createTime;
        this.lastAccessedTime = lastAccessedTime;
    }

    public String getId() {
        return id;
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
