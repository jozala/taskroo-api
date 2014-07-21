package com.taskroo.service;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.springframework.stereotype.Component;
import com.taskroo.data.NonExistingResourceOperationException;
import com.taskroo.data.TagDao;
import com.taskroo.domain.Tag;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.List;

@Component
@RolesAllowed("user")
@Path("tags")
@Api(value = "tags", description = "Operations for tags")
public class TagsService {

    private final TagDao tagDao;

    @Inject
    public TagsService(TagDao tagDao) {
        this.tagDao = tagDao;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all tags", responseContainer = "List", response = Tag.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Correct response"),
            @ApiResponse(code = 403, message = "Access forbidden")})
    public List<Tag> getAll(@Context SecurityContext sc) {
        String userId = sc.getUserPrincipal().getName();
        return tagDao.getAllTagsByOwnerId(userId);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create new tag", notes = "Returns created tag", response = Tag.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Tag with given name already exists"),
            @ApiResponse(code = 201, message = "Tag created"),
            @ApiResponse(code = 403, message = "Access forbidden")})
    public Response create(@Context SecurityContext securityContext, Tag tag) {
        tag.setOwnerId(securityContext.getUserPrincipal().getName());
        Tag existingTag;
        if ((existingTag = tagDao.findByName(tag.getOwnerId(), tag.getName())) != null) {
            return Response.ok(existingTag).build();
        }
        Tag savedTag = tagDao.insert(tag);
        return Response.created(URI.create("tasks/" + savedTag.getId())).entity(savedTag).build();
    }

    @DELETE
    @Path("/{tagId}")
    @ApiOperation(value = "Remove tag")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Tag removed"),
            @ApiResponse(code = 404, message = "Tag not found"),
            @ApiResponse(code = 403, message = "Access forbidden")})
    public Response delete(@Context SecurityContext securityContext, @PathParam("tagId") String tagId) {
        String ownerId = securityContext.getUserPrincipal().getName();
        try {
            tagDao.remove(ownerId, tagId);
        } catch (NonExistingResourceOperationException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @PUT
    @Path("/{tagId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update existing tag", notes = "Returns updated tag", response = Tag.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Tag updated"),
            @ApiResponse(code = 400, message = "Invalid input data"),
            @ApiResponse(code = 404, message = "Tag with given id not found"),
            @ApiResponse(code = 403, message = "Access forbidden")})
    public Response update(@Context SecurityContext securityContext, @PathParam("tagId") String tagId, Tag tag) {
        if (tag == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        String ownerId = securityContext.getUserPrincipal().getName();
        tag.setOwnerId(ownerId);
        Tag existingTagWithSameName = tagDao.findByName(tag.getOwnerId(), tag.getName());
        if (existingTagWithSameName != null && !existingTagWithSameName.getId().equals(tagId)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Tag with name: " + tag.getName() + " already exists.").build();
        }
        try {
            Tag tagAfterUpdate = tagDao.update(ownerId, tagId, tag);
            return Response.ok(tagAfterUpdate).build();
        } catch (NonExistingResourceOperationException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
