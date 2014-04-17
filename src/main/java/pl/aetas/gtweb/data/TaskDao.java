package pl.aetas.gtweb.data;

import com.mongodb.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;
import pl.aetas.gtweb.domain.Tag;
import pl.aetas.gtweb.domain.Task;

import javax.inject.Inject;
import java.util.*;

@Repository
public class TaskDao {

    private static Logger LOGGER = LogManager.getLogger();

    public static final String DESCRIPTION_KEY = "description";
    public static final String DUE_DATE_KEY = "due_date";
    public static final String START_DATE_KEY = "start_date";
    public static final String CREATED_DATE_KEY = "created_date";
    public static final String CLOSED_DATE_KEY = "closed_date";
    public static final String FINISHED_KEY = "finished";
    public static final String TAGS_KEY = "tags";
    public static final String PATH_KEY = "path";
    public static final String OWNER_ID_KEY = "owner_id";
    public static final String TITLE_KEY = "title";

    private final DBCollection tasksCollection;
    private final TagDao tagDao;
    private final DbTasksConverter dbTasksConverter;

    @Inject
    public TaskDao(DBCollection tasksCollection, TagDao tagDao, DbTasksConverter dbTasksConverter) {
        this.tasksCollection = tasksCollection;
        this.tagDao = tagDao;
        this.dbTasksConverter = dbTasksConverter;
    }

    public Task insert(Task task) {
        if (!task.getSubtasks().isEmpty()) {
            LOGGER.info("POSSIBLE CLIENT MALFUNCTION: Trying to insert task with subtasks.");
            throw new UnsupportedDataOperationException("Cannot insert task with subtasks. This is not supported.");
        }

        if (task.getOwnerId() == null) {
            LOGGER.error("Trying to insert task without ownerId set");
            throw new UnsupportedDataOperationException("Trying to insert task without ownerId set");
        }

        if (task.getCreatedDate() == null) {
            task.setCreatedDate(new Date());
        }

        List<Tag> allUserTags = tagDao.getAllTagsByOwnerId(task.getOwnerId());

        DBObject taskDbObject = BasicDBObjectBuilder.start(TITLE_KEY, task.getTitle())
                .append(DESCRIPTION_KEY, task.getDescription())
                .append(DUE_DATE_KEY, task.getDueDate())
                .append(START_DATE_KEY, task.getStartDate())
                .append(CREATED_DATE_KEY, task.getCreatedDate())
                .append(CLOSED_DATE_KEY, task.getClosedDate())
                .append(FINISHED_KEY, task.isFinished())
                .append(OWNER_ID_KEY, task.getOwnerId())
                .append(TAGS_KEY, getTagsIds(task.getTags(), allUserTags))
                .append(PATH_KEY, getPath(task.getParentTask()))
                .get();

        tasksCollection.insert(taskDbObject);
        String taskId = taskDbObject.get("_id").toString();
        task.setId(taskId);

        return task;
    }

    private Set<String> getTagsIds(Set<Tag> taskTags, List<Tag> allUserTags) {
        Set<String> tagsIds = new HashSet<>();
        for (Tag taskTag : taskTags) {
            String tagId = findTagOnListByName(allUserTags, taskTag);
            if (tagId == null) {
                LOGGER.info("POSSIBLE CLIENT MALFUNCTION: Trying to add task with non-existing tag.");
                throw new UnsupportedDataOperationException("Cannot add tag "+ taskTag.getName() +
                        " which does not exists for this user: " + taskTag.getOwnerId());
            }
            tagsIds.add(tagId);
        }
        return tagsIds;
    }

    private String findTagOnListByName(List<Tag> allUserTags, Tag taskTag) {
        String tagId = null;
        for (Tag userTag : allUserTags) {
            if (userTag.getName().equals(taskTag.getName())) {
                tagId = userTag.getId();
                break;
            }
        }
        return tagId;
    }

    private String getPath(Task parentTask) {
        if (parentTask == null) {
            return null;
        }
        DBObject dbPath = tasksCollection.findOne(new BasicDBObject("_id", new ObjectId(parentTask.getId()))
                        .append(OWNER_ID_KEY, parentTask.getOwnerId()), new BasicDBObject(PATH_KEY, true));

        Object parentPath = dbPath.get(PATH_KEY);
        return (parentPath != null ? parentPath.toString() + "," : "") + dbPath.get("_id").toString();
    }

    public Collection<Task> findAllByOwnerId(String ownerId) {
        DBObject queryByOwner = QueryBuilder.start(OWNER_ID_KEY).is(ownerId).get();
        DBCursor dbTasks = tasksCollection.find(queryByOwner).sort(new BasicDBObject(PATH_KEY, 1));
        List<Tag> allUserTags = tagDao.getAllTagsByOwnerId(ownerId);
        return dbTasksConverter.convertToTasksTree(dbTasks.toArray(), allUserTags);
    }

}
