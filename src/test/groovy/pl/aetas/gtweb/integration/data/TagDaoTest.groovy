package pl.aetas.gtweb.integration.data
import com.mongodb.BasicDBObject
import pl.aetas.gtweb.data.DbTagConverter
import pl.aetas.gtweb.data.TagDao
import pl.aetas.gtweb.data.UnsupportedDataOperationException
import pl.aetas.gtweb.domain.Tag

class TagDaoTest extends IntegrationTestBase {

    TagDao tagDao;

    void setup() {
        tagDao = new TagDao(tagsCollection, new DbTagConverter())
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

    def "should map object from db to tag object"() {
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
        def tag = new Tag('id', 'owner2Login', 'tag1OfOwner2', 'pink', true)
        when:
        def tagExists = tagDao.exists(tag)
        then:
        tagExists
    }

    def "should return false when tags does not exist for specified owner"() {
        given:
        def tag = new Tag('id', 'owner2Login', 'nonExistingTagName', 'black', true)
        when:
        def tagExists = tagDao.exists(tag)
        then:
        !tagExists
    }

    def "should throw exception when ownerId of tag is not set and check if tag exists"() {
        given:
        def tag = new Tag(null, null, 'newTag', 'black', true)
        when:
        tagDao.exists(tag)
        then:
        thrown(UnsupportedDataOperationException)
    }

    def "should insert tag into DB when inserting tag"() {
        given:
        def tag = new Tag(null, 'owner1Login', 'newTag', 'black', true)
        when:
        tagDao.insert(tag)
        then:
        tagsCollection.count(new BasicDBObject('owner_id','owner1Login').append('name', 'newTag')) == 1
    }

    def "should return tag with id set when inserting tag"() {
        given:
        def tag = new Tag(null, 'owner1Login', 'newTag', 'black', true)
        when:
        def tagAfterInsert = tagDao.insert(tag)
        then:
        tagAfterInsert.id != null
        tagAfterInsert.name == 'newTag'
    }

    def "should throw exception when ownerId of the tag to insert is not set"() {
        given:
        def tag = new Tag(null, null, 'newTag', 'black', true)
        when:
        tagDao.insert(tag)
        then:
        thrown(UnsupportedDataOperationException)
    }

    private void prepareTestData() {
        List<Map> tagMaps = []
        tagMaps << [name: 'tag1OfOwner1', owner_id: 'owner1Login', color: 'blue', visible_in_workview: true]
        tagMaps << [name: 'tag2OfOwner1', owner_id: 'owner1Login', color: 'white', visible_in_workview: false]
        tagMaps << [name: 'tag1OfOwner2', owner_id: 'owner2Login', color: 'pink', visible_in_workview: true]
        tagMaps << [name: 'tag3OfOwner1', owner_id: 'owner1Login', color: 'black', visible_in_workview: false]

        tagMaps.each { tagsCollection.insert(new BasicDBObject(it)) }
    }
}
