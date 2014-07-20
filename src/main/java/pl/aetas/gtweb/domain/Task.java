package pl.aetas.gtweb.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.*;

@JsonDeserialize(builder = Task.TaskBuilder.class)
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
    private Date createdDate;
    @JsonIgnore
    private Task parentTask;
    private final Date startDate;
    private final Date dueDate;
    private final Date closedDate;

    private Task(String id, String ownerId, String title, String description, Set<Tag> tags, List<Task> subtasks,
                 boolean finished, Date closedDate, Date createdDate, Date startDate, Date dueDate, Task parentTask) {
        this.id = id;
        this.ownerId = ownerId;
        this.description = description;
        this.title = title;
        this.tags = new HashSet<>();
        this.tags.addAll(tags);
        this.subtasks = new LinkedList<>();
        this.subtasks.addAll(subtasks);
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

    public void addSubtask(Task task) {
        subtasks.add(task);
    }

    public void setParentTask(Task parentTask) {
        this.parentTask = parentTask;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
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

        public TaskBuilder() {
            tags = new HashSet<>();
            subtasks = new LinkedList<>();
        }

        @JsonProperty("id")
        public TaskBuilder setId(String id) {
            this.id = id;
            return this;
        }

        @JsonProperty(value = "title", required = true)
        public TaskBuilder setTitle(String title) {
            this.title = Objects.requireNonNull(title);
            return this;
        }

        @JsonProperty("description")
        public TaskBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        @JsonProperty("finished")
        public TaskBuilder setFinished(boolean finished) {
            this.finished = finished;
            return this;
        }


        public TaskBuilder setOwnerId(String ownerId) {
            this.ownerId = Objects.requireNonNull(ownerId);
            return this;
        }

        @JsonProperty("createdDate")
        public TaskBuilder setCreatedDate(Date createdDate) {
            this.createdDate = Objects.requireNonNull(createdDate);
            return this;
        }

        @JsonProperty("closedDate")
        public TaskBuilder setClosedDate(Date closedDate) {
            this.closedDate = closedDate;
            return this;
        }

        @JsonProperty("startDate")
        public TaskBuilder setStartDate(Date startDate) {
            this.startDate = startDate;
            return this;
        }

        @JsonProperty("dueDate")
        public TaskBuilder setDueDate(Date dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public TaskBuilder addTag(Tag tag) {
            tags.add(Objects.requireNonNull(tag));
            return this;
        }

        @JsonProperty("tags")
        public TaskBuilder addTags(List<Tag> tags) {
            this.tags.addAll(tags);
            return this;
        }

        public TaskBuilder addSubtask(Task subtask) {
            subtasks.add(Objects.requireNonNull(subtask));
            return this;
        }

        @JsonProperty("subtasks")
        public TaskBuilder addSubtasks(List<Task> subtasks) {
            this.subtasks.addAll(subtasks);
            return this;
        }

        public TaskBuilder setParentTask(Task parentTask) {
            this.parentTask = parentTask;
            return this;
        }

        public Task build() {
            if (title == null) {
                throw new NullPointerException("Task cannot be build without title");
            }
            return new Task(id, ownerId, title, description, tags, subtasks, finished, closedDate, createdDate, startDate,
                    dueDate, parentTask);
        }
    }
}
