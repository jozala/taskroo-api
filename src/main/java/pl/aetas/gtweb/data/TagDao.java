package pl.aetas.gtweb.data;

import com.mongodb.*;
import org.springframework.stereotype.Repository;
import pl.aetas.gtweb.domain.Tag;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Repository
public class TagDao {

    public static final String NAME_KEY = "name";
    public static final String OWNER_ID_KEY = "owner_id";
    public static final String COLOR_KEY = "color";
    public static final String VISIBLE_IN_WORK_VIEW_KEY = "visibleInWorkView";

    private final DBCollection tagsCollection;

    @Inject
    public TagDao(DBCollection tagsCollection) {
        this.tagsCollection = tagsCollection;
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
        String name = ((Map)dbObject.get("_id")).get(NAME_KEY).toString();
        String ownerId = ((Map)dbObject.get("_id")).get(OWNER_ID_KEY).toString();
        String color = dbObject.get(COLOR_KEY).toString();
        boolean isVisibleInWorkView = Boolean.parseBoolean(dbObject.get(VISIBLE_IN_WORK_VIEW_KEY).toString());

        return new Tag.TagBuilder()
                .name(name)
                .ownerId(ownerId)
                .color(color)
                .visibleInWorkView(isVisibleInWorkView)
                .build();
    }

    public boolean exists(Tag tag) {
        DBObject query = QueryBuilder.start("_id").is(createKey(tag.getName(), tag.getOwnerId())).get();
        long count = tagsCollection.count(query);
        return count > 0;
    }

    private DBObject createKey(String name, String ownerId) {
        return new BasicDBObject(NAME_KEY, name).append(OWNER_ID_KEY, ownerId);
    }
}
