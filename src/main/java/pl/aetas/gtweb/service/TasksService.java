package pl.aetas.gtweb.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.aetas.gtweb.data.TagDao;
import pl.aetas.gtweb.data.TaskDao;
import pl.aetas.gtweb.domain.Tag;
import pl.aetas.gtweb.domain.Task;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.Collection;

@Singleton
@RolesAllowed("user")
@Path("tasks")
public class TasksService {

    private static final Logger LOGGER = LogManager.getLogger();

    private final TaskDao taskDao;
    private final TagDao tagDao;

    @Inject
    public TasksService(TaskDao taskDao, TagDao tagDao) {
        this.taskDao = taskDao;
        this.tagDao = tagDao;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll(@Context SecurityContext sc) {
        Collection<Task> tasks = taskDao.findAllByOwnerId(sc.getUserPrincipal().getName());
        return Response.ok(tasks).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext sc, Task task) {
        LOGGER.info("Create task request received");
        task.setOwnerId(sc.getUserPrincipal().getName());
        for (Tag tag : task.getTags()) {
            tag.setOwnerId(sc.getUserPrincipal().getName());
            if (!tagDao.exists(tag)) {
                LOGGER.warn("Client tried to create task with non-existing tags. Bad Request.");
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
        }
        Task savedTask = taskDao.insert(task);

        LOGGER.debug("New task created for customer: {} with task id: {}", task.getOwnerId(), task.getId());
        return Response.created(URI.create("tasks/" + savedTask.getId())).entity(savedTask).build();
    }

}


