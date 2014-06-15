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
        HttpResponseDecorator response = client.get(path: 'tags', headers: ['Authorization': generateAuthorizationHeader(sessionId)])
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
        def response = client.post(path: 'tags', headers: ['Authorization': generateAuthorizationHeader(sessionId)], body: tag, requestContentType: ContentType.JSON)
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
        def previousResponse = client.post(path: 'tags', headers: ['Authorization': generateAuthorizationHeader(sessionId)], body: tag, requestContentType: ContentType.JSON)
        when: "client sends request to create 'abc' tag"
        def response = client.post(path: 'tags', headers: ['Authorization': generateAuthorizationHeader(sessionId)], body: tag, requestContentType: ContentType.JSON)
        then: "response is 200"
        response.status == 200
        and: "id of the tag in the response is the same as existing tag"
        response.data.id == previousResponse.data.id
    }

    def "should remove tag from DB when client sends DELETE request with tag ID"() {
        given: "Tag exists in DB"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        def tag = '{"name": "abc", "color": "gray", "visibleInWorkView": true}'
        def existingTagResponse = client.post(path: 'tags', headers: ['Authorization': generateAuthorizationHeader(sessionId)], body: tag, requestContentType: ContentType.JSON)
        when: "client sends DELETE request with tag ID"
        def response = client.delete(path: "tags/${existingTagResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(sessionId)])
        then: "tag is removed from DB"
        tagsCollection.count(new BasicDBObject('name','newTag').append('owner_id', TEST_USER_ID)) == 0
        and: "response status is 204 (no content)"
        response.status == 204
    }

    def "should return 404 (not found) when trying to remove non-existing tag"() {
        when: "client sends DELETE request with non-existing tag id"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        def response = client.delete(path: "tags/${ObjectId.get().toString()}", headers: ['Authorization': generateAuthorizationHeader(sessionId)])
        then: "response is 404 (not found)"
        response.status == 404
    }

    def "should remove tag from all tasks when removing tag"() {
        given: "Task exists with tag 'abc'"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        def tag = '{"name": "abc", "color": "gray", "visibleInWorkView": true}'
        def existingTagResponse = client.post(path: 'tags', headers: ['Authorization': generateAuthorizationHeader(sessionId)], body: tag, requestContentType: ContentType.JSON)
        and: "task exists with tag 'abc'"
        def existingTaskResponse = client.post(path: 'tasks', headers: ['Authorization': generateAuthorizationHeader(sessionId)], body: '{"title": "taskTitle1", "tags":[{"name":"abc"}]}', requestContentType: ContentType.JSON)
        when: "client sends DELETE request to remove tag 'abc'"
        client.delete(path: "tags/${existingTagResponse.data.id}", headers: ['Authorization': generateAuthorizationHeader(sessionId)])
        then: "tag is removed from the task's tags"
        def tagsDbFromTask = tasksCollection.findOne(new BasicDBObject([_id: new ObjectId(existingTaskResponse.data.id)])).get('tags')
        tagsDbFromTask.isEmpty()
    }

    def "should update tag fields and return updated tag when update request received"() {
        given: "Tag 'abc' exists"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        def tag = '{"name": "abc", "color": "gray", "visibleInWorkView": true}'
        def existingTagResponse = client.post(path: 'tags', headers: ['Authorization': generateAuthorizationHeader(sessionId)], body: tag, requestContentType: ContentType.JSON)
        when: "client sends PUT request to update tag 'abc' with new name, color and changed visibleInWorView value"
        def updatedTag = '{"name": "notAbcAnyMore", "color": "violet", "visibleInWorkView": false}'
        def tagAfterUpdateResponse = client.put(path: "tags/$existingTagResponse.data.id", headers: ['Authorization': generateAuthorizationHeader(sessionId)], body: updatedTag, requestContentType: ContentType.JSON)
        then: 'response code should be 200 OK'
        tagAfterUpdateResponse.status == 200
        and: "response should contain updated tag"
        tagAfterUpdateResponse.data.name == 'notAbcAnyMore'
        tagAfterUpdateResponse.data.color == 'violet'
        tagAfterUpdateResponse.data.visibleInWorkView == false
        and: "tag should be updated with new values in DB"
        def tagDbObject = tagsCollection.findOne(new BasicDBObject('_id', new ObjectId(existingTagResponse.data.id)))
        tagDbObject.name == 'notAbcAnyMore'
        tagDbObject.color == 'violet'
        tagDbObject.visible_in_workview == false
    }

    def "should return 404 when trying to update non-existing tag"() {
        when: "client sends PUT request to update non-existing tag"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        def someTag = '{"name": "nonExistingTag", "color": "violet", "visibleInWorkView": false}'
        def response = client.put(path: "tags/${ObjectId.get().toString()}", headers: ['Authorization': generateAuthorizationHeader(sessionId)], body: someTag, requestContentType: ContentType.JSON)
        then: "service should respond with 404 (Not Found)"
        response.status == 404
    }

    private static void prepareTestData() {
        List<Map> tagMaps = []
        tagMaps << [name: 'tag1OfOwner1', owner_id: 'owner1Login', color: 'blue', visible_in_workview: true]
        tagMaps << [name: 'tag2OfOwner1', owner_id: 'owner1Login', color: 'white', visible_in_workview: false]
        tagMaps << [name: 'tag1OfOwner2', owner_id: 'owner2Login', color: 'pink', visible_in_workview: true]
        tagMaps << [name: 'tag3OfOwner1', owner_id: 'owner1Login', color: 'black', visible_in_workview: false]

        tagMaps.each { tagsCollection.insert(new BasicDBObject(it)) }
    }

    def "should return 400 (bad request) when trying to update task to have same name as other already existing tag"() {
        given: "Tags 'abc' and 'cde' exists"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        def tagAbc = '{"name": "abc", "color": "gray", "visibleInWorkView": true}'
        def tagCde = '{"name": "cde", "color": "pink", "visibleInWorkView": true}'
        client.post(path: 'tags', headers: ['Authorization': generateAuthorizationHeader(sessionId)], body: tagAbc, requestContentType: ContentType.JSON)
        def cdeResponse = client.post(path: 'tags', headers: ['Authorization': generateAuthorizationHeader(sessionId)], body: tagCde, requestContentType: ContentType.JSON)
        when: "client sends request to update 'cde' tag's name to 'abc'"
        def response = client.put(path: "tags/$cdeResponse.data.id", headers: ['Authorization': generateAuthorizationHeader(sessionId)], body: tagAbc, requestContentType: ContentType.JSON)
        then: "response is 400"
        response.status == 400
    }

    def "should change colour of tag when request send with the same name but different colour"() {
        given: "Tag 'abc' exists"
        def sessionId = createSessionWithUser(TEST_USER_ID)
        def tag = '{"name": "abc", "color": "gray", "visibleInWorkView": true}'
        def existingTagResponse = client.post(path: 'tags', headers: ['Authorization': generateAuthorizationHeader(sessionId)], body: tag, requestContentType: ContentType.JSON)
        when: "client sends PUT request to update tag 'abc' with new color"
        def updatedTag = '{"name": "abc", "color": "violet", "visibleInWorkView": true}'
        def tagAfterUpdateResponse = client.put(path: "tags/$existingTagResponse.data.id", headers: ['Authorization': generateAuthorizationHeader(sessionId)], body: updatedTag, requestContentType: ContentType.JSON)
        then: 'response code should be 200 OK'
        tagAfterUpdateResponse.status == 200
        and: "response should contain updated tag"
        tagAfterUpdateResponse.data.name == 'abc'
        tagAfterUpdateResponse.data.color == 'violet'
        tagAfterUpdateResponse.data.visibleInWorkView == true
        and: "tag's colour should be updated in DB"
        def tagDbObject = tagsCollection.findOne(new BasicDBObject('_id', new ObjectId(existingTagResponse.data.id)))
        tagDbObject.color == 'violet'
    }
}
