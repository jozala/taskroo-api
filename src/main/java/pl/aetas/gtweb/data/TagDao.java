package pl.aetas.gtweb.data;

import com.mongodb.*;
import org.springframework.stereotype.Repository;
import pl.aetas.gtweb.domain.Tag;

import javax.inject.Inject;
import java.util.List;

@Repository
public class TagDao {

    public static final String ID_KEY = "_id";
    public static final String NAME_KEY = "name";
    public static final String OWNER_ID_KEY = "owner_id";
    public static final String COLOR_KEY = "color";
    public static final String VISIBLE_IN_WORK_VIEW_KEY = "visible_in_workview";

    private final DBCollection tagsCollection;
    private final DbTagConverter dbTagConverter;

    @Inject
    public TagDao(DBCollection tagsCollection, DbTagConverter dbTagConverter) {
        this.tagsCollection = tagsCollection;
        this.dbTagConverter = dbTagConverter;
    }

    public List<Tag> getAllTagsByOwnerId(String ownerId) {
        DBCursor dbTags = tagsCollection.find(QueryBuilder.start(OWNER_ID_KEY).is(ownerId).get());
        return dbTagConverter.convertDbObjectsToSetOfTags(dbTags.toArray());
    }

    public boolean exists(Tag tag) {
        DBObject query = new BasicDBObject(NAME_KEY, tag.getName()).append(OWNER_ID_KEY, tag.getOwnerId());
        long count = tagsCollection.count(query);
        return count > 0;
    }

}
