package com.taskroo.domain;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class TaskTags implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -7977943994686583586L;

    private final Set<Tag> tags;

    public TaskTags() {
        tags = new LinkedHashSet<Tag>();
    }

    /**
     * Adds new tag at the end of the collection of tags
     *
     * @param newTag not null tag to add
     * @return true if this set did not already contain the specified element
     */
    public boolean add(final Tag newTag) {
        requireNonNull(newTag);
        return tags.add(newTag);
    }

    /**
     * Remove given tag from the tags collection.
     *
     * @param tagToRemove tag to remove from this tag collection
     * @return true if the set contained the specified element
     */
    public boolean remove(final Tag tagToRemove) {
        requireNonNull(tagToRemove);
        return tags.remove(tagToRemove);
    }

    /**
     * Gets elements of the newTags which does not exist in the tags collections yet and add them at the end of the
     * collection. No tag will be added to the tags list if the tag of the same name (and owner) already exists.
     *
     * @param newTags tags
     */
    public void addNonExistingTags(final Collection<Tag> newTags) {
        tags.addAll(newTags);
    }

    /**
     * Returns unmodifiable set of tags
     *
     * @return unmodifiable set of tags
     */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }
}
