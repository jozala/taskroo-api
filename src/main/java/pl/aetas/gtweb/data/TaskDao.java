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

    public static final String ID_KEY = "_id";
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
            LOGGER.warn("POSSIBLE CLIENT MALFUNCTION: Trying to insert task with subtasks.");
            throw new UnsupportedDataOperationException("Cannot insert task with subtasks. This is not supported.");
        }

        if (task.getOwnerId() == null) {
            LOGGER.error("Trying to insert task without ownerId set");
            throw new UnsupportedDataOperationException("Trying to insert task without ownerId set");
        }
        if (task.getParentTask() != null) {
            LOGGER.error("Trying to insert task with subtask what is unsupported.");
            throw new UnsupportedDataOperationException("Cannot insert task with parent task set. Insert top level task and move it to subtask instead.");
        }

        if (task.getCreatedDate() == null) {
            task.setCreatedDate(new Date());
        }

        List<Tag> allUserTags = tagDao.getAllTagsByOwnerId(task.getOwnerId());

        Set<String> tagsIdsForTask = getTagsIds(task.getTags(), allUserTags);
        DBObject taskDbObject = BasicDBObjectBuilder.start(TITLE_KEY, task.getTitle())
                .append(DESCRIPTION_KEY, task.getDescription())
                .append(DUE_DATE_KEY, task.getDueDate())
                .append(START_DATE_KEY, task.getStartDate())
                .append(CREATED_DATE_KEY, task.getCreatedDate())
                .append(CLOSED_DATE_KEY, task.getClosedDate())
                .append(FINISHED_KEY, task.isFinished())
                .append(OWNER_ID_KEY, task.getOwnerId())
                .append(TAGS_KEY, tagsIdsForTask)
                .append(PATH_KEY, Collections.emptyList())
                .get();

        tasksCollection.insert(taskDbObject);

        taskDbObject = updateTagsIfConcurrentTagsModificationHappen(tagsIdsForTask, taskDbObject);
        String taskId = taskDbObject.get(ID_KEY).toString();
        task.setId(taskId);

        return dbTasksConverter.convertToTasksTree(Collections.singletonList(taskDbObject), allUserTags, true).iterator().next();
    }

    private Set<String> getTagsIds(Set<Tag> taskTags, List<Tag> allUserTags) {
        Set<String> tagsIds = new HashSet<>();
        for (Tag taskTag : taskTags) {
            String tagId = findTagOnListByName(allUserTags, taskTag);
            if (tagId == null) {
                LOGGER.warn("POSSIBLE CLIENT MALFUNCTION: Trying to add task with non-existing tag.");
                throw new UnsupportedDataOperationException("Cannot add tag '"+ taskTag.getName() +
                        "' which does not exists for this user: " + taskTag.getOwnerId());
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

    private List<String> getPath(String ownerId, String taskId, boolean includingTaskId) throws NonExistingResourceOperationException {
        if (taskId == null) {
            return Collections.emptyList();
        }

        DBObject dbPath = tasksCollection.findOne(new BasicDBObject(ID_KEY, new ObjectId(taskId)).append(OWNER_ID_KEY, ownerId),
                new BasicDBObject(PATH_KEY, true));

        if (dbPath == null) {
            LOGGER.warn("Cannot find path for task with id {} of user {} because task does not exists in database", taskId, ownerId);
            throw new NonExistingResourceOperationException("Cannot find path for task with id " + taskId + " of user " + ownerId + " because task does not exist in database");
        }

        List<String> parentPath = (List<String>)dbPath.get(PATH_KEY);
        if (includingTaskId) {
            parentPath.add(dbPath.get(ID_KEY).toString());
        }
        return parentPath;
    }

    public Collection<Task> findAllByOwnerId(String ownerId) {
        DBObject queryByOwner = QueryBuilder.start(OWNER_ID_KEY).is(ownerId).get();
        DBCursor dbTasks = tasksCollection.find(queryByOwner);
        List<Tag> allUserTags = tagDao.getAllTagsByOwnerId(ownerId);
        return dbTasksConverter.convertToTasksTree(dbTasks.toArray(), allUserTags);
    }

    public void remove(String ownerId, String taskId) throws NonExistingResourceOperationException {
        if (!ObjectId.isValid(taskId)) {
            LOGGER.warn("Task id is invalid: {} (ownerId: {}). Nothing has been removed.", taskId, ownerId);
            throw new NonExistingResourceOperationException("Invalid task id: " + taskId);
        }
        WriteResult result = tasksCollection.remove(findTaskWithSubtasksQuery(ownerId, taskId));
        if (result.getN() == 0) {
            LOGGER.info("Task with id {} and ownerId {} not found in DB. Nothing has been removed.", taskId, ownerId);
            throw new NonExistingResourceOperationException("Task with id " + taskId + "and ownerId " + ownerId + " not found in DB");
        }
    }

    public Task update(String ownerId, Task task) throws NonExistingResourceOperationException {
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(task);

        DBObject findByIdAndOwnerIdQuery = QueryBuilder.start(ID_KEY).is(new ObjectId(task.getId()))
                .and(TaskDao.OWNER_ID_KEY).is(ownerId).get();

        List<Tag> allUserTags = tagDao.getAllTagsByOwnerId(ownerId);
        Set<String> tagsIdsForTask = getTagsIds(task.getTags(), allUserTags);
        DBObject taskDbObject = BasicDBObjectBuilder.start(TITLE_KEY, task.getTitle())
                .append(DESCRIPTION_KEY, task.getDescription())
                .append(DUE_DATE_KEY, task.getDueDate())
                .append(START_DATE_KEY, task.getStartDate())
                .append(CLOSED_DATE_KEY, task.getClosedDate())
                .append(FINISHED_KEY, task.isFinished())
                .append(TAGS_KEY, tagsIdsForTask)
                .get();

        DBObject dbTaskAfterUpdate = tasksCollection.findAndModify(findByIdAndOwnerIdQuery, null, null, false,
                new BasicDBObject("$set", taskDbObject), true, false);

        if (dbTaskAfterUpdate == null) {
            LOGGER.info("Task with id {} and ownerId {} not found in DB. Nothing has been updated.", task.getId(), ownerId);
            throw new NonExistingResourceOperationException("Task with id " + task.getId() + "and ownerId " + ownerId + " not found in DB");
        }

        dbTaskAfterUpdate = updateTagsIfConcurrentTagsModificationHappen(tagsIdsForTask, dbTaskAfterUpdate);

        Map<String, Tag> tagsMap = new HashMap<>();
        for (Tag tag : allUserTags) {
            tagsMap.put(tag.getId(), tag);
        }
        return dbTasksConverter.convertSingleDbObjectToTask(dbTaskAfterUpdate, tagsMap);
    }

    private DBObject updateTagsIfConcurrentTagsModificationHappen(Set<String> tagsIdsForTask,
                                                                  DBObject dbTaskAfterUpdate) {
        BasicDBObject findByIdQuery = new BasicDBObject(ID_KEY, dbTaskAfterUpdate.get(ID_KEY));
        Set<String> tagsRemovedInTheMeanTime = tagDao.findNonExistingTags(tagsIdsForTask);
        if (!tagsRemovedInTheMeanTime.isEmpty()) {
            LOGGER.info("Some tags have been removed in the time of processing task change/create (id={}). " +
                    "These tags will be removed from task: {}", dbTaskAfterUpdate.get(ID_KEY), tagsRemovedInTheMeanTime);
            tagsIdsForTask.removeAll(tagsRemovedInTheMeanTime);
            return tasksCollection.findAndModify(findByIdQuery, null, null, false,
                    new BasicDBObject("$set", new BasicDBObject(TAGS_KEY, tagsIdsForTask)), true, false);
        }
        return dbTaskAfterUpdate;
    }

    public Task addSubtask(String ownerId, String parentId, String subtaskId) throws NonExistingResourceOperationException, ConcurrentTasksModificationException {
        if (taskDoesNotExistInDb(ownerId, parentId)) {
            LOGGER.warn("Parent task with id: {} does not exists for customer with id {}", parentId, ownerId);
            throw new NonExistingResourceOperationException("Parent task with id: " + parentId + " does not exists for customer with id: " + ownerId);
        }

        if (parentId.equals(subtaskId)) {
            LOGGER.warn("POSSIBLE CLIENT MALFUNCTION: Cannot add task (id: {}) as subtask to itself", subtaskId);
            throw new UnsupportedDataOperationException("Cannot add task as subtask to itself");
        }

        List<String> parentTaskPath = getPath(ownerId, parentId, true);
        if (parentTaskPath.contains(subtaskId)) {
            LOGGER.warn("POSSIBLE CLIENT MALFUNCTION: Cannot add task (id: {}) as subtask to one of its subtasks (id: {})", subtaskId, parentId);
            throw new UnsupportedDataOperationException("Cannot add task as subtask to one of its subtasks");
        }

        moveTaskWithSubtasksToTopLevel(ownerId, subtaskId);
        moveTaskWithSubtasksToNewPath(ownerId, subtaskId, parentTaskPath);

        if (!allTasksExists(parentTaskPath)) {
            LOGGER.warn("Concurrent task hierarchy modification. Task with id {} and all its subtasks will be removed, because new parent task has been removed.", subtaskId);
            tasksCollection.remove(findTaskWithSubtasksQuery(ownerId, subtaskId));
            throw new ConcurrentTasksModificationException("Ascendants tasks have been removed when processing this request. " +
                    "Tasks have been removed to keep data consistent.");
        }

        if (!parentTaskPath.equals(getPath(ownerId, parentId, true))) {
            LOGGER.warn("Concurrent task hierarchy modification. Task with id {} will be moved to top level, because new ancestors hierarchy has changed.", subtaskId);
            moveTaskWithSubtasksToTopLevel(ownerId, subtaskId);
            throw new ConcurrentTasksModificationException("Ascendants tasks have been shuffled when processing this request. " +
                    "Task has been move to top-level to keep data consistent.");
        }


        return getTask(ownerId, parentId);
    }

    private DBObject findTaskWithSubtasksQuery(String ownerId, String taskId) {
        DBObject findSubtaskByIdAndOwnerIdQuery = QueryBuilder.start(ID_KEY).is(new ObjectId(taskId)).and(TaskDao.OWNER_ID_KEY).is(ownerId).get();
        DBObject findSubtasksOfSubtaskQuery = QueryBuilder.start(PATH_KEY).is(taskId).get();
        return QueryBuilder.start().or(findSubtaskByIdAndOwnerIdQuery, findSubtasksOfSubtaskQuery).get();
    }

    private boolean allTasksExists(Collection<String> tasksIds) {
        List<ObjectId> objectIds = new ArrayList<>(tasksIds.size());
        for (String ancestorId : tasksIds) {
            objectIds.add(new ObjectId(ancestorId));
        }
        DBObject findTasksByIdQuery = QueryBuilder.start(ID_KEY).in(objectIds).get();
        return (tasksCollection.count(findTasksByIdQuery) == tasksIds.size());
    }

    private void moveTaskWithSubtasksToNewPath(String ownerId, String taskId, List<String> newParentPath) {
        BasicDBObject prependNewParentPath = new BasicDBObject("$push", new BasicDBObject(PATH_KEY, new BasicDBObject("$each", newParentPath).append("$position", 0)));
        tasksCollection.update(findTaskWithSubtasksQuery(ownerId, taskId), prependNewParentPath, false, true);
    }

    private void moveTaskWithSubtasksToTopLevel(String ownerId, String taskId) throws NonExistingResourceOperationException {
        DBObject findSubtaskWithItsSubtasksQuery = findTaskWithSubtasksQuery(ownerId, taskId);
        List<String> previousTaskPath = getPath(ownerId, taskId, false);
        BasicDBObject removeHigherLevelTasksFromPath = new BasicDBObject("$pullAll", new BasicDBObject(PATH_KEY, previousTaskPath));
        tasksCollection.update(findSubtaskWithItsSubtasksQuery, removeHigherLevelTasksFromPath, false, true);
    }

    private boolean taskDoesNotExistInDb(String ownerId, String taskId) {
        DBObject findTaskByIdAndOwnerIdQuery = QueryBuilder.start(ID_KEY).is(new ObjectId(taskId))
                .and(TaskDao.OWNER_ID_KEY).is(ownerId).get();
        return tasksCollection.count(findTaskByIdAndOwnerIdQuery) == 0;
    }

    private Task getTask(String ownerId, String taskId) throws NonExistingResourceOperationException {
        DBObject findTaskWithSubtasks = QueryBuilder.start().or(new BasicDBObject(ID_KEY, new ObjectId(taskId)),
                new BasicDBObject(PATH_KEY, taskId)).get();
        DBCursor dbTasks = tasksCollection.find(findTaskWithSubtasks);
        if (dbTasks.count() == 0) {
            throw new NonExistingResourceOperationException("Task with id: " + taskId + " does not exists for customer with id: " + ownerId);
        }
        Collection<Task> tasks = dbTasksConverter.convertToTasksTree(dbTasks.toArray(), tagDao.getAllTagsByOwnerId(ownerId), true);

        return tasks.iterator().next();
    }
}
