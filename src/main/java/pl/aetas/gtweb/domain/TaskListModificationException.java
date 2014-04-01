package pl.aetas.gtweb.domain;

/**
 * Exception thrown while trying to add or remove task from/to the list and failed
 */
public class TaskListModificationException extends Exception {

    public TaskListModificationException(final String message, final Long taskId) {
        super(message + " task id = " + taskId);
    }
}
