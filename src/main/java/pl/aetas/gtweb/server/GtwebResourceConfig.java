package pl.aetas.gtweb.server;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class GtwebResourceConfig extends ResourceConfig {
    public GtwebResourceConfig() {
        register(JacksonFeature.class);
        register(new ExceptionListener());
        packages("pl.aetas.gtweb.service");
    }
}