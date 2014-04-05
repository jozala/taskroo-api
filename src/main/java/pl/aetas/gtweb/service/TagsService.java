package pl.aetas.gtweb.service;

import pl.aetas.gtweb.data.MongoConnector;
import pl.aetas.gtweb.data.TagDao;
import pl.aetas.gtweb.domain.Tag;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.net.UnknownHostException;
import java.util.List;

@Path("tags")
public class TagsService {

    private final TagDao tagDao;

    public TagsService() throws UnknownHostException {
        this.tagDao = new TagDao(new MongoConnector().getDatabase("gtweb").getCollection("tags"));
    }

    @GET
    @Path("all/{ownerId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Tag> retrieveTasks(@PathParam("ownerId") String ownerId) {
        return tagDao.getAllTagsByOwnerId(ownerId);

    }

}
