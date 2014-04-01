package pl.aetas.gtweb.server;

import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class JettyRunner {

    public static void main(String[] args) throws Exception {
        URI baseUri = UriBuilder.fromUri("http://localhost/").port(8080).build();
        ResourceConfig config = new GtwebResourceConfig();
        Server server = JettyHttpContainerFactory.createServer(baseUri, config);
        server.start();
        server.join();

    }
}
