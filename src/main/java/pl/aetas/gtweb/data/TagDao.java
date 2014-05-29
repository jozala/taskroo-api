package pl.aetas.gtweb.data;

import com.mongodb.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;
import pl.aetas.gtweb.domain.Tag;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
    private final DBCollection tasksCollection;

    @Inject
    public TagDao(DBCollection tagsCollection, DbTagConverter dbTagConverter, DBCollection tasksCollection) {
        this.tagsCollection = tagsCollection;
        this.dbTagConverter = dbTagConverter;
        this.tasksCollection = tasksCollection;
    }

    public List<Tag> getAllTagsByOwnerId(String ownerId) {
        DBCursor dbTags = tagsCollection.find(QueryBuilder.start(OWNER_ID_KEY).is(ownerId).get());
        return dbTagConverter.convertDbObjectsToSetOfTags(dbTags.toArray());
    }

    public boolean exists(String ownerId, String name) {
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(name);
        DBObject query = new BasicDBObject(NAME_KEY, name).append(OWNER_ID_KEY, ownerId);
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

    public Tag findOne(String ownerId, String name) {
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(name);
        DBObject tag = tagsCollection.findOne(new BasicDBObject("owner_id", ownerId).append("name", name));
        if (tag == null) {
            return null;
        }
        return dbTagConverter.convertDbObjectToTag(tag);
    }

    public void remove(String ownerId, String name) throws NonExistingResourceOperationException{
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(name);

        if (!exists(ownerId, name)) {
            LOGGER.error("Trying to remove non-existing tag with name {} of owner {}", name, ownerId);
            throw new NonExistingResourceOperationException("Trying to remove non-existing tag");
        }

        BasicDBObject queryTagByOwnerAndName = new BasicDBObject(TagDao.OWNER_ID_KEY, ownerId).append(TagDao.NAME_KEY, name);
        String tagId = tagsCollection.findOne(queryTagByOwnerAndName, new BasicDBObject("_id", true)).get("_id").toString();
        tagsCollection.remove(new BasicDBObject("_id", new ObjectId(tagId)));
        removeTagFromAllTasksOfThisUser(ownerId, tagId);
    }

    private void removeTagFromAllTasksOfThisUser(String ownerId, String tagId) {
        DBObject queryTasksByOwnerWithTag = QueryBuilder.start(TaskDao.OWNER_ID_KEY).is(ownerId).and(TaskDao.TAGS_KEY).is(tagId).get();
        tasksCollection.update(queryTasksByOwnerWithTag, new BasicDBObject("$pull", new BasicDBObject(TaskDao.TAGS_KEY, tagId)), false, true);
    }

    public Tag update(String ownerId, String currentTagName, Tag tagToUpdate) throws NonExistingResourceOperationException{
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(currentTagName);
        Objects.requireNonNull(tagToUpdate);

        if (!ownerId.equals(tagToUpdate.getOwnerId())) {
            LOGGER.warn("Trying to update tag with different userId ({}) then found in session ({})", tagToUpdate.getOwnerId(), ownerId);
            throw new IllegalArgumentException("OwnerId given is different than ownerId in the tag object");
        }

        BasicDBObject queryTagByOwnerAndName = new BasicDBObject(TagDao.OWNER_ID_KEY, ownerId).append(TagDao.NAME_KEY, currentTagName);
        DBObject dbTagToUpdate = BasicDBObjectBuilder.start(OWNER_ID_KEY, ownerId)
                .add(NAME_KEY, tagToUpdate.getName())
                .add(COLOR_KEY, tagToUpdate.getColor())
                .add(VISIBLE_IN_WORK_VIEW_KEY, tagToUpdate.isVisibleInWorkView()).get();
        DBObject dbTagAfterUpdate = tagsCollection.findAndModify(queryTagByOwnerAndName, null, null, false, dbTagToUpdate, true, false);
        if (dbTagAfterUpdate == null) {
            throw new NonExistingResourceOperationException("Tag: " + currentTagName + " for user: " + ownerId +
                    " cannot be updated, because it has not been found");
        }
        return dbTagConverter.convertDbObjectToTag(dbTagAfterUpdate);
    }

    Set<String> findNonExistingTags(Set<String> tagsIds) {
        Set<ObjectId> tagsIdsObjectIds = new HashSet<>();
        for (String tagId : tagsIds) {
            tagsIdsObjectIds.add(new ObjectId(tagId));
        }
        DBCursor existingTags = tagsCollection.find(new BasicDBObject("_id", new BasicDBObject("$in", tagsIdsObjectIds)), new BasicDBObject("_id", true));
        Set<String> existingTagsIds = new HashSet<>();
        for (DBObject existingTag : existingTags) {
            existingTagsIds.add(existingTag.get("_id").toString());
        }
        Set<String> nonExistingTagsIds = new HashSet<>();
        for (String tagId : tagsIds) {
            if (!existingTagsIds.contains(tagId)) {
                nonExistingTagsIds.add(tagId);
            }
        }
        return nonExistingTagsIds;
    }
}
