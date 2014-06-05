package pl.aetas.gtweb.acceptance
import com.mongodb.BasicDBObject
import com.mongodb.QueryBuilder
import groovyx.net.http.ContentType
import org.bson.types.ObjectId
import pl.aetas.testing.RunJetty

@RunJetty
class TasksServiceSpec extends AcceptanceTestBase {

    def setupSpec() {
        cleanup()
    }

    def setup() {
        createTagInDb(TEST_USER_ID, 'planned', '#10f028', false)
        createTagInDb(TEST_USER_ID, 'next', '#5ca028', true)
    }

    void cleanup() {
        tasksCollection.remove(QueryBuilder.start('owner_id').is(TEST_USER_ID).get())
        tagsCollection.remove(QueryBuilder.start('owner_id').is(TEST_USER_ID).get())
    }

    def "should return 201 with newly created task when new task has been created"() {
        given: "user is authenticated"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        when: "client sends POST request to create a new task"
        def response = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Session-Id': sessionId])
        then: "new task object is returned"
        response.status == 201
        response.data.id != null
        response.data.title == 'Reserve the table in Pillars for 20th of March'
        response.data.tags.size() == 1
        response.data.tags.first().name == 'planned'
    }

    def "should return Forbidden (403) when unauthorized access"() {
        when: "client sends POST request to create a new task without authorization"
        def response = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON)
        then: "response Forbidden (403) should be returned"
        response.status == 403
    }

    def "should create task in DB with ownerId set"() {
        given: 'user exists in the session'
        def sessionId = createSessionWithUser(TEST_USER_ID)
        when: "client sends request to create new task"
        def response = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Session-Id': sessionId])
        then: "new task is saved in database"
        response.status == 201
        def taskDbObject = tasksCollection.findOne(new BasicDBObject('_id', new ObjectId(response.data.id)))
        taskDbObject.get('owner_id') == TEST_USER_ID
        taskDbObject.get('title') == 'Reserve the table in Pillars for 20th of March'
    }

    private static String JSON_TASK =
            '''{
                "closedDate": null,
                "description": "0208 231 2200",
                "dueDate": 1395100800000,
                "finished": false,
                "id": null,
                "startDate": null,
                "subtasks": [],
                "tags": [{
                        "color": "#10f028",
                        "name": "planned",
                        "visibleInWorkView": false
                    }],
                "title": "Reserve the table in Pillars for 20th of March"
            }'''

    void createTagInDb(String ownerId, String name, String color, boolean visibleInWorkView) {
        tagsCollection.insert(new BasicDBObject([name: name, owner_id: ownerId, color: color, visible_in_workview: visibleInWorkView]))
    }

    def "should retrieve all tasks belong to user from session"() {
        given: 'user exists in session and user has tasks'
        def sessionId = createSessionWithUser(TEST_USER_ID)
        client.post(path: 'tasks', body: '{"title": "taskTitle1"}', requestContentType: ContentType.JSON,
                headers: ['Session-Id': sessionId])
        client.post(path: 'tasks', body: '{"title": "taskTitle2"}', requestContentType: ContentType.JSON,
                headers: ['Session-Id': sessionId])
        when: 'client sends request to get all tasks'
        def response = client.get([path: 'tasks', headers: ['Session-Id': sessionId]])
        then: 'response contains list of tasks'
        response.status == 200
        response.data.collect { it.title }.contains('taskTitle1')
        response.data.collect { it.title }.contains('taskTitle2')
    }

    def "should remove task of given id from DB and return 204"() {
        given: 'user exists in session and user has tasks'
        def sessionId = createSessionWithUser(TEST_USER_ID)
        client.post(path: 'tasks', body: '{"title": "taskTitle1"}', requestContentType: ContentType.JSON,
                headers: ['Session-Id': sessionId])
        def createTaskResponse = client.post(path: 'tasks', body: '{"title": "taskTitle2"}',
                requestContentType: ContentType.JSON, headers: ['Session-Id': sessionId])
        when: 'client sends request to delete task'
        def response = client.delete([path: "tasks/${createTaskResponse.data.id}", headers: ['Session-Id': sessionId]])
        then: 'task should not exists in DB'
        tasksCollection.count(new BasicDBObject('_id', new ObjectId(createTaskResponse.data.id))) == 0
        and: 'response code is 204'
        response.status == 204
    }

    def "should return 404 when trying to delete non existing task"() {
        given: 'user exists without any tasks'
        def sessionId = createSessionWithUser(TEST_USER_ID)
        when: 'client sends request to delete non-existing task'
        def response = client.delete([path: 'tasks/nonExistingTaskId', headers: ['Session-Id': sessionId]])
        then: 'response code is 404 (not found)'
        response.status == 404
    }

    def "should return 404 when trying to remove another user's task"() {
        given: 'user A exists in session and has tasks'
        def userASessionId = createSessionWithUser(TEST_USER_ID)
        client.post(path: 'tasks', body: '{"title": "taskTitle1"}', requestContentType: ContentType.JSON,
                headers: ['Session-Id': userASessionId])
        def createTaskResponse = client.post(path: 'tasks', body: '{"title": "taskTitle2"}',
                requestContentType: ContentType.JSON, headers: ['Session-Id': userASessionId])
        and: 'user B exists without any tasks'
        def userBSessionId = createSessionWithUser('UserB')
        when: 'client sends request as user B to delete task of user A'
        def response = client.delete([path: "tasks/${createTaskResponse.data.id}", headers: ['Session-Id': userBSessionId]])
        then: 'response code is 404 (not found)'
        response.status == 404
    }

    def "should update task with given data when client sends PUT request with task id and task in JSON"() {
        given: "user is authenticated"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        and: "user has created a task"
        def taskCreatedResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Session-Id': sessionId])
        when: "client sends PUT request to update existing task"
        def response = client.put(path: "tasks/${taskCreatedResponse.data.id}", body: UPDATED_JSON_TASK,
                requestContentType: ContentType.JSON, headers: ['Session-Id': sessionId])
        then: "200 (OK) is returned in response"
        response.status == 200
        and: "all task field should be updated in response"
        response.data.title == 'This task title has been updated'
        response.data.closedDate == 1399411560000
        response.data.description == 'new description'
        response.data.dueDate == 1395273600000
        response.data.finished == true
        response.data.startDate == 1395273600000
        response.data.tags.size() == 1
        response.data.tags.first().name == 'next'
        and: "all fields of task should be updated in DB"
        def taskDbObject = tasksCollection.findOne(new BasicDBObject([_id: new ObjectId(taskCreatedResponse.data.id)]))
        taskDbObject.get('title') == 'This task title has been updated'
        taskDbObject.get('closed_date') == new Date(1399411560000)
        taskDbObject.get('description') == 'new description'
        taskDbObject.get('due_date') == new Date(1395273600000)
        taskDbObject.get('finished') == true
        taskDbObject.get('start_date') == new Date(1395273600000)
        taskDbObject.get('tags').size() == 1
        tagsCollection.findOne(new BasicDBObject([_id: new ObjectId(taskDbObject.get('tags').first())])).get('name') == 'next'
    }

    private static String UPDATED_JSON_TASK =
            // closed date: Tue May 06 2014 21:26:00
            // due date: Tue Mar 20 2014 00:00:00
            '''{
                "closedDate": 1399411560000,
                "description": "new description",
                "dueDate": 1395273600000,
                "finished": true,
                "startDate": 1395273600000,
                "subtasks": [],
                "tags": [{
                        "color": "#5ca028",
                        "name": "next",
                        "visibleInWorkView": true
                    }],
                "title": "This task title has been updated"
            }'''

    def "should return 404 when trying to update task with non-existing id"() {
        given: 'user exists without any tasks'
        def sessionId = createSessionWithUser(TEST_USER_ID)
        when: 'client sends request to update non-existing task'
        def response = client.put(path: "tasks/${ObjectId.get()}", body: UPDATED_JSON_TASK,
                requestContentType: ContentType.JSON, headers: ['Session-Id': sessionId])
        then: 'response code is 404 (not found)'
        response.status == 404
    }

    def "should return 404 when trying to update another user's task"() {
        given: 'user A exists in session and has tasks'
        def userASessionId = createSessionWithUser(TEST_USER_ID)
        def createTaskResponse = client.post(path: 'tasks', body: '{"title": "taskTitle2"}',
                requestContentType: ContentType.JSON, headers: ['Session-Id': userASessionId])
        and: 'user B exists without any tasks'
        def userBSessionId = createSessionWithUser('UserB')
        when: 'client sends request as user B to update task of user A'
        def response = client.put([path: "tasks/${createTaskResponse.data.id}", body: '{"title": "taskTitle3"}',
                                   requestContentType: ContentType.JSON, headers: ['Session-Id': userBSessionId]])
        then: 'response code is 404 (not found)'
        response.status == 404
    }

    def "should return 400 (Bad Request) when trying to update task with non-existing tags"() {
        given: "user is authenticated"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        and: "user has created a task"
        def taskCreatedResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Session-Id': sessionId])
        when: "client sends PUT request to update existing task with non-existing tags"
        def response = client.put(path: "tasks/${taskCreatedResponse.data.id}", body: UPDATED_JSON_TASK_WITH_INCORRECT_TAG,
                requestContentType: ContentType.JSON, headers: ['Session-Id': sessionId])
        then: "400 (Bad Request) is returned in response"
        response.status == 400
    }

    private static String UPDATED_JSON_TASK_WITH_INCORRECT_TAG =
            // closed date: Tue May 06 2014 21:26:00
            // due date: Tue Mar 20 2014 00:00:00
            '''{
                "closedDate": 1399411560000,
                "description": "new description",
                "dueDate": 1395273600000,
                "finished": true,
                "startDate": 1395273600000,
                "subtasks": [],
                "tags": [{
                        "color": "#5ca028",
                        "name": "non-existing",
                        "visibleInWorkView": true
                    }],
                "title": "This task title has been updated"
            }'''

    def "should add task of given taskId as given task's subtask"() {
        given: "user is authenticated"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        and: "user has two, top-level tasks"
        def taskBResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Session-Id': sessionId])
        def taskAResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Session-Id': sessionId])
        when: "client sends POST request to add existing task A as subtask to other existing task B"
        def response = client.post(path: "tasks/${taskBResponse.data.id}/subtasks/${taskAResponse.data.id}", headers: ['Session-Id': sessionId])
        then: "task A is saved as task's B subtask in the database"
        def taskAAfterUpdate = tasksCollection.findOne(new BasicDBObject([_id: new ObjectId(taskAResponse.data.id.toString())]))
        taskAAfterUpdate.path == [taskBResponse.data.id]
        and: "200 OK is returned in response"
        response.status == 200
        and: "updated task B is returned in response (task A is subtask of task B)"
        response.data.subtasks.first().id == taskAResponse.data.id
    }

    def "should add task of given taskId as given task's subtask lower in hierarchy"() {
        given: "user is authenticated"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        and: "user has three tasks, task A with subtask B and top-level task C"
        def taskAResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Session-Id': sessionId])
        def taskBResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Session-Id': sessionId])
        def taskCResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Session-Id': sessionId])
        client.post(path: "tasks/${taskAResponse.data.id}/subtasks/${taskBResponse.data.id}", headers: ['Session-Id': sessionId])
        when: "client sends POST request to add task C as subtask to task B"
        def response = client.post(path: "tasks/${taskBResponse.data.id}/subtasks/${taskCResponse.data.id}", headers: ['Session-Id': sessionId])
        then: "task C is saved as task's B subtask in the database"
        def taskCAfterUpdate = tasksCollection.findOne(new BasicDBObject([_id: new ObjectId(taskCResponse.data.id.toString())]))
        taskCAfterUpdate.path == [taskAResponse.data.id, taskBResponse.data.id]
        and: "200 OK is returned in response"
        response.status == 200
    }

    def "should return BAD REQUEST (400) when trying to add task as subtask to itself"() {
        given: "user is authenticated"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        and: "user has existing task"
        def taskResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Session-Id': sessionId])
        when: "client sends POST request to add task as subtask to itself"
        def response = client.post(path: "tasks/${taskResponse.data.id}/subtasks/${taskResponse.data.id}", headers: ['Session-Id': sessionId])
        then: "400 BAD REQUEST is returned in response"
        response.status == 400
    }

    def "should return BAD REQUEST (400) when trying to add task as subtask to create circular subtasks"() {
        given: "user is authenticated"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        and: "user has existing task A with subtask B"
        def taskAResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Session-Id': sessionId])
        def taskBResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Session-Id': sessionId])
        client.post(path: "tasks/${taskAResponse.data.id}/subtasks/${taskBResponse.data.id}", headers: ['Session-Id': sessionId])
        when: "client sends POST request to add task A as subtask to task B"
        def response = client.post(path: "tasks/${taskBResponse.data.id}/subtasks/${taskAResponse.data.id}", headers: ['Session-Id': sessionId])
        then: "400 BAD REQUEST is returned in response"
        response.status == 400
    }

    def "should remove task from parent task's subtasks when task is added as subtask to other task"() {
        given: "user is authenticated"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        and: "user has three tasks, task A with subtask B and top-level task C"
        def taskAResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Session-Id': sessionId])
        def taskBResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Session-Id': sessionId])
        def taskCResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Session-Id': sessionId])
        client.post(path: "tasks/${taskAResponse.data.id}/subtasks/${taskBResponse.data.id}", headers: ['Session-Id': sessionId])
        when: "client sends POST request to add task B as subtask to task C"
        client.post(path: "tasks/${taskCResponse.data.id}/subtasks/${taskBResponse.data.id}", headers: ['Session-Id': sessionId])
        then: "task B is saved as task's C subtask in the database"
        def taskBAfterUpdate = tasksCollection.findOne(new BasicDBObject([_id: new ObjectId(taskBResponse.data.id.toString())]))
        taskBAfterUpdate.path == [taskCResponse.data.id]
        and: "task A should not have any subtasks"
        def response = client.get([path: 'tasks', headers: ['Session-Id': sessionId]])
        response.data.find { it.id == taskAResponse.data.id }.subtasks.isEmpty()
    }

    def "should return NOT FOUND 404 when trying to add task as subtask to non-existing task"() {
        given: "user is authenticated"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        and: "user task"
        def taskResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Session-Id': sessionId])
        when: "client sends POST request to add task as subtask to non-existing task"
        def response = client.post(path: "tasks/${ObjectId.get().toString()}/subtasks/${taskResponse.data.id}", headers: ['Session-Id': sessionId])
        then: "404 NOT FOUND is returned in response"
        response.status == 404
    }

    def "should return NOT FOUND 404 when trying to add task as subtask to other user's task"() {
        given: 'user A exists with task A'
        def userASessionId = createSessionWithUser(TEST_USER_ID)
        def taskAResponse = client.post(path: 'tasks', body: '{"title": "taskTitleA"}',
                requestContentType: ContentType.JSON, headers: ['Session-Id': userASessionId])
        and: 'user B exists with task B'
        def userBSessionId = createSessionWithUser('UserB')
        def taskBResponse = client.post(path: 'tasks', body: '{"title": "taskTitleB"}',
                requestContentType: ContentType.JSON, headers: ['Session-Id': userBSessionId])
        when: "client as user B sends POST request to add task B as subtask to task A of user A"
        def response = client.post(path: "tasks/${taskAResponse.data.id}/subtasks/${taskBResponse.data.id}", headers: ['Session-Id': userBSessionId])
        then: "404 NOT FOUND is returned in response"
        response.status == 404
    }

    def "should move whole subtree under new parent task when moving task with subtasks"() {
        given: "user is authenticated"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        and: "user has three tasks, task A with subtask B with subtask C"
        def taskAResponse = client.post(path: 'tasks', body: '{"title": "taskTitleA"}', requestContentType: ContentType.JSON, headers: ['Session-Id': sessionId])
        def taskBResponse = client.post(path: 'tasks', body: '{"title": "taskTitleB"}', requestContentType: ContentType.JSON, headers: ['Session-Id': sessionId])
        def taskCResponse = client.post(path: 'tasks', body: '{"title": "taskTitleC"}', requestContentType: ContentType.JSON, headers: ['Session-Id': sessionId])
        client.post(path: "tasks/${taskAResponse.data.id}/subtasks/${taskBResponse.data.id}", headers: ['Session-Id': sessionId])
        client.post(path: "tasks/${taskBResponse.data.id}/subtasks/${taskCResponse.data.id}", headers: ['Session-Id': sessionId])
        and: "user has top-level task E"
        def taskEResponse = client.post(path: 'tasks', body: '{"title": "taskTitleE"}', requestContentType: ContentType.JSON, headers: ['Session-Id': sessionId])
        when: "client sends POST request to add task A as subtask of task E"
        client.post(path: "tasks/${taskEResponse.data.id}/subtasks/${taskAResponse.data.id}", headers: ['Session-Id': sessionId])
        then: "tasks A,B and C should be returned as task's E descendants"
        def response = client.get([path: 'tasks', headers: ['Session-Id': sessionId]])
        response.data.first().subtasks.first().id == taskAResponse.data.id
        response.data.first().subtasks.first().subtasks.first().id == taskBResponse.data.id
        response.data.first().subtasks.first().subtasks.first().subtasks.first().id == taskCResponse.data.id
    }
}
