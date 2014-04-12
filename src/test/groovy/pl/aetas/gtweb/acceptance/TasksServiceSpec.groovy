package pl.aetas.gtweb.acceptance
import com.mongodb.BasicDBObject
import com.mongodb.QueryBuilder
import groovyx.net.http.ContentType
import org.bson.types.ObjectId

class TasksServiceSpec extends AcceptanceTestBase {

    def setup() {
        createTagInDb(TEST_USER_ID, 'planned', '#10f028', false)
    }

    void cleanup() {
        tagsCollection.remove(QueryBuilder.start('_id.owner_id').is(TEST_USER_ID).get())
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
        response.data.tags.first().id != 24
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
        when:
        def response = client.post(path: 'tasks', body: JSON_TASK, requestContentType: ContentType.JSON,
                headers: ['Session-Id': sessionId])
        then:
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
        tagsCollection.insert(new BasicDBObject([_id: [name: name, owner_id: ownerId, ], color: color, visible_in_work_view: visibleInWorkView]))
    }
}
