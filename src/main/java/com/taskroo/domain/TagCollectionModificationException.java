package com.taskroo.domain;

/**
 * Exception thrown while trying to add or remove tag from/to the collection and failed
 */
public class TagCollectionModificationException extends Exception {

    public TagCollectionModificationException(final String message, final Long tagId) {
        super(message + " tag id = " + tagId);
    }

}
