package pl.aetas.gtweb.data;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import pl.aetas.gtweb.domain.Tag;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Singleton
public class TagDao {

    private final DBCollection tagsCollection;

    @Inject
    public TagDao(DBCollection dbTagsCollection) {
        tagsCollection = dbTagsCollection;
    }

    public List<Tag> getAllTagsByOwnerId(String ownerId) {
        DBCursor dbTags = tagsCollection.find(QueryBuilder.start("_id.owner_id").is(ownerId).get());
        List<Tag> tags = new LinkedList<>();
        for (DBObject tagDbObject : dbTags) {
            tags.add(mapDbObjectToTag(tagDbObject));
        }
        return tags;
    }

    private Tag mapDbObjectToTag(DBObject dbObject) {
        String name = ((Map)dbObject.get("_id")).get("name").toString();
        String ownerId = ((Map)dbObject.get("_id")).get("owner_id").toString();
        String color = dbObject.get("color").toString();
        boolean isVisibleInWorkView = Boolean.parseBoolean(dbObject.get("visibleInWorkView").toString());

        return new Tag.TagBuilder()
                .name(name)
                .ownerId(ownerId)
                .color(color)
                .visibleInWorkView(isVisibleInWorkView)
                .build();
    }
}
