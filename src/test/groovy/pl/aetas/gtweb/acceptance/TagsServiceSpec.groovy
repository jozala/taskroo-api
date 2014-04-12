package pl.aetas.gtweb.acceptance
import com.mongodb.BasicDBObject
import com.mongodb.QueryBuilder
import groovyx.net.http.HttpResponseDecorator

class TagsServiceSpec extends AcceptanceTestBase {

    def cleanup() {
        tagsCollection.remove(QueryBuilder.start('_id.owner_id').in(['owner1Login', 'owner2Login', 'owner3Login']).get())
        sessionCollection.remove(QueryBuilder.start('user_id').is('owner1Login').get())
    }

    def "should return all tags of the specified user in the JSON format and status code 200"() {
        given: "tags of the specified user exists in the DB"
        prepareTestData()
        def sessionId = createSessionWithUser('owner1Login')
        when: "client sends POST request to /task to create a new task"
        HttpResponseDecorator response = client.get(path: 'tags', headers: ['Session-Id': sessionId])
        then: "Response should be 200 and contains all tags of specified user"
        response.status == 200
        response.data ==
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


}
