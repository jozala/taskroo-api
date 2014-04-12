package pl.aetas.gtweb.integration.data
import com.mongodb.BasicDBObject
import pl.aetas.gtweb.data.TagDao
import pl.aetas.gtweb.domain.Tag

class TagDaoTest extends IntegrationTestBase {

    TagDao tagDao;

    void setup() {
        tagDao = new TagDao(tagsCollection)
        tagsCollection.drop()
        prepareTestData()
    }

    void cleanup() {
        tagsCollection.drop()
    }

    def "should retrieve tags for specified user"() {
        when:
        def tags = tagDao.getAllTagsByOwnerId('owner1Login')
        then:
        tags.every { it.name.contains('OfOwner1')}
    }

    def "should map object from db to tag obejct"() {
        when:
        def tags = tagDao.getAllTagsByOwnerId('owner2Login')
        def tag = tags.first()
        then:
        tag.name == 'tag1OfOwner2'
        tag.ownerId == 'owner2Login'
        tag.color == 'pink'
        tag.isVisibleInWorkView()
    }

    def "should return true when tags exists for specified owner"() {
        given:
        def tag = new Tag('owner2Login', 'tag1OfOwner2', 'pink', true)
        when:
        def tagExists = tagDao.exists(tag)
        then:
        tagExists
    }

    def "should return false when tags does not exist for specified owner"() {
        given:
        def tag = new Tag('owner2Login', 'nonExistingTagName', 'black', true)
        when:
        def tagExists = tagDao.exists(tag)
        then:
        !tagExists

    }

    private void prepareTestData() {
        List<Map> tagMaps = []
        tagMaps << [_id: [name: 'tag1OfOwner1', owner_id: 'owner1Login'], color: 'blue', visibleInWorkView: true]
        tagMaps << [_id: [name: 'tag2OfOwner1', owner_id: 'owner1Login'], color: 'white', visibleInWorkView: false]
        tagMaps << [_id: [name: 'tag1OfOwner2', owner_id: 'owner2Login'], color: 'pink', visibleInWorkView: true]
        tagMaps << [_id: [name: 'tag3OfOwner1', owner_id: 'owner1Login'], color: 'black', visibleInWorkView: false]

        tagMaps.each { tagsCollection.insert(new BasicDBObject(it)) }
    }
}
