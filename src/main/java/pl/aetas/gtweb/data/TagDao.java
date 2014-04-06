package pl.aetas.gtweb.data;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.springframework.stereotype.Repository;
import pl.aetas.gtweb.domain.Tag;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Repository
public class TagDao {

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
