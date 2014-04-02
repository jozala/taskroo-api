package pl.aetas.gtweb.data;

import com.mongodb.*;
import pl.aetas.gtweb.domain.Tag;
import pl.aetas.gtweb.domain.User;

import java.util.List;

public class TagDao {

    private static final String TAGS_COLLECTION_NAME = "tags";
    private final DBCollection tagsCollection;

    public TagDao(DB db) {
        tagsCollection = db.getCollection(TAGS_COLLECTION_NAME);
    }

    public List<Tag> getAllTagsByOwnerId(long ownerId) {
        // TODO find on the web if this ownerId should be done like relational db or user should have whole hierarchy
        DBCursor tags = tagsCollection.find(new BasicDBObject("owner_id", ownerId));

    }

    private Tag mapDbObjectToTag(DBObject dbObject) {
        String id = dbObject.get("_id").toString();
        String name = dbObject.get("name").toString();
        String color = dbObject.get("color").toString();
        boolean isVisibleInWorkView = Boolean.parseBoolean(dbObject.get("visibleInWorkView").toString());
        String owner = dbObject.get("owner").toString();

        return new Tag(new User(), name, color, isVisibleInWorkView);
    }
}
