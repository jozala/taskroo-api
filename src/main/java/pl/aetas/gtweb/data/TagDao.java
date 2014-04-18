package pl.aetas.gtweb.data;

import com.mongodb.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;
import pl.aetas.gtweb.domain.Tag;

import javax.inject.Inject;
import java.util.List;

@Repository
public class TagDao {

    private static final Logger LOGGER = LogManager.getLogger();

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
        if (tag.getOwnerId() == null) {
            LOGGER.error("Trying to check if tag exists without ownerId set.");
            throw new UnsupportedDataOperationException("OwnerId not set in tag to check if exists. OwnerId has to be set.");
        }
        DBObject query = new BasicDBObject(NAME_KEY, tag.getName()).append(OWNER_ID_KEY, tag.getOwnerId());
        long count = tagsCollection.count(query);
        return count > 0;
    }

    public Tag insert(Tag tag) {
        if (tag.getOwnerId() == null) {
            LOGGER.error("Trying to insert tag without ownerId set.");
            throw new UnsupportedDataOperationException("OwnerId not set in tag to insert. OwnerId has to be set.");
        }
        DBObject dbTag = BasicDBObjectBuilder.start(OWNER_ID_KEY, tag.getOwnerId())
                .add(NAME_KEY, tag.getName())
                .add(COLOR_KEY, tag.getColor())
                .add(VISIBLE_IN_WORK_VIEW_KEY, tag.isVisibleInWorkView()).get();

        tagsCollection.insert(dbTag);
        return dbTagConverter.convertDbObjectToTag(dbTag);
    }
}
