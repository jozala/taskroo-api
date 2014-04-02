package pl.aetas.gtweb.domain;

import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.*;

import static java.util.Objects.requireNonNull;

public class Task {

    private static final int MAX_SUBTASKS_LEVELS = 10;

    private static final long serialVersionUID = 1032323894884580197L;
    private final List<Task> subtasks;
    private final TaskTags tags;
    private final Date createdDate;
    private String title;
    private String description;
    private boolean finished;
    @JsonIgnore
    private User owner;
    @JsonIgnore
    private Task parentTask;
    private Date startingOn;
    private Date dueDate;
    private Date closedDate;


    public Task(final String title, final String description, final boolean finished, final User owner,
                final Task parentTask, final Date startingOn, final Date dueDate, final Date closedDate,
                final Date createdDate) {

        this.title = requireNonNull(title);
        this.description = description;
        this.finished = finished;
        this.subtasks = new LinkedList<>();
        this.parentTask = parentTask;
        this.tags = new TaskTags();
        this.startingOn = startingOn;
        this.dueDate = dueDate;
        this.closedDate = closedDate;
        this.owner = owner;
        this.createdDate = createdDate;

    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = requireNonNull(title);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public boolean getFinished() {
        return finished;
    }

    public void setFinished(final boolean finished) {
        this.finished = finished;
    }

    public Task getParentTask() {
        return parentTask;
    }

    /**
     * Sets parent task of this task. If it is null than this task is at the top.
     *
     * @param parentTask task which is parent of this task or null if this task should has no parent
     */
    public void setParentTask(Task parentTask) {
        if (this.equals(parentTask)) {
            throw new IllegalArgumentException("Cannot set the task as its own parent");
        }
        if (isTaskInSubtasksHierarchy(parentTask)) {
            throw new IllegalArgumentException("Cannot set the task subtask as its parent");
        }
        this.parentTask = parentTask;
    }

    public Date getStartingOn() {
        return startingOn;
    }

    public void setStartingOn(final Date startingOn) {
        this.startingOn = startingOn;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(final Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getClosedDate() {
        return closedDate;
    }

    public void setClosedDate(final Date closedDate) {
        this.closedDate = closedDate;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = requireNonNull(owner);
    }

    public List<Task> getSubtasks() {
        return Collections.unmodifiableList(subtasks);
    }

    public Set<Tag> getTags() {
        return tags.getTags();
    }

    /**
     * Adds tag to this task's tags
     *
     * @param tag to add to this task
     * @return true if this task did not already contain the specified tag
     */
    public boolean addTag(Tag tag) {
        requireNonNull(tag);
        return tags.add(tag);
    }

    public void addTags(final Set<Tag> tagsSet) {
        tags.addNonExistingTags(tagsSet);
    }

    /**
     * Removes given tag from task
     *
     * @param tag to remove from task
     * @return true if this set contained the specified tag
     */
    public boolean removeTag(final Tag tag) {
        requireNonNull(tag);
        return tags.remove(tag);
    }

    public void addSubtask(final Task task) {
        requireNonNull(task);
        task.setParentTask(this);
        subtasks.add(task);
    }

    /**
     * Removes given subtask from this task
     *
     * @param task to remove from task's subtasks list
     * @return true if this task contained the specified subtask
     */
    public boolean removeSubtask(final Task task) {
        requireNonNull(task);
        return subtasks.remove(task);
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

        final List<Task> allHierarchySubtasks = new LinkedList<>();
        allHierarchySubtasks.addAll(subtasksFromLevel);
        for (final Task subtask : subtasksFromLevel) {
            allHierarchySubtasks.addAll(subtask.getAllTaskInSubtasksHierarchy(level + 1));
        }

        return allHierarchySubtasks;
    }

}
