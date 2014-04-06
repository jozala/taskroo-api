package pl.aetas.gtweb.service;

import org.springframework.stereotype.Component;
import pl.aetas.gtweb.data.TagDao;
import pl.aetas.gtweb.domain.Tag;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
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
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Tag> retrieveTasks(@Context SecurityContext sc) {
        String userId = sc.getUserPrincipal().getName();
        return tagDao.getAllTagsByOwnerId(userId);

    }

}
