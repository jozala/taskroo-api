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

    public static final String TEST_USER_ID = 'testUserId123'

    TasksService tasksService

    TaskDao taskDao = Mock(TaskDao)
    TagDao tagDao = Mock(TagDao)
    SecurityContext securityContext = Mock(SecurityContext)

    void setup() {
        tasksService = new TasksService(taskDao, tagDao);

        def principal = Mock(Principal)
        principal.getName() >> TEST_USER_ID
        securityContext.getUserPrincipal() >> principal
    }

    def "should return 201 when task has been created correctly"() {
        given:
        def task = new Task.TaskBuilder().setOwnerId(TEST_USER_ID).setTitle('taskTitle').setCreatedDate(new Date()).build()
        def taskAfterSave = new Task.TaskBuilder().setOwnerId(TEST_USER_ID).setTitle('taskTitle')
                .setId('someTaskId').setCreatedDate(new Date()).build();
        taskDao.insert(task) >> taskAfterSave
        when:
        def response = tasksService.create(securityContext, task)
        then:
        response.status == 201
    }

    def "should save task"() {
        given:
        def task = new Task.TaskBuilder().setOwnerId(TEST_USER_ID).setTitle('taskTitle').setCreatedDate(new Date()).build()
        def taskAfterSave = new Task.TaskBuilder().setOwnerId(TEST_USER_ID).setTitle('taskTitle')
                .setId('someTaskId').setCreatedDate(new Date()).build();
        when:
        tasksService.create(securityContext, task)
        then:
        1 * taskDao.insert(task) >> taskAfterSave
    }

    def "should return task after save in the entity"() {
        given:
        def task = new Task.TaskBuilder().setOwnerId(TEST_USER_ID).setTitle('taskTitle').setCreatedDate(new Date()).build()
        def taskAfterSave = new Task.TaskBuilder().setOwnerId(TEST_USER_ID).setTitle('taskTitle')
                .setId('someTaskId').setCreatedDate(new Date()).build();
        taskDao.insert(task) >> taskAfterSave
        when:
        def response = tasksService.create(securityContext, task)
        then:
        response.entity == taskAfterSave
    }

    def "should return task resource location"() {
        given:
        def task = new Task.TaskBuilder().setOwnerId(TEST_USER_ID).setTitle('taskTitle').setCreatedDate(new Date()).build()
        def taskAfterSave = new Task.TaskBuilder().setOwnerId(TEST_USER_ID).setTitle('taskTitle')
                .setId('someTaskId').setCreatedDate(new Date()).build();
        taskDao.insert(task) >> taskAfterSave
        when:
        def response = tasksService.create(securityContext, task)
        then:
        response.location == URI.create('tasks/someTaskId')
    }

    def "should throw exception when trying to create task with non-existing tags"() {
        given:
        def nonExistingTag = new Tag('someId', TEST_USER_ID, 'nonExisting', 'purple', false)
        def task = new Task.TaskBuilder().setOwnerId(TEST_USER_ID).setTitle('taskTitle')
                .setCreatedDate(new Date())
                .addTag(nonExistingTag)
                .build()
        tagDao.exists(TEST_USER_ID, 'nonExisting') >> false
        when:
        tasksService.create(securityContext, task)
        then:
        thrown(WebApplicationException)
    }

    def "should set owner id from security context for the task when creating task"() {
        given:
        def task = new Task.TaskBuilder().setOwnerId('ownerId').setTitle('someTitle').setCreatedDate(new Date()).build()
        when:
        tasksService.create(securityContext, task)
        then:
        1 * taskDao.insert({ it.getOwnerId() == TEST_USER_ID }) >> task

    }

    def "should return tasks retrieved from DB for specified user"() {
        given:
        def tasks = (1..4).collect {
            new Task.TaskBuilder().setOwnerId(TEST_USER_ID).setTitle("title$it").setCreatedDate(new Date()).build()
        }
        taskDao.findAllByOwnerId(TEST_USER_ID) >> tasks
        when:
        def response = tasksService.getAll(securityContext)
        then:
        response.status == 200
        response.entity == tasks
    }
}
