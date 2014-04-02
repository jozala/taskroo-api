package pl.aetas.gtweb.domain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;

import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class TaskTest {

    @InjectMocks
    private Task task = new Task("title", "description", false, null, null, null, null, null, null);

    @InjectMocks
    private Task task2 = new Task("title", "description", false, null, null, null, null, null, null);

    @InjectMocks
    private Task task3 = new Task("title", "description", false, null, null, null, null, null, null);

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenTryingToSetParentTaskWithTheSameIdAsBaseTask() throws Exception {
        task.setParentTask(task);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenTryingToMakeParentTaskWhichIsSubtaskOfTheBaseSubtask() throws Exception {
        Task parentTask = mock(Task.class);
        task.addSubtask(parentTask);
        task.setParentTask(parentTask);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenTryingToMakeParentTaskWhichIsLowerInOriginalTaskSubtaskHierarchy()
            throws Exception {
        task.addSubtask(task2);
        task.addSubtask(task3);
        task.setParentTask(task3);
    }

    @Test
    public void isTaskInSubtasksHierarchy_shouldReturnTrueWhenGivenTaskIsSubtaskOfMainTask() throws Exception {
        task.addSubtask(task2);
        assertThat(task.isTaskInSubtasksHierarchy(task2)).isTrue();
    }

    @Test
    public void isTaskInSubtasksHierarchy_shouldReturnTrueWhenGivenTaskIsSubtaskOfMainTaskSubtask() throws Exception {
        task.addSubtask(task2);
        task2.addSubtask(task3);
        assertThat(task.isTaskInSubtasksHierarchy(task3)).isTrue();
    }

    @Test
    public void isTaskInSubtasksHierarchy_shouldReturnTrueWhenGivenTaskIsSubtaskOfMainTaskSubtaskSubtask()
            throws Exception {
        final Task task4 = mock(Task.class);
        task.addSubtask(task2);
        task2.addSubtask(task3);
        task3.addSubtask(task4);
        assertThat(task.isTaskInSubtasksHierarchy(task4)).isTrue();
    }

    @Test
    public void addSubtask_shouldSetSubtasksParentToTask() throws Exception {
        task.addSubtask(task2);

        assertThat(task2.getParentTask()).isSameAs(task);
    }



}
