package com.taskroo.data;

import com.mongodb.DBObject;
import com.taskroo.domain.Tag;
import com.taskroo.domain.Task;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.taskroo.data.TaskDao.*;

@Component
public class DbTasksConverter {

    public Collection<Task> convertToTasksTree(List<DBObject> dbTasksObjects, List<Tag> allUserTags, boolean buildingPartOfATree) {
        Map<String, Tag> tagsMap = convertTagsToTagsMap(allUserTags);
        List<Task> topLevelTasks = new LinkedList<>();
        Map<String, Task> allTasksMap = new LinkedHashMap<>();

        List<DBObject> pathSortedDbTasks = sortTasksByPath(dbTasksObjects);

        for (DBObject dbTask : pathSortedDbTasks) {
            Task taskToAdd = convertSingleDbObjectToTask(dbTask, tagsMap);
            if (isTopLevelTask(dbTask, allTasksMap, buildingPartOfATree)) {
                topLevelTasks.add(taskToAdd);
                allTasksMap.put(taskToAdd.getId(), taskToAdd);
            } else {
                List<String> path = (List<String>) dbTask.get(PATH_KEY);
                Task parentTask = allTasksMap.get(path.get(path.size() - 1));
                assert parentTask != null : "Parent task cannot be null. " +
                        "Did you forget about sorting tasks by path or trying to build part of a tree?";
                parentTask.addSubtask(taskToAdd);
                taskToAdd.setParentTask(parentTask);
                allTasksMap.put(taskToAdd.getId(), taskToAdd);
            }
        }
        return topLevelTasks;
    }

    private List<DBObject> sortTasksByPath(List<DBObject> dbTasksObjects) {
        List<DBObject> pathSortedDbTasks = new ArrayList<>(dbTasksObjects);
        Collections.sort(pathSortedDbTasks, new Comparator<DBObject>() {
            @Override
            public int compare(DBObject taskDb1, DBObject taskDb2) {
                StringBuilder task1Path = new StringBuilder();
                for (String ancestorTaskId : (List<String>) taskDb1.get(PATH_KEY)) {
                    task1Path.append(ancestorTaskId).append(",");
                }
                StringBuilder task2Path = new StringBuilder();
                for (String ancestorTaskId : (List<String>) taskDb2.get(PATH_KEY)) {
                    task2Path.append(ancestorTaskId).append(",");
                }
                return task1Path.toString().compareTo(task2Path.toString());
            }
        });
        return pathSortedDbTasks;
    }

    public Collection<Task> convertToTasksTree(List<DBObject> dbTasksObjects, List<Tag> allUserTags) {
        return convertToTasksTree(dbTasksObjects, allUserTags, false);
    }

    private boolean isTopLevelTask(DBObject dbTask, Map<String, Task> alreadyReadTasks, boolean buildingPartOfATree) {
        List<String> path = (List<String>) dbTask.get(PATH_KEY);
        return path.isEmpty() ||
                (buildingPartOfATree && !alreadyReadTasks.containsKey(path.get(path.size() - 1)));
    }

    public Collection<Task> convertToFlatTasksList(List<DBObject> dbTasksObjects, List<Tag> allUserTags) {
        Map<String, Tag> tagsMap = convertTagsToTagsMap(allUserTags);
        Collection<Task> tasks = new ArrayList<>(dbTasksObjects.size());
        for (DBObject dbTask : dbTasksObjects) {
            tasks.add(convertSingleDbObjectToTask(dbTask, tagsMap));
        }
        return tasks;
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
            for (String tagId : (Collection<String>) dbTask.get(TAGS_KEY)) {
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
