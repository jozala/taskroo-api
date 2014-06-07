package pl.aetas.gtweb.data;

public class ConcurrentTasksModificationException extends Exception {
    public ConcurrentTasksModificationException(String message) {
        super(message);
    }
}
