package pl.aetas.gtweb.data;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.springframework.stereotype.Repository;
import pl.aetas.gtweb.domain.Tag;
import pl.aetas.gtweb.domain.Task;

import javax.inject.Inject;
import java.util.*;

@Repository
public class TaskDao {

    private final DBCollection tasksCollection;

    @Inject
    public TaskDao(DBCollection tasksCollection) {
        this.tasksCollection = tasksCollection;
    }

    public Task insert(Task task) {
        if (!task.getSubtasks().isEmpty()) {
            throw new UnsupportedDataOperationException("Cannot insert task with subtasks. This is not supported.");
        }

        DBObject taskDbObject = BasicDBObjectBuilder.start("title", task.getTitle())
                .append("description", task.getDescription())
                .append("due_date", task.getDueDate())
                .append("start_date", task.getStartDate())
                .append("created_date", task.getCreatedDate())
                .append("closed_date", task.getClosedDate())
                .append("finished", task.isFinished())
                .append("subtasks", Collections.emptyList())
                .append("owner_id", task.getOwnerId())
                // TODO keeping tags by names here means that when changing tags names it will have to be changes in every task
                .append("tags", getTagsNames(task.getTags()))
                .append("ancestors", getAncestorsIds(task.getParentTask()))
                .get();

        tasksCollection.insert(taskDbObject);
        String taskId = taskDbObject.get("_id").toString();

        task.setId(taskId);
        return task;
    }

    private Set<String> getTagsNames(Set<Tag> tags) {
        Set<String> tagsNames = new HashSet<>();
        for (Tag tag : tags) {
            tagsNames.add(tag.getName());
        }
        return tagsNames;
    }

    private List<String> getAncestorsIds(Task parentTask) {
        if (parentTask == null) {
            return new LinkedList<>();
        }
        List<String> ancestorsIds = getAncestorsIds(parentTask.getParentTask());
        ancestorsIds.add(parentTask.getId());
        return ancestorsIds;
    }
}
