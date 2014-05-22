package pl.aetas.gtweb.service;

import org.springframework.stereotype.Component;
import pl.aetas.gtweb.data.NonExistingResourceOperationException;
import pl.aetas.gtweb.data.TagDao;
import pl.aetas.gtweb.domain.Tag;

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
public class TagsService {

    private final TagDao tagDao;

    @Inject
    public TagsService(TagDao tagDao) {
        this.tagDao = tagDao;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Tag> getAll(@Context SecurityContext sc) {
        String userId = sc.getUserPrincipal().getName();
        return tagDao.getAllTagsByOwnerId(userId);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext securityContext, Tag tag) {
        tag.setOwnerId(securityContext.getUserPrincipal().getName());
        Tag existingTag;
        if ((existingTag = tagDao.findOne(tag.getOwnerId(), tag.getName())) != null) {
            return Response.ok(existingTag).build();
        }
        Tag savedTag = tagDao.insert(tag);
        return Response.created(URI.create("tasks/" + savedTag.getId())).entity(savedTag).build();
    }

    @DELETE
    @Path("{tagName}")
    public Response delete(@Context SecurityContext securityContext, @PathParam("tagName") String tagName) {
        String ownerId = securityContext.getUserPrincipal().getName();
        try {
            tagDao.remove(ownerId, tagName);
        } catch (NonExistingResourceOperationException e) {
            // TODO should be better 404 ?
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.noContent().build();
    }

    @PUT
    @Path("{tagName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@Context SecurityContext securityContext, @PathParam("tagName") String tagName, Tag tag) {
        String ownerId = securityContext.getUserPrincipal().getName();
        tag.setOwnerId(ownerId);
        try {
            Tag tagAfterUpdate = tagDao.update(ownerId, tagName, tag);
            return Response.ok(tagAfterUpdate).build();
        } catch (NonExistingResourceOperationException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
