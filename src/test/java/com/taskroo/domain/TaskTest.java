package com.taskroo.domain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class TaskTest {

    @Test
    public void isTaskInSubtasksHierarchy_shouldReturnTrueWhenGivenTaskIsSubtaskOfMainTask() throws Exception {
        Task task2 = new Task.TaskBuilder().setOwnerId("mariusz").setTitle("title").setCreatedDate(new Date()).build();
        Task task = new Task.TaskBuilder().setOwnerId("mariusz").setTitle("title").setCreatedDate(new Date()).addSubtask(task2).build();
        assertThat(task.isTaskInSubtasksHierarchy(task2)).isTrue();
    }

    @Test
    public void isTaskInSubtasksHierarchy_shouldReturnTrueWhenGivenTaskIsSubtaskOfMainTaskSubtask() throws Exception {
        Task task3 = new Task.TaskBuilder().setOwnerId("mariusz").setTitle("title").setCreatedDate(new Date()).build();
        Task task2 = new Task.TaskBuilder().setOwnerId("mariusz").setTitle("title").setCreatedDate(new Date()).addSubtask(task3).build();
        Task task1 = new Task.TaskBuilder().setOwnerId("mariusz").setTitle("title").setCreatedDate(new Date()).addSubtask(task2).build();
        assertThat(task1.isTaskInSubtasksHierarchy(task3)).isTrue();
    }
}
