package com.taskroo.domain;

import java.security.Principal;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class User implements Principal {

    private final String userId;
    private final Set<Role> roles;

    public User(String userId, Set<Role> roles) {
        this.userId = Objects.requireNonNull(userId);
        this.roles = Collections.unmodifiableSet(roles);
    }

    @Override
    public String getName() {
        return userId;
    }

    public Set<Role> getRoles() {
        return roles;
    }
}
