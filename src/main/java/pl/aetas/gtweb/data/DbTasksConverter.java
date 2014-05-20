package pl.aetas.gtweb.data;

import com.mongodb.DBObject;
import org.springframework.stereotype.Component;
import pl.aetas.gtweb.domain.Tag;
import pl.aetas.gtweb.domain.Task;

import java.util.*;

import static pl.aetas.gtweb.data.TaskDao.*;

@Component
public class DbTasksConverter {

    public Collection<Task> convertToTasksTree(List<DBObject> dbTasksObjects, List<Tag> allUserTags, boolean buildingPartOfATree) {
        Map<String, Tag> tagsMap = convertTagsToTagsMap(allUserTags);
        Map<String, Task> tasksMap = new LinkedHashMap<>();
        for (DBObject dbTask : dbTasksObjects) {
            if (isTopLevelTask(dbTask, tasksMap, buildingPartOfATree)) {
                tasksMap.put(dbTask.get("_id").toString(), convertSingleDbObjectToTask(dbTask, tagsMap));
            } else {
                List<String> pathList = Arrays.asList(dbTask.get(PATH_KEY).toString().split(","));
                Task taskToAdd = convertSingleDbObjectToTask(dbTask, tagsMap);
                addSubtask(tasksMap.get(pathList.get(0)), taskToAdd, pathList.subList(1, pathList.size()));
            }
        }
        return tasksMap.values();
    }

    public Collection<Task> convertToTasksTree(List<DBObject> dbTasksObjects, List<Tag> allUserTags) {
        return convertToTasksTree(dbTasksObjects, allUserTags, false);
    }

    private boolean isTopLevelTask(DBObject dbTask, Map<String, Task> alreadyReadTasks, boolean buildingPartOfATree) {
        return dbTask.get(PATH_KEY) == null ||
                (buildingPartOfATree && !alreadyReadTasks.containsKey(dbTask.get(PATH_KEY)));
    }

    private void addSubtask(Task ancestorTask, Task taskToAdd, List<String> pathList) {
        assert ancestorTask != null : "AncestorTask cannot be null. " +
                "Did you forget about sorting tasks by path or trying to build part of a tree?";
        if (pathList.isEmpty()) {
            taskToAdd.setParentTask(ancestorTask);
            ancestorTask.addSubtask(taskToAdd);
            return;
        }
        String subtaskId = pathList.get(0);
        for (Task subtask : ancestorTask.getSubtasks()) {
            if (subtask.getId().equals(subtaskId)) {
                addSubtask(subtask, taskToAdd, pathList.subList(1, pathList.size()));
                return;
            }
        }
    }

    public Task convertSingleDbObjectToTask(DBObject dbTask, Map<String, Tag> tagsMap) {
        String ownerId = dbTask.get(OWNER_ID_KEY).toString();
        Task.TaskBuilder builder = new Task.TaskBuilder()
                .setOwnerId(ownerId)
                .setTitle(dbTask.get(TITLE_KEY).toString())
                .setId(dbTask.get("_id").toString())
                .setDescription((String) dbTask.get(DESCRIPTION_KEY))
                .setDueDate((Date) dbTask.get(DUE_DATE_KEY))
                .setStartDate((Date) dbTask.get(START_DATE_KEY))
                .setCreatedDate((Date) dbTask.get(CREATED_DATE_KEY))
                .setClosedDate((Date) dbTask.get(CLOSED_DATE_KEY))
                .setFinished((boolean) dbTask.get(FINISHED_KEY));

        if (dbTask.get(TAGS_KEY) != null) {
            for (String tagId : (List<String>) dbTask.get(TAGS_KEY)) {
                builder.addTag(tagsMap.get(tagId));
            }
        }

        return builder.build();
    }


    private Map<String, Tag> convertTagsToTagsMap(List<Tag> tags) {
        Map<String, Tag> tagsMap = new HashMap<>();
        for (Tag tag : tags) {
            tagsMap.put(tag.getId(), tag);
        }
        return tagsMap;
    }



}
