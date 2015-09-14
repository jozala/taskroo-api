package com.taskroo.service
import org.bson.types.ObjectId
import com.taskroo.data.*
import com.taskroo.domain.Task
import spock.lang.Specification

import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.SecurityContext
import java.security.Principal

class TasksServiceTest extends Specification {

    public static final String TEST_USER_ID = 'testUserId123'

    TasksService tasksService

    TaskDao taskDao = Mock(TaskDao)
    SecurityContext securityContext = Mock(SecurityContext)

    void setup() {
        tasksService = new TasksService(taskDao);

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
        def response = tasksService.getAll(securityContext, null, null, null, null, null)
        then:
        response.status == 200
        response.entity == tasks
    }

    def "should remove task from DB using given task's id and user from security context when removing task"() {
        when:
        tasksService.delete(securityContext, "someTaskId")
        then:
        1 * taskDao.remove(TEST_USER_ID, "someTaskId")
    }

    def "should return 404 (not found) when task with given id does not exists for given user"() {
        given:
        taskDao.remove(TEST_USER_ID, "nonExistingTaskId") >> { throw new NonExistingResourceOperationException('') }
        when:
        def response = tasksService.delete(securityContext, "nonExistingTaskId")
        then:
        response.status == 404
    }

    def "should return 204 when task has been removed correctly"() {
        when:
        def response = tasksService.delete(securityContext, "someTaskId")
        then:
        response.status == 204
    }

    def "should update task in DB when requested"() {
        given:
        def task = new Task.TaskBuilder().setOwnerId(TEST_USER_ID).setTitle('taskTitle').setCreatedDate(new Date()).build()
        when:
        tasksService.update(securityContext, 'someTaskId', task)
        then:
        1 * taskDao.update(TEST_USER_ID, task)
    }

    def "should return 200 when task has been updated"() {
        given:
        def task = new Task.TaskBuilder().setOwnerId(TEST_USER_ID).setTitle('taskTitle').setCreatedDate(new Date()).build()
        when:
        def response = tasksService.update(securityContext, 'someTaskId', task)
        then:
        response.status == 200
    }

    def "should return updated task when task has been updated"() {
        given:
        def task = new Task.TaskBuilder().setOwnerId(TEST_USER_ID).setTitle('taskTitle').setCreatedDate(new Date()).build()
        def taskAfterUpdate = new Task.TaskBuilder().setOwnerId(TEST_USER_ID).setTitle('taskTitle').setCreatedDate(new Date()).build()
        taskDao.update(_, _) >> taskAfterUpdate
        when:
        def response = tasksService.update(securityContext, 'someTaskId', task)
        then:
        response.status == 200
        response.entity.is(taskAfterUpdate)
    }

    def "should return 404 (not found) when trying to update non-existing task"() {
        given:
        def task = new Task.TaskBuilder().setOwnerId(TEST_USER_ID).setTitle('taskTitle').setCreatedDate(new Date()).build()
        taskDao.update(TEST_USER_ID, task) >> { throw new NonExistingResourceOperationException('') }
        when:
        def response = tasksService.update(securityContext, 'someTaskId', task)
        then:
        response.status == 404
    }

    def "should throw exception when trying to add subtask and give null as parent task's id"() {
        when:
        tasksService.addSubtask(securityContext, null, ObjectId.get().toString())
        then:
        thrown(NullPointerException)
    }

    def "should throw exception when trying to add subtask and give null as subtaskId"() {
        when:
        tasksService.addSubtask(securityContext, ObjectId.get().toString(), null)
        then:
        thrown(NullPointerException)
    }

    def "should add subtask to task in DB passing logged user id as owner"() {
        given:
        def parentTaskId = ObjectId.get().toString()
        def subtaskId = ObjectId.get().toString()
        when:
        tasksService.addSubtask(securityContext, parentTaskId, subtaskId)
        then:
        1 * taskDao.addSubtask(TEST_USER_ID, parentTaskId, subtaskId)
    }

    def "should return 404 when given parentId not exists"() {
        given:
        def parentTaskId = ObjectId.get().toString()
        def subtaskId = ObjectId.get().toString()
        taskDao.addSubtask(TEST_USER_ID, parentTaskId, subtaskId) >> {
            throw new NonExistingResourceOperationException('')
        }
        when:
        def response = tasksService.addSubtask(securityContext, parentTaskId, subtaskId)
        then:
        response.status == 404
    }

    def "should throw exception with 400 response when DB return data as incorrect when trying to add subtask"() {
        given:
        def parentTaskId = ObjectId.get().toString()
        def subtaskId = ObjectId.get().toString()
        taskDao.addSubtask(TEST_USER_ID, parentTaskId, subtaskId) >> { throw new UnsupportedDataOperationException('') }
        when:
        tasksService.addSubtask(securityContext, parentTaskId, subtaskId)
        then:
        thrown(WebApplicationException)
    }

    def "should return 200 response when task has been successfully added as a subtask"() {
        given:
        def parentTaskId = ObjectId.get().toString()
        def subtaskId = ObjectId.get().toString()
        when:
        def response = tasksService.addSubtask(securityContext, parentTaskId, subtaskId)
        then:
        response.status == 200
    }

    def "should return updated parent task returned by DAO when subtask has been successfully added"() {
        given:
        def parentTaskId = ObjectId.get().toString()
        def subtaskId = ObjectId.get().toString()
        def parentTaskAfterUpdateMock = Mock(Task)
        taskDao.addSubtask(TEST_USER_ID, parentTaskId, subtaskId) >> parentTaskAfterUpdateMock
        when:
        def response = tasksService.addSubtask(securityContext, parentTaskId, subtaskId)
        then:
        response.entity == parentTaskAfterUpdateMock
    }

    def "should return 409 Conflict when DAO throws ConcurrentTaskModificationException"() {
        given:
        def parentTaskId = ObjectId.get().toString()
        def subtaskId = ObjectId.get().toString()
        taskDao.addSubtask(TEST_USER_ID, parentTaskId, subtaskId) >> {
            throw new ConcurrentTasksModificationException('')
        }
        when:
        def response = tasksService.addSubtask(securityContext, parentTaskId, subtaskId)
        then:
        response.status == 409
        response.entity != null
    }
}
