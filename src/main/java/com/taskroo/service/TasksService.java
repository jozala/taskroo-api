package com.taskroo.service;

import com.taskroo.data.ConcurrentTasksModificationException;
import com.taskroo.data.NonExistingResourceOperationException;
import com.taskroo.data.TaskDao;
import com.taskroo.data.UnsupportedDataOperationException;
import com.taskroo.domain.Tag;
import com.taskroo.domain.Task;
import com.wordnik.swagger.annotations.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
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
@Api(value = "tasks", description = "Operations for tasks")
public class TasksService {

    private static final Logger LOGGER = LogManager.getLogger();

    private final TaskDao taskDao;

    @Inject
    public TasksService(TaskDao taskDao) {
        this.taskDao = taskDao;
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all tasks", responseContainer = "List", response=Task.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Correct response"),
            @ApiResponse(code = 403, message = "Access forbidden")})
    public Response getAll(@Context SecurityContext sc,
                           @ApiParam(value = "Specify if you want to filter by finished") @QueryParam("finished") Boolean finished,
                           @QueryParam("closedDateAfter") DateTime closedDateAfter, @QueryParam("closedDateBefore") DateTime closedDateBefore) {
        Collection<Task> tasks;
        String ownerId = sc.getUserPrincipal().getName();
        if (finished == null) {
            tasks = taskDao.findAllByOwnerId(ownerId);
        } else if (!finished) {
            tasks = taskDao.findUnfinishedByOwnerId(ownerId);
        } else {
            tasks = taskDao.findFinishedByOwnerAndClosedBetween(ownerId, closedDateAfter, closedDateBefore);
        }
        return Response.ok(tasks).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create new task", notes = "Returns created task with unique id", response=Task.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Task created"),
            @ApiResponse(code = 400, message = "Incorrect input data"),
            @ApiResponse(code = 403, message = "Access forbidden")})
    public Response create(@Context SecurityContext sc, @Valid Task task) {
        LOGGER.debug("Create task request received");
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
    @Path("/{taskId}")
    @ApiOperation(value = "Delete task")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Task deleted"),
            @ApiResponse(code = 403, message = "Access forbidden"),
            @ApiResponse(code = 404, message = "Task with given id does not exist")})
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
    @Path("/{taskId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update task", notes = "Returns updated task", response = Task.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Task updated"),
            @ApiResponse(code = 400, message = "Incorrect input data"),
            @ApiResponse(code = 403, message = "Access forbidden"),
            @ApiResponse(code = 404, message = "Task with given id does not exist")})
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
    @Path("/{parentTaskId}/subtasks/{subtaskId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Move task to be subtask of parent task", notes = "Returns parent task", response = Task.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Task moved"),
            @ApiResponse(code = 400, message = "Incorrect request"),
            @ApiResponse(code = 403, message = "Access forbidden"),
            @ApiResponse(code = 404, message = "Parent task or subtask with given id does not exist"),
            @ApiResponse(code = 409, message = "Conflicting concurrent task or tags modification")})
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
    @Path("/{subtaskId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Move task to be top-level task", notes = "Returns moved task", response = Task.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Task moved"),
            @ApiResponse(code = 403, message = "Access forbidden"),
            @ApiResponse(code = 404, message = "Task with given id does not exist")})
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


