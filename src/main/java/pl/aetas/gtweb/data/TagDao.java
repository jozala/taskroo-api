package pl.aetas.gtweb.data;

import com.mongodb.*;
import pl.aetas.gtweb.domain.Tag;

import java.util.LinkedList;
import java.util.List;

public class TagDao {

    private static final String TAGS_COLLECTION_NAME = "tags";
    private final DBCollection tagsCollection;

    public TagDao(DBCollection dbTagsCollection) {
        tagsCollection = dbTagsCollection;
    }

    public List<Tag> getAllTagsByOwnerId(long ownerId) {
        DBCursor dbTags = tagsCollection.find(new BasicDBObject("id.owner_id", ownerId));
        List<Tag> tags = new LinkedList<>();
        for (DBObject tagDbObject : dbTags) {
            tags.add(mapDbObjectToTag(tagDbObject));
        }
        return tags;
    }

    private Tag mapDbObjectToTag(DBObject dbObject) {
        String name = dbObject.get("_id.name").toString();
        String owner = dbObject.get("_id.owner_id").toString();
        String color = dbObject.get("color").toString();
        boolean isVisibleInWorkView = Boolean.parseBoolean(dbObject.get("visibleInWorkView").toString());

        return new Tag.TagBuilder()
                .name(name)
                .color(color)
                .visibleInWorkView(isVisibleInWorkView)
                .build();
    }
}
