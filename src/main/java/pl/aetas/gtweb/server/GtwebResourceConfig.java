package pl.aetas.gtweb.server;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class GtwebResourceConfig extends ResourceConfig {
    public GtwebResourceConfig() {
        register(ExceptionListener.class);
        register(RolesAllowedDynamicFeature.class);
        packages("pl.aetas.gtweb.service", "com.wordnik.swagger.jersey.listing");
    }
}