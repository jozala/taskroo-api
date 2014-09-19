package com.taskroo.acceptance
import com.mongodb.BasicDBObject
import com.mongodb.QueryBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import org.bson.types.ObjectId
import com.taskroo.testing.RunJetty
import org.joda.time.DateTime

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
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        when: "client sends POST request to create a new task"
        def response = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "new task object is returned"
        response.status == 201
        response.data.id != null
        response.data.title == 'Reserve the table in Pillars for 20th of March'
        response.data.tags.size() == 1
        response.data.tags.first().name == 'planned'
    }

    def "should return 400 (Bad Request) when trying to insert task with non-existing tags"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        when: "client sends POST request to create task with non-existing tags"
        def response = client.post(path: "tasks", body: UPDATED_JSON_TASK_WITH_INCORRECT_TAG,
                requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "400 (Bad Request) is returned in response"
        response.status == 400
    }

    def "should return 400 (Bad Request) and error message when trying to insert task with empty title"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        when: "client sends POST request to create a new task with empty title"
        def response = client.post(path: 'tasks', body: '{"title": ""}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "400 (Bad Request) is returned"
        response.status == 400
    }

    def "should return 400 (Bad Request) when trying to insert task with null title"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        when: "client sends POST request to create a new task with empty title"
        def response = client.post(path: 'tasks', body: '{"title": null}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "400 (Bad Request) is returned"
        response.status == 400
    }

    def "should return Forbidden (403) when unauthorized access"() {
        when: "client sends POST request to create a new task without authorization"
        def response = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON)
        then: "response Forbidden (403) should be returned"
        response.status == 403
    }

    def "should return Forbidden (403) with WWW-authentication domain from properties when unauthorized access"() {
        when: "client sends POST request to create a new task without authorization"
        HttpResponseDecorator response = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON)
        then: "response WWW-Authenticate header with domain should be return"
        response.getFirstHeader("WWW-Authenticate").value.contains("domain=\"")
    }

    def "should create task in DB with ownerId set"() {
        given: 'user exists in the security token'
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        when: "client sends request to create new task"
        def response = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
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

    def "should retrieve all tasks belong to user from security token"() {
        given: 'user exists in security token and user has tasks'
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        client.post(path: 'tasks', body: '{"title": "taskTitle1"}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        client.post(path: 'tasks', body: '{"title": "taskTitle2", "finished": true}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: 'client sends request to get all tasks'
        def response = client.get([path: 'tasks', headers: ['Authorization': generateAuthorizationHeader(securityTokenId)]])
        then: 'response contains list of tasks'
        response.status == 200
        response.data.collect { it.title }.toSet() == ['taskTitle1', 'taskTitle2'].toSet()
    }

    def "should remove task of given id from DB and return 204"() {
        given: 'user exists in security token and user has tasks'
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        client.post(path: 'tasks', body: '{"title": "taskTitle1"}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def createTaskResponse = client.post(path: 'tasks', body: '{"title": "taskTitle2"}',
                requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: 'client sends request to delete task'
        def response = client.delete([path: "tasks/${createTaskResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)]])
        then: 'task should not exists in DB'
        tasksCollection.count(new BasicDBObject('_id', new ObjectId(createTaskResponse.data.id))) == 0
        and: 'response code is 204'
        response.status == 204
    }

    def "should return 404 when trying to delete non existing task"() {
        given: 'user exists without any tasks'
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        when: 'client sends request to delete non-existing task'
        def response = client.delete([path: 'tasks/nonExistingTaskId', headers: ['Authorization': generateAuthorizationHeader(securityTokenId)]])
        then: 'response code is 404 (not found)'
        response.status == 404
    }

    def "should return 404 when trying to remove another user's task"() {
        given: 'user A exists in security token and has tasks'
        def userASecurityId = createSecurityTokenWithUser(TEST_USER_ID)
        client.post(path: 'tasks', body: '{"title": "taskTitle1"}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(userASecurityId)])
        def createTaskResponse = client.post(path: 'tasks', body: '{"title": "taskTitle2"}',
                requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(userASecurityId)])
        and: 'user B exists without any tasks'
        def userBSecurityId = createSecurityTokenWithUser('UserB')
        when: 'client sends request as user B to delete task of user A'
        def response = client.delete([path: "tasks/${createTaskResponse.data.id}",
                                      headers: ['Authorization': generateAuthorizationHeader(userBSecurityId)]])
        then: 'response code is 404 (not found)'
        response.status == 404
    }

    def "should remove all subtasks of removed tasks when removing task"() {
        given: 'user exists in security token'
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        and: 'user has task with subtasks'
        def topLevelTaskResponse = client.post(path: 'tasks', body: '{"title": "topLevelTask"}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def subtaskResponse = client.post(path: 'tasks', body: '{"title": "subTask"}',
                requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def subSubtaskResponse = client.post(path: 'tasks', body: '{"title": "subSubTask"}',
                requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        client.post(path: "tasks/${topLevelTaskResponse.data.id}/subtasks/${subtaskResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        client.post(path: "tasks/${subtaskResponse.data.id}/subtasks/${subSubtaskResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: 'client sends request to delete top-level task'
        client.delete([path: "tasks/${topLevelTaskResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)]])
        then: 'subtask should not exists in DB'
        tasksCollection.count(new BasicDBObject('_id', new ObjectId(subtaskResponse.data.id))) == 0
        and: 'subSubtask should not exists in DB'
        tasksCollection.count(new BasicDBObject('_id', new ObjectId(subSubtaskResponse.data.id))) == 0
    }

    def "should update task with given data when client sends PUT request with task id and task in JSON"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user has created a task"
        def taskCreatedResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: "client sends PUT request to update existing task"
        def response = client.put(path: "tasks/${taskCreatedResponse.data.id}", body: UPDATED_JSON_TASK,
                requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "200 (OK) is returned in response"
        response.status == 200
        and: "all task field should be updated in response"
        response.data.title == 'This task title has been updated'
        response.data.closedDate > new DateTime().minusSeconds(10).getMillis()
        response.data.description == 'new description'
        response.data.dueDate == 1395273600000
        response.data.finished == true
        response.data.startDate == 1395273600000
        response.data.tags.size() == 1
        response.data.tags.first().name == 'next'
        and: "all fields of task should be updated in DB"
        def taskDbObject = tasksCollection.findOne(new BasicDBObject([_id: new ObjectId(taskCreatedResponse.data.id)]))
        taskDbObject.get('title') == 'This task title has been updated'
        taskDbObject.get('closed_date') > new DateTime().minusSeconds(10).toDate()
        taskDbObject.get('description') == 'new description'
        taskDbObject.get('due_date') == new Date(1395273600000)
        taskDbObject.get('finished') == true
        taskDbObject.get('start_date') == new Date(1395273600000)
        taskDbObject.get('tags').size() == 1
        tagsCollection.findOne(new BasicDBObject([_id: new ObjectId(taskDbObject.get('tags').first())])).get('name') == 'next'
    }

    def "should return task with subtasks after update"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user has task A with subtask B"
        def taskAResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def taskBResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        client.post(path: "tasks/${taskAResponse.data.id}/subtasks/${taskBResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: "client sends PUT request to update existing task A"
        def response = client.put(path: "tasks/${taskAResponse.data.id}", body: UPDATED_JSON_TASK,
                requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "task A with subtask B is returned in the response"
        response.data.subtasks.size() == 1
        response.data.subtasks.first().id == taskBResponse.data.id
    }

    def "should change all subtasks state to finished when task is updated to be finished"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user has task A with subtask B"
        def taskAResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def taskBResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        client.post(path: "tasks/${taskAResponse.data.id}/subtasks/${taskBResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: "client sends PUT request to set task A as finished"
        def response = client.put(path: "tasks/${taskAResponse.data.id}", body: UPDATED_JSON_TASK,
                requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "subtask of A (task B) in response should be set as finished"
        response.data.subtasks.first().finished == true
        and: "task B should be set in DB as finished"
        tasksCollection.findOne(new BasicDBObject('_id', new ObjectId(taskBResponse.data.id))).get("finished") == true
    }

    def "should change all parents' state to unfinished when subtask is set to NOT finished"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user has task A with subtask B - both finished"
        def taskAResponse = client.post(path: 'tasks', body: '{"title": "topLevelTask", "finished": true}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def taskBResponse = client.post(path: 'tasks', body: '{"title": "subTask", "finished": true}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        client.post(path: "tasks/${taskAResponse.data.id}/subtasks/${taskBResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: "user sends request to set task B as unfinished"
        client.put(path: "tasks/$taskBResponse.data.id", body: '{"title": "subTask", "finished": false}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "task A should be updated to be not finished as well as task B"
        tasksCollection.findOne(new BasicDBObject('_id', new ObjectId(taskAResponse.data.id))).get('finished') == false
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
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        when: 'client sends request to update non-existing task'
        def response = client.put(path: "tasks/${ObjectId.get()}", body: UPDATED_JSON_TASK,
                requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: 'response code is 404 (not found)'
        response.status == 404
    }

    def "should return 400 (bad request) when trying to update task and task not given"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user has created a task"
        def taskCreatedResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: "client sends PUT request without task given in body"
        def response = client.put(path: "tasks/${taskCreatedResponse.data.id}",
                requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then:
        response.status == 400
    }

    def "should return 404 when trying to update another user's task"() {
        given: 'user A exists in security token and has tasks'
        def userASecurityId = createSecurityTokenWithUser(TEST_USER_ID)
        def createTaskResponse = client.post(path: 'tasks', body: '{"title": "taskTitle2"}',
                requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(userASecurityId)])
        and: 'user B exists without any tasks'
        def userBSecurityId = createSecurityTokenWithUser('UserB')
        when: 'client sends request as user B to update task of user A'
        def response = client.put([path: "tasks/${createTaskResponse.data.id}", body: '{"title": "taskTitle3"}',
                                   requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(userBSecurityId)]])
        then: 'response code is 404 (not found)'
        response.status == 404
    }

    def "should return 400 (Bad Request) when trying to update task with non-existing tags"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user has created a task"
        def taskCreatedResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: "client sends PUT request to update existing task with non-existing tags"
        def response = client.put(path: "tasks/${taskCreatedResponse.data.id}", body: UPDATED_JSON_TASK_WITH_INCORRECT_TAG,
                requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
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
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user has two, top-level tasks"
        def taskBResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def taskAResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: "client sends POST request to add existing task A as subtask to other existing task B"
        def response = client.post(path: "tasks/${taskBResponse.data.id}/subtasks/${taskAResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
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
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user has three tasks, task A with subtask B and top-level task C"
        def taskAResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def taskBResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def taskCResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        client.post(path: "tasks/${taskAResponse.data.id}/subtasks/${taskBResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: "client sends POST request to add task C as subtask to task B"
        def response = client.post(path: "tasks/${taskBResponse.data.id}/subtasks/${taskCResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "task C is saved as task's B subtask in the database"
        def taskCAfterUpdate = tasksCollection.findOne(new BasicDBObject([_id: new ObjectId(taskCResponse.data.id.toString())]))
        taskCAfterUpdate.path == [taskAResponse.data.id, taskBResponse.data.id]
        and: "200 OK is returned in response"
        response.status == 200
    }

    def "should return BAD REQUEST (400) when trying to add task as subtask to itself"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user has existing task"
        def taskResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: "client sends POST request to add task as subtask to itself"
        def response = client.post(path: "tasks/${taskResponse.data.id}/subtasks/${taskResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "400 BAD REQUEST is returned in response"
        response.status == 400
    }

    def "should return BAD REQUEST (400) when trying to add task as subtask to create circular subtasks"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user has existing task A with subtask B"
        def taskAResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def taskBResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        client.post(path: "tasks/${taskAResponse.data.id}/subtasks/${taskBResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: "client sends POST request to add task A as subtask to task B"
        def response = client.post(path: "tasks/${taskBResponse.data.id}/subtasks/${taskAResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "400 BAD REQUEST is returned in response"
        response.status == 400
    }

    def "should remove task from parent task's subtasks when task is added as subtask to other task"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user has three tasks, task A with subtask B and top-level task C"
        def taskAResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def taskBResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def taskCResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        client.post(path: "tasks/${taskAResponse.data.id}/subtasks/${taskBResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: "client sends POST request to add task B as subtask to task C"
        client.post(path: "tasks/${taskCResponse.data.id}/subtasks/${taskBResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "task B is saved as task's C subtask in the database"
        def taskBAfterUpdate = tasksCollection.findOne(new BasicDBObject([_id: new ObjectId(taskBResponse.data.id.toString())]))
        taskBAfterUpdate.path == [taskCResponse.data.id]
        and: "task A should not have any subtasks"
        def response = client.get([path: 'tasks', headers: ['Authorization': generateAuthorizationHeader(securityTokenId)]])
        response.data.find { it.id == taskAResponse.data.id }.subtasks.isEmpty()
    }

    def "should return NOT FOUND 404 when trying to add task as subtask to non-existing task"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user task"
        def taskResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: "client sends POST request to add task as subtask to non-existing task"
        def response = client.post(path: "tasks/${ObjectId.get().toString()}/subtasks/${taskResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "404 NOT FOUND is returned in response"
        response.status == 404
    }

    def "should return NOT FOUND 404 when trying to add task as subtask to other user's task"() {
        given: 'user A exists with task A'
        def userASecurityId = createSecurityTokenWithUser(TEST_USER_ID)
        def taskAResponse = client.post(path: 'tasks', body: '{"title": "taskTitleA"}',
                requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(userASecurityId)])
        and: 'user B exists with task B'
        def userBSecurityId = createSecurityTokenWithUser('UserB')
        def taskBResponse = client.post(path: 'tasks', body: '{"title": "taskTitleB"}',
                requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(userBSecurityId)])
        when: "client as user B sends POST request to add task B as subtask to task A of user A"
        def response = client.post(path: "tasks/${taskAResponse.data.id}/subtasks/${taskBResponse.data.id}",
                headers: ['Authorization': generateAuthorizationHeader(userBSecurityId)])
        then: "404 NOT FOUND is returned in response"
        response.status == 404
    }

    def "should move whole subtree under new parent task when moving task with subtasks"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user has three tasks, task A with subtask B with subtask C"
        def taskAResponse = client.post(path: 'tasks', body: '{"title": "taskTitleA"}', requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def taskBResponse = client.post(path: 'tasks', body: '{"title": "taskTitleB"}', requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def taskCResponse = client.post(path: 'tasks', body: '{"title": "taskTitleC"}', requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        client.post(path: "tasks/${taskAResponse.data.id}/subtasks/${taskBResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        client.post(path: "tasks/${taskBResponse.data.id}/subtasks/${taskCResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        and: "user has top-level task E"
        def taskEResponse = client.post(path: 'tasks', body: '{"title": "taskTitleE"}', requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: "client sends POST request to add task A as subtask of task E"
        client.post(path: "tasks/${taskEResponse.data.id}/subtasks/${taskAResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "tasks A,B and C should be returned as task's E descendants"
        def response = client.get([path: 'tasks', headers: ['Authorization': generateAuthorizationHeader(securityTokenId)]])
        response.data.first().subtasks.first().id == taskAResponse.data.id
        response.data.first().subtasks.first().subtasks.first().id == taskBResponse.data.id
        response.data.first().subtasks.first().subtasks.first().subtasks.first().id == taskCResponse.data.id
    }

    def "should move task of given id to top level tasks"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user has task A with subtask B"
        def taskAResponse = client.post(path: 'tasks', body: '{"title": "taskTitleA"}', requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def taskBResponse = client.post(path: 'tasks', body: '{"title": "taskTitleB"}', requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        client.post(path: "tasks/${taskAResponse.data.id}/subtasks/${taskBResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: "client sends POST request to move task B to the top level"
        def response = client.post(path: "tasks/${taskBResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "task B should be returned"
        response.data.id == taskBResponse.data.id
        and: "task B path should be empty in DB"
        def taskBDb = tasksCollection.findOne(new BasicDBObject('_id', new ObjectId(response.data.id)), new BasicDBObject('path', true))
        taskBDb.get('path').isEmpty()
    }

    def "should remove task from parent task's subtasks when task is moved to top level"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user has three tasks, task A with subtask B with subtask task C"
        def taskAResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def taskBResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def taskCResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        client.post(path: "tasks/${taskAResponse.data.id}/subtasks/${taskBResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        client.post(path: "tasks/${taskBResponse.data.id}/subtasks/${taskCResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: "client sends POST request to move task B to top-level"
        client.post(path: "tasks/${taskBResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "task A should not have any subtasks"
        def response = client.get([path: 'tasks', headers: ['Authorization': generateAuthorizationHeader(securityTokenId)]])
        response.data.find { it.id == taskAResponse.data.id }.subtasks.isEmpty()
    }

    def "should move whole task's hierarchy from parent task's subtasks when task is moved to top level"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user has three tasks, task A with subtask B1 and B2"
        def taskAResponse = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def taskB1Response = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        def taskB2Response = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        client.post(path: "tasks/${taskAResponse.data.id}/subtasks/${taskB1Response.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        client.post(path: "tasks/${taskAResponse.data.id}/subtasks/${taskB2Response.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        and: "task B1 has subtask C1"
        def taskC1Response = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON, headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        client.post(path: "tasks/${taskB1Response.data.id}/subtasks/${taskC1Response.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        when: "client sends POST request to move task B1 to top-level"
        client.post(path: "tasks/${taskB1Response.data.id}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "task A should not have have only subtask B2"
        def response = client.get([path: 'tasks', headers: ['Authorization': generateAuthorizationHeader(securityTokenId)]])
        response.data.find { it.id == taskAResponse.data.id }.subtasks.size() == 1
        response.data.find { it.id == taskAResponse.data.id }.subtasks.first().id == taskB2Response.data.id
        and: "task C1 should have only task B1 in path in DB"
        def taskC1Db = tasksCollection.findOne(new BasicDBObject('_id', new ObjectId(taskC1Response.data.id)))
        taskC1Db.get('path').toSet() == [taskB1Response.data.id].toSet()
    }

    def "should return 404 when trying to move non existing task to top-level"() {
        given: "user is authenticated"
        def securityTokenId = createSecurityTokenWithUser(TEST_USER_ID)
        when: "client sends POST request to move non-existing task to top-level"
        def response = client.post(path: "tasks/${ObjectId.get().toString()}", headers: ['Authorization': generateAuthorizationHeader(securityTokenId)])
        then: "404 (not found) is returned in response"
        response.status == 404
    }

    def "should return 404 when trying to move task of another user to top-level"() {
        given: "user A is authenticated and has task"
        def userASecurityId = createSecurityTokenWithUser(TEST_USER_ID)
        def taskOfUserAResponse = client.post(path: 'tasks', body: '{"title": "taskTitle1"}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(userASecurityId)])
        and: 'user B exists without any tasks'
        def userBSecurityId = createSecurityTokenWithUser('UserB')
        when: "client sends POST request as user B to move task of user A to top-level"
        def response = client.post(path: "tasks/${taskOfUserAResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(userBSecurityId)])
        then: "404 (not found) is returned in response"
        response.status == 404
    }

    def "should return all unfinished tasks of user from security token and 200 response when retrieving unfinished tasks"() {
        given: "user A is authenticated"
        def userASecurityId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user A has 2 unfinished and 2 finished tasks"
        client.post(path: 'tasks', body: '{"title": "taskTitle1"}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(userASecurityId)])
        client.post(path: 'tasks', body: '{"title": "taskTitle2"}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(userASecurityId)])
        client.post(path: 'tasks', body: '{"title": "taskTitle3", "finished": true}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(userASecurityId)])
        client.post(path: 'tasks', body: '{"title": "taskTitle4", "finished": true}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(userASecurityId)])
        when: "client sends GET request as user A to get all unfinished tasks"
        def response = client.get([path: 'tasks', query: [finished: false], headers: ['Authorization': generateAuthorizationHeader(userASecurityId)]])
        then: "200 (OK) is returned is response"
        response.status == 200
        and: "response body should contain all unfinished tasks of user A"
        response.data.size() == 2
        response.data.every { it.finished == false }
        response.data.any { it.title == "taskTitle1" }
        response.data.any { it.title == "taskTitle2" }
    }

    def "should return all finished tasks of user from security token and 200 response when retrieving finished tasks"() {
        given: "user A is authenticated"
        def userASecurityId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user A has 1 unfinished and 3 finished tasks"
        client.post(path: 'tasks', body: '{"title": "taskTitle1"}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(userASecurityId)])
        client.post(path: 'tasks', body: '{"title": "taskTitle2", "finished": true}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(userASecurityId)])
        client.post(path: 'tasks', body: '{"title": "taskTitle3", "finished": true}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(userASecurityId)])
        client.post(path: 'tasks', body: '{"title": "taskTitle4", "finished": true}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(userASecurityId)])
        when: "client sends GET request as user A to get all finished tasks"
        def response = client.get([path: 'tasks', query: [finished: true], headers: ['Authorization': generateAuthorizationHeader(userASecurityId)]])
        then: "200 (OK) is returned is response"
        response.status == 200
        and: "response body should contain all finished tasks of user A"
        response.data.size() == 3
        response.data.every { it.finished == true }
        response.data.any { it.title == 'taskTitle2' }
        response.data.any { it.title == 'taskTitle3' }
        response.data.any { it.title == 'taskTitle4' }
    }

    def "should return finished tasks as flat list"() {
        given: "user A is authenticated"
        def userASecurityId = createSecurityTokenWithUser(TEST_USER_ID)
        and: "user has 1 finished task with subtask"
        def parentResponse = client.post(path: 'tasks', body: '{"title": "parent", "finished": true}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(userASecurityId)])
        def child1Response = client.post(path: 'tasks', body: '{"title": "subtask", "finished": true}', requestContentType: ContentType.JSON,
                headers: ['Authorization': generateAuthorizationHeader(userASecurityId)])
        client.post(path: "tasks/${parentResponse.data.id}/subtasks/${child1Response.data.id}", headers: ['Authorization': generateAuthorizationHeader(userASecurityId)])
        when: "client sends GET request as user A to get all finished tasks"
        def response = client.get([path: 'tasks', query: [finished: true], headers: ['Authorization': generateAuthorizationHeader(userASecurityId)]])
        then: "200 (OK) is returned is response"
        response.status == 200
        and: "response body should contain two finished tasks of user A"
        response.data.size() == 2
        response.data.every { it.finished == true }
        response.data.any { it.id == parentResponse.data.id }
        response.data.any { it.id == child1Response.data.id }
    }
}
