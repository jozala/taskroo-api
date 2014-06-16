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
        if ((existingTag = tagDao.findByName(tag.getOwnerId(), tag.getName())) != null) {
            return Response.ok(existingTag).build();
        }
        Tag savedTag = tagDao.insert(tag);
        return Response.created(URI.create("tasks/" + savedTag.getId())).entity(savedTag).build();
    }

    @DELETE
    @Path("{tagId}")
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
    @Path("{tagId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
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
