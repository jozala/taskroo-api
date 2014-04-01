package pl.aetas.gtweb.domain;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * First class collection for {@link Tag} class
 */
public class UserTags implements Serializable {

    private final Set<Tag> tags;

    public UserTags() {
        tags = new LinkedHashSet<>();
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

    public Tag getTag(final String name) {
        requireNonNull(name);
        for (final Tag tag : tags) {
            if (tag.getName().equals(name)) {
                return tag;
            }
        }
        return null;
    }

}