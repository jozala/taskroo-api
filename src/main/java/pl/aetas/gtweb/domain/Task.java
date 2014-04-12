package pl.aetas.gtweb.domain;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.*;

public class Task {

    private static final int MAX_SUBTASKS_LEVELS = 10;

    private String id;
    @JsonIgnore
    private String ownerId;
    private final String title;
    private final String description;
    private final Set<Tag> tags;
    private final List<Task> subtasks;
    private final boolean finished;
    private final Date createdDate;
    @JsonIgnore
    private final Task parentTask;
    private final Date startDate;
    private final Date dueDate;
    private final Date closedDate;

    @JsonCreator
    private Task(@JsonProperty("id") String id, @JsonProperty("ownerId") String ownerId,
                 @JsonProperty("title") String title, @JsonProperty("description") String description,
                 @JsonProperty("tags") Set<Tag> tags, @JsonProperty("subtasks") List<Task> subtasks,
                 @JsonProperty("finished") boolean finished, @JsonProperty("closedDate") Date closedDate,
                 @JsonProperty("createdDate") Date createdDate, @JsonProperty("startDate") Date startDate,
                 @JsonProperty("dueDate") Date dueDate, @JsonProperty("parentTask") Task parentTask) {
        this.id = id;
        this.ownerId = ownerId;
        this.description = description;
        this.title = title;
        this.tags = tags;
        this.subtasks = subtasks;
        this.finished = finished;
        this.closedDate = closedDate;
        this.createdDate = createdDate;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.parentTask = parentTask;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFinished() {
        return finished;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public Date getClosedDate() {
        return closedDate;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public List<Task> getSubtasks() {
        return Collections.unmodifiableList(subtasks);
    }

    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public Task getParentTask() {
        return parentTask;
    }

    public boolean isTaskInSubtasksHierarchy(final Task task) {
        final List<Task> allTaskInSubtaskHierarchy = getAllTaskInSubtasksHierarchy(0);
        for (final Task taskFromHierarchy : allTaskInSubtaskHierarchy) {
            if (taskFromHierarchy.equals(task)) {
                return true;
            }
        }
        return false;
    }

    private List<Task> getAllTaskInSubtasksHierarchy(final int level) {
        if (level >= MAX_SUBTASKS_LEVELS) {
            throw new IllegalStateException("Task should never has more than " + MAX_SUBTASKS_LEVELS
                    + " levels of subtasks");
        }
        final List<Task> subtasksFromLevel = this.getSubtasks();
        if (subtasksFromLevel.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Task> wholeHierarchySubtasks = new LinkedList<>();
        wholeHierarchySubtasks.addAll(subtasksFromLevel);
        for (final Task subtask : subtasksFromLevel) {
            wholeHierarchySubtasks.addAll(subtask.getAllTaskInSubtasksHierarchy(level + 1));
        }

        return wholeHierarchySubtasks;
    }

    public String getId() {
        return id;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }


    public static class TaskBuilder {

        private String id;
        private String ownerId;
        private String title;
        private String description;
        private Set<Tag> tags;
        private List<Task> subtasks;
        private boolean finished;
        private Date createdDate;
        private Date startDate;
        private Date dueDate;
        private Date closedDate;
        private Task parentTask;

        private TaskBuilder() {
            tags = new HashSet<>();
            subtasks = new LinkedList<>();
        }

        public static TaskBuilder start(String ownerId, String title) {
            TaskBuilder builder = new TaskBuilder();
            builder.setOwnerId(Objects.requireNonNull(ownerId));
            builder.setTitle(Objects.requireNonNull(title));
            return builder;
        }

        public TaskBuilder setId(String id) {
            this.id = id;
            return this;
        }

        public TaskBuilder setTitle(String title) {
            this.title = Objects.requireNonNull(title);
            return this;
        }

        public TaskBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public TaskBuilder setFinished(boolean finished) {
            this.finished = finished;
            return this;
        }

        public TaskBuilder setOwnerId(String ownerId) {
            this.ownerId = Objects.requireNonNull(ownerId);
            return this;
        }

        public TaskBuilder setCreatedDate(Date createdDate) {
            this.createdDate = Objects.requireNonNull(createdDate);
            return this;
        }

        public TaskBuilder setClosedDate(Date closedDate) {
            this.closedDate = Objects.requireNonNull(closedDate);
            return this;
        }

        public TaskBuilder setStartDate(Date startDate) {
            this.startDate = Objects.requireNonNull(startDate);
            return this;
        }

        public TaskBuilder setDueDate(Date dueDate) {
            this.dueDate = Objects.requireNonNull(dueDate);
            return this;
        }

        public TaskBuilder addTag(Tag tag) {
            tags.add(Objects.requireNonNull(tag));
            return this;
        }

        public TaskBuilder addSubtask(Task subtask) {
            subtasks.add(Objects.requireNonNull(subtask));
            return this;
        }

        public TaskBuilder setParentTask(Task parentTask) {
            this.parentTask = parentTask;
            return this;
        }

        public Task createTask() {
            if (title == null || ownerId == null) {
                throw new NullPointerException("Task cannot be build without required fields: title, ownerId");
            }
            return new Task(id, ownerId, title, description, tags, subtasks, finished, createdDate, closedDate, startDate,
                    dueDate, parentTask);
        }
    }
}
