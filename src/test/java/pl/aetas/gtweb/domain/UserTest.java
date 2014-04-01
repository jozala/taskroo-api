package pl.aetas.gtweb.domain;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class UserTest {

    private User user;

    @Mock
    private Task task;

    @Before
    public void setUp() {
        String username = "testuser";
        String email = "test@example.com";
        String firstname = "foo";
        String lastname = "bar";
        String password = "123456";
        Role role = Role.ROLE_ADMIN;
        user = new User.UserBuilder().username(username).email(email).firstName(firstname).lastName(lastname)
                .password(password).role(role).setEnabled(true).build();
    }

    @Test
    public void testAddTask() throws Exception {
        user.addTask(task);
        assertThat(user.getTasks()).containsOnly(task);
    }

    @Test
    public void testRemoveTask() throws Exception {
        user.addTask(task);
        user.removeTask(task);
        assertThat(user.getTasks()).isEmpty();

    }

    @Test(expected = TaskListModificationException.class)
    public void testRemoveTask_taskNotExists() throws TaskListModificationException {
        user.removeTask(task);
    }

}
