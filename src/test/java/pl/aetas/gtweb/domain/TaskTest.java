package pl.aetas.gtweb.domain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class TaskTest {

    @InjectMocks
    private Task task = Task.TaskBuilder.start("mariusz", "title").setFinished(false).createTask();

    @InjectMocks
    private Task task2 = Task.TaskBuilder.start("mariusz", "title").setFinished(false).createTask();

    @InjectMocks
    private Task task3 = Task.TaskBuilder.start("mariusz", "title").setFinished(false).createTask();

    @Test
    public void isTaskInSubtasksHierarchy_shouldReturnTrueWhenGivenTaskIsSubtaskOfMainTask() throws Exception {
        Task task = Task.TaskBuilder.start("mariusz", "title").addSubtask(task2).createTask();
        assertThat(task.isTaskInSubtasksHierarchy(task2)).isTrue();
    }

    @Test
    public void isTaskInSubtasksHierarchy_shouldReturnTrueWhenGivenTaskIsSubtaskOfMainTaskSubtask() throws Exception {
        Task task2 = Task.TaskBuilder.start("mariusz", "title").addSubtask(task3).createTask();
        Task task1 = Task.TaskBuilder.start("mariusz", "title").addSubtask(task2).createTask();
        assertThat(task1.isTaskInSubtasksHierarchy(task3)).isTrue();
    }
}
