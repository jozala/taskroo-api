package pl.aetas.gtweb.service
import pl.aetas.gtweb.data.TagDao
import pl.aetas.gtweb.data.TaskDao
import pl.aetas.gtweb.domain.Tag
import pl.aetas.gtweb.domain.Task
import spock.lang.Specification

import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.SecurityContext
import java.security.Principal

class TasksServiceTest extends Specification {

    TasksService tasksService

    TaskDao taskDao = Mock(TaskDao)
    TagDao tagDao = Mock(TagDao)
    SecurityContext securityContext = Mock(SecurityContext)

    void setup() {
        tasksService = new TasksService(taskDao, tagDao);

        def principal = Mock(Principal)
        principal.getName() >> 'mariusz'
        securityContext.getUserPrincipal() >> principal
    }

    def "should return 201 when task has been created correctly"() {
        given:
        def task = Task.TaskBuilder.start('mariusz', 'taskTitle').createTask()
        def taskAfterSave = Task.TaskBuilder.start('mariusz', 'taskTitle').setId('someTaskId').createTask();
        taskDao.insert(task) >> taskAfterSave
        when:
        def response = tasksService.create(securityContext, task)
        then:
        response.status == 201
    }

    def "should save task"() {
        given:
        def task = Task.TaskBuilder.start('mariusz', 'taskTitle').createTask()
        def taskAfterSave = Task.TaskBuilder.start('mariusz', 'taskTitle').setId('someTaskId').createTask();
        when:
        tasksService.create(securityContext, task)
        then:
        1 * taskDao.insert(task) >> taskAfterSave
    }

    def "should return task after save in the entity"() {
        given:
        def task = Task.TaskBuilder.start('mariusz', 'taskTitle').createTask()
        def taskAfterSave = Task.TaskBuilder.start('mariusz', 'taskTitle').setId('someTaskId').createTask();
        taskDao.insert(task) >> taskAfterSave
        when:
        def response = tasksService.create(securityContext, task)
        then:
        response.entity == taskAfterSave
    }

    def "should return task resource location"() {
        given:
        def task = Task.TaskBuilder.start('mariusz', 'taskTitle').createTask()
        def taskAfterSave = Task.TaskBuilder.start('mariusz', 'taskTitle').setId('someTaskId').createTask();
        taskDao.insert(task) >> taskAfterSave
        when:
        def response = tasksService.create(securityContext, task)
        then:
        response.location == URI.create('tasks/someTaskId')
    }

    def "should throw exception when trying to create task with non-existing tags"() {
        given:
        def nonExistingTag = new Tag('mariusz', 'nonExisting', 'purple', false)
        def task = Task.TaskBuilder.start('mariusz', 'taskTitle')
                .addTag(nonExistingTag)
                .createTask()
        tagDao.exists(nonExistingTag) >> false
        when:
        tasksService.create(securityContext, task)
        then:
        thrown(WebApplicationException)
    }

    def "should set owner id from security context for the task when creating task"() {
        given:
        def task = Task.TaskBuilder.start('ownerId', 'someTitle').createTask()
        def principal = Mock(Principal)
        securityContext.getUserPrincipal() >> principal
        when:
        tasksService.create(securityContext, task)
        then:
        1 * taskDao.insert({ it.getOwnerId() == 'mariusz' }) >> task

    }

    // TODO test if task is given with existing tags
}
