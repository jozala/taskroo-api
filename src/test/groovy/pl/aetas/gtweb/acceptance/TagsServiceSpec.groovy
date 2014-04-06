package pl.aetas.gtweb.acceptance
import com.mongodb.BasicDBObject
import groovy.json.JsonSlurper
import org.apache.http.HttpResponse
import org.apache.http.client.fluent.Request

class TagsServiceSpec extends AcceptanceTestBase {

    def cleanup() {
        tagsCollection.drop()
    }

    def "should return all tags of the specified user in the JSON format and status code 200"() {
        given: "tags of the specified user exists in the DB"
            prepareTestData()
            def sessionId = userIsLoggedIn("owner1Login")
        when: "client sends POST request to /task to create a new task"
            HttpResponse response = Request.Get(APP_URL + "/tags/all").addHeader("session-id", sessionId).execute().returnResponse()
        then: "Response should be 200 and contains all tags of specified user"
        println response.entity.content.toString()
            assert response.statusLine.statusCode == 200
            assert new JsonSlurper().parseText(response.entity.content.getText()) ==
                    [[ownerId: 'owner1Login', name: 'tag1OfOwner1', color: 'blue', visibleInWorkView: true],
                     [ownerId: 'owner1Login', name: 'tag2OfOwner1', color: 'white', visibleInWorkView: false],
                     [ownerId: 'owner1Login', name: 'tag3OfOwner1', color: 'black', visibleInWorkView: false]]
    }



    private static void prepareTestData() {
        List<Map> tagMaps = []
        tagMaps << [_id: [name: 'tag1OfOwner1', owner_id: 'owner1Login'], color: 'blue', visibleInWorkView: true]
        tagMaps << [_id: [name: 'tag2OfOwner1', owner_id: 'owner1Login'], color: 'white', visibleInWorkView: false]
        tagMaps << [_id: [name: 'tag1OfOwner2', owner_id: 'owner2Login'], color: 'pink', visibleInWorkView: true]
        tagMaps << [_id: [name: 'tag3OfOwner1', owner_id: 'owner1Login'], color: 'black', visibleInWorkView: false]

        tagMaps.each { tagsCollection.insert(new BasicDBObject(it)) }
    }

    private String userIsLoggedIn(String username) {
        def sessionMap = [user_id: username, active: true, secure: true]
        def sessionObject = new BasicDBObject(sessionMap)
        sessionCollection.insert(sessionObject)
        return sessionObject.get('_id')
    }


}
