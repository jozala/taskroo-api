package pl.aetas.gtweb.domain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.Principal;
import java.util.*;

import static java.util.Objects.requireNonNull;

public class User implements Principal {

    private final String username;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String password;
    private final Set<Role> roles;
    private final List<Task> tasks;
    private final UserTags tags;
    private final boolean enabled;

    private User(final String username, final String email, final String firstName, final String lastName, final String password, final Set<Role> roles,
                 final boolean enabled) {
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.roles = roles;
        this.tasks = new LinkedList<Task>();
        this.tags = new UserTags();
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    /**
     * Returns user's roles
     *
     * @return user's roles
     */

    public Set<Role> getRoles() {
        return roles;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPassword() {
        return password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns unmodifiable list of user's tasks
     *
     * @return unmodifiable list of user's tasks
     */
    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    /**
     * Adds new task at the end of user's tasks list
     *
     * @param task new task to add
     * @throws TaskListModificationException thrown when task has not been added to the tasks list
     */
    public void addTask(final Task task) throws TaskListModificationException {
        requireNonNull(task);
        final boolean added = tasks.add(task);
        if (!added) {
            // TODO task used to have id which has been added as a parameter to this exception
            throw new TaskListModificationException("Task not added to list", 666L);
        }
    }

    /**
     * Removes given task from the user's tasks list
     *
     * @param task task to remove
     * @throws TaskListModificationException thrown when task has not been removed from the tasks list
     */
    public void removeTask(final Task task) throws TaskListModificationException {
        final boolean removed = tasks.remove(task);
        if (!removed) {
            // TODO task used to have id which has been added as a parameter to this exception
            throw new TaskListModificationException("Task not removed from list", 666L);
        }
    }

    public void removeTag(final Tag tag) throws TagCollectionModificationException {
        final boolean removed = tags.remove(tag);
        if (!removed) {
            // TODO task used to have id which has been added as a parameter to this exception
            throw new TagCollectionModificationException("Tag not removed from list", 666L);
        }
    }

    public Tag getTag(final String tagName) {
        requireNonNull(tagName);
        return tags.getTag(tagName);
    }

    @Override
    public String getName() {
        return getUsername();
    }

    public static class UserBuilder {

        private final Logger logger = LogManager.getLogger(UserBuilder.class);

        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String password;
        private Set<Role> roles = new HashSet<>();
        private boolean enabled;

        public UserBuilder username(final String username) {

            this.username = username;
            return this;
        }

        public UserBuilder firstName(final String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserBuilder lastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserBuilder password(final String password) {
            this.password = password;
            logger.warn("User password has been saved without encoding.");
            return this;
        }

        public UserBuilder email(final String email) {
            this.email = email;
            return this;
        }

        public UserBuilder role(final Role role) {
            this.roles.add(role);
            return this;
        }

        public UserBuilder setEnabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public UserBuilder update(final User user) {
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.firstName = user.getFirstName();
            this.lastName = user.getLastName();
            this.password = user.getPassword();
            this.roles = new HashSet<>(user.getRoles());
            this.enabled = user.isEnabled();
            return this;
        }

        public User build() {
            requireNonNull(username, "Username has to be specified first. Actual [" + username + "]");
            requireNonNull(email, "E-mail has to be specified first. Actual [" + email + "]");
            requireNonNull(firstName, "First name has to be specified first. Actual [" + firstName + "]");
            requireNonNull(lastName, "Last name has to be specified first. Actual [" + lastName + "]");
            requireNonNull(password, "Password has to be specified first. Actual [" + password + "]");
            requireNonNull(roles, "Roles has to be specified first. Actual [" + roles + "]");
            return new User(username, email, firstName, lastName, password, roles, enabled);
        }

    }

}
