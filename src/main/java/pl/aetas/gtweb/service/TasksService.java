package pl.aetas.gtweb.service;

import pl.aetas.gtweb.domain.Task;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Singleton
@Path("tasks")
public class TasksService {

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    public Task retrieveTasks() {
        return new Task("title", "description", false, null, null, null, null, null, null);

    }

}


