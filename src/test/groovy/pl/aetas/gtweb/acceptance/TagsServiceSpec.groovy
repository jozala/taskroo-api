package pl.aetas.gtweb.acceptance
import com.mongodb.BasicDBObject
import com.mongodb.QueryBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import org.bson.types.ObjectId
import pl.aetas.testing.RunJetty

@RunJetty
class TagsServiceSpec extends AcceptanceTestBase {

    def setupSpec() {
        cleanup()
    }

    def cleanup() {
        tasksCollection.remove(QueryBuilder.start('owner_id').is(TEST_USER_ID).get())
        tagsCollection.remove(QueryBuilder.start('owner_id').in(['owner1Login', 'owner2Login', 'owner3Login', TEST_USER_ID]).get())
        sessionCollection.remove(QueryBuilder.start('user_id').in(['owner1Login', TEST_USER_ID]).get())
    }

    def "should return all tags of the specified user in the JSON format and status code 200"() {
        given: "tags of the specified user exists in the DB"
        prepareTestData()
        def sessionId = createSessionWithUser('owner1Login')
        when: "client sends POST request to /task to create a new task"
        HttpResponseDecorator response = client.get(path: 'tags', headers: ['Session-Id': sessionId])
        then: "Response should be 200 and contains all tags of specified user"
        response.status == 200
        response.data.collect { it.remove('id'); return it } ==
                [[ownerId: 'owner1Login', name: 'tag1OfOwner1', color: 'blue', visibleInWorkView: true],
                 [ownerId: 'owner1Login', name: 'tag2OfOwner1', color: 'white', visibleInWorkView: false],
                 [ownerId: 'owner1Login', name: 'tag3OfOwner1', color: 'black', visibleInWorkView: false]]
    }

    def "should create new tag when client sends POST request with tag"() {
        given: "Tag in JSON format"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        def tag = '{"name": "newTag", "color": "gray", "visibleInWorkView": true}'
        when: "Client sends POST request with tag in JSON format"
        def response = client.post(path: 'tags', headers: ['Session-Id': sessionId], body: tag, requestContentType: ContentType.JSON)
        then: "tag is created in the db"
        tagsCollection.count(new BasicDBObject('name','newTag').append('owner_id', TEST_USER_ID)) == 1
        and: "tag with id is returned in 201 response"
        response.status == 201
        response.data.id != null
        response.data.name == 'newTag'
    }

    def "should return 200 and tag data when trying to create tag with existing name"() {
        given: "Tag 'abc' exists"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        def tag = '{"name": "abc", "color": "gray", "visibleInWorkView": true}'
        def previousResponse = client.post(path: 'tags', headers: ['Session-Id': sessionId], body: tag, requestContentType: ContentType.JSON)
        when: "client sends request to create 'abc' tag"
        def response = client.post(path: 'tags', headers: ['Session-Id': sessionId], body: tag, requestContentType: ContentType.JSON)
        then: "response is 200"
        response.status == 200
        and: "id of the tag in the response is the same as existing tag"
        response.data.id == previousResponse.data.id
    }

    def "should remove tag from DB when client sends DELETE request with tag"() {
        given: "Tag exists in DB"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        def tag = '{"name": "abc", "color": "gray", "visibleInWorkView": true}'
        def existingTagResponse = client.post(path: 'tags', headers: ['Session-Id': sessionId], body: tag, requestContentType: ContentType.JSON)
        when: "client sends DELETE request with tag id"
        def response = client.delete(path: "tags/${existingTagResponse.data.name}", headers: ['Session-Id': sessionId])
        then: "tag is removed from DB"
        tagsCollection.count(new BasicDBObject('name','newTag').append('owner_id', TEST_USER_ID)) == 0
        and: "response status is 204 (no content)"
        response.status == 204
    }

    def "should return 400 (bad request) when trying to remove non-existing tag"() {
        when: "client sends DELETE request with non-existing tag id"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        def response = client.delete(path: 'tags/nonExisingTagId', headers: ['Session-Id': sessionId])
        then: "response is 400 (bad request)"
        response.status == 400
    }

    def "should remove tag from all tasks when removing tag"() {
        given: "Task exists with tag 'abc'"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        def tag = '{"name": "abc", "color": "gray", "visibleInWorkView": true}'
        def existingTagResponse = client.post(path: 'tags', headers: ['Session-Id': sessionId], body: tag, requestContentType: ContentType.JSON)
        and: "task exists with tag 'abc'"
        def existingTaskResponse = client.post(path: 'tasks', headers: ['Session-Id': sessionId], body: '{"title": "taskTitle1", "tags":[{"name":"abc"}]}', requestContentType: ContentType.JSON)
        when: "client sends DELETE request to remove tag 'abc'"
        client.delete(path: "tags/${existingTagResponse.data.name}", headers: ['Session-Id': sessionId])
        then: "tag is removed from the task's tags"
        def tagsDbFromTask = tasksCollection.findOne(new BasicDBObject([_id: new ObjectId(existingTaskResponse.data.id)])).get('tags')
        tagsDbFromTask.isEmpty()
    }

    private static void prepareTestData() {
        List<Map> tagMaps = []
        tagMaps << [name: 'tag1OfOwner1', owner_id: 'owner1Login', color: 'blue', visible_in_workview: true]
        tagMaps << [name: 'tag2OfOwner1', owner_id: 'owner1Login', color: 'white', visible_in_workview: false]
        tagMaps << [name: 'tag1OfOwner2', owner_id: 'owner2Login', color: 'pink', visible_in_workview: true]
        tagMaps << [name: 'tag3OfOwner1', owner_id: 'owner1Login', color: 'black', visible_in_workview: false]

        tagMaps.each { tagsCollection.insert(new BasicDBObject(it)) }
    }


}
