package com.taskroo.server;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class TaskRooResourceConfig extends ResourceConfig {
    public TaskRooResourceConfig() {
        register(ExceptionListener.class);
        register(RolesAllowedDynamicFeature.class);
        packages("com.taskroo.service", "com.wordnik.swagger.jersey.listing");
    }
}