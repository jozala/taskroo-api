package pl.aetas.gtweb.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.aetas.gtweb.data.ConcurrentTasksModificationException;
import pl.aetas.gtweb.data.NonExistingResourceOperationException;
import pl.aetas.gtweb.data.TaskDao;
import pl.aetas.gtweb.data.UnsupportedDataOperationException;
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
import java.util.Objects;

@Singleton
@RolesAllowed("user")
@Path("tasks")
public class TasksService {

    private static final Logger LOGGER = LogManager.getLogger();

    private final TaskDao taskDao;

    @Inject
    public TasksService(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll(@Context SecurityContext sc) {
        Collection<Task> tasks = taskDao.findAllByOwnerId(sc.getUserPrincipal().getName());
        return Response.ok(tasks).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext sc, Task task) {
        LOGGER.info("Create task request received");
        task.setOwnerId(sc.getUserPrincipal().getName());
        for (Tag tag : task.getTags()) {
            tag.setOwnerId(sc.getUserPrincipal().getName());
        }
        try {
            Task savedTask = taskDao.insert(task);
            LOGGER.debug("New task created for customer: {} with task id: {}", task.getOwnerId(), task.getId());
            return Response.created(URI.create("tasks/" + savedTask.getId())).entity(savedTask).build();
        } catch (UnsupportedDataOperationException e) {
            LOGGER.warn("Unsupported data operation when trying to insert task", e);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    @DELETE
    @Path("{taskId}")
    public Response delete(@Context SecurityContext sc, @PathParam("taskId") String id) {
        LOGGER.debug("Delete task request received");
        try {
            taskDao.remove(sc.getUserPrincipal().getName(), id);
        } catch(NonExistingResourceOperationException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @PUT
    @Path("{taskId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@Context SecurityContext sc, @PathParam("taskId") String id, Task task) {
        LOGGER.debug("Update task request received");
        if (task == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        task.setId(id);
        for (Tag tag : task.getTags()) {
            tag.setOwnerId(sc.getUserPrincipal().getName());
        }
        try {
            Task taskAfterUpdate = taskDao.update(sc.getUserPrincipal().getName(), task);
            return Response.ok(taskAfterUpdate).build();
        } catch (NonExistingResourceOperationException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (UnsupportedDataOperationException e) {
            LOGGER.warn("Unsupported data operation when trying to update task", e);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    @POST
    @Path("{parentTaskId}/subtasks/{subtaskId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addSubtask(@Context SecurityContext sc, @PathParam("parentTaskId") String parentTaskId,
                               @PathParam("subtaskId") String subtaskId) {
        Objects.requireNonNull(parentTaskId);
        Objects.requireNonNull(subtaskId);

        try {
            Task parentTaskAfterUpdate = taskDao.addSubtask(sc.getUserPrincipal().getName(), parentTaskId, subtaskId);
            return Response.ok(parentTaskAfterUpdate).build();
        } catch (NonExistingResourceOperationException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (UnsupportedDataOperationException e) {
            LOGGER.warn("Unsupported data operation when trying to add subtask", e);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        } catch (ConcurrentTasksModificationException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("{subtaskId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response moveToTopLevel(@Context SecurityContext sc, @PathParam("subtaskId") String subtaskId) {
        Objects.requireNonNull(subtaskId);
        try {
            Task taskAfterUpdate = taskDao.moveToTopLevel(sc.getUserPrincipal().getName(), subtaskId);
            return Response.ok(taskAfterUpdate).build();
        } catch (NonExistingResourceOperationException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}


