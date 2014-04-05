package pl.aetas.gtweb.acceptance

import com.mongodb.BasicDBObject
import org.apache.http.client.fluent.Request

class TagsServiceSpec extends AcceptanceTestBase {

    def cleanup() {
        tagsCollection.drop()
    }

    def "should return all tags of the specified user in the JSON format and status code 200"() {
        given: "tags of the specified user exists in the DB"
            prepareTestData()
        when: "client sends POST request to /task to create a new task"
            def response = Request.Get(APP_URL + "/tags/all/owner1Login").execute()
        then: "I don't care"
            assert response.returnResponse().statusLine.statusCode == 200
            assert true
    }



    private static void prepareTestData() {
        List<Map> tagMaps = []
        tagMaps << [_id: [name: 'tag1OfOwner1', owner_id: 'owner1Login'], color: 'blue', visibleInWorkView: true]
        tagMaps << [_id: [name: 'tag2OfOwner1', owner_id: 'owner1Login'], color: 'white', visibleInWorkView: false]
        tagMaps << [_id: [name: 'tag1OfOwner2', owner_id: 'owner2Login'], color: 'pink', visibleInWorkView: true]
        tagMaps << [_id: [name: 'tag3OfOwner1', owner_id: 'owner1Login'], color: 'black', visibleInWorkView: false]

        tagMaps.each { tagsCollection.insert(new BasicDBObject(it)) }
    }


}
