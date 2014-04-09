package pl.aetas.gtweb.integration.data

import com.mongodb.BasicDBObject
import com.mongodb.DB
import com.mongodb.DBCollection
import pl.aetas.gtweb.data.TagDao
import pl.aetas.gtweb.domain.Tag
import pl.aetas.gtweb.mongo.MongoConnector

class TagDaoTest extends GroovyTestCase {

    // SUT
    TagDao tagDao;

    DBCollection tagsCollection;

    TagDaoTest() {
        DB db = new MongoConnector("mongodb://localhost").getDatabase("gtweb-integration-tests-db")
        tagsCollection = db.getCollection("tags")
        tagDao = new TagDao(tagsCollection)
    }

    @Override
    void setUp() {
        prepareTestData()
    }

    @Override
    void tearDown() {
        tagsCollection.drop()
    }

    void testShouldRetrieveTagsForSpecifiedUser() {
        List<Tag> tags = tagDao.getAllTagsByOwnerId('owner1Login')
        assert tags.every { it.getName().contains('OfOwner1')}
    }

    void testShouldMapObjectFromDbToTagObject() {
        def tags = tagDao.getAllTagsByOwnerId('owner2Login')
        def tag = tags.first();
        assert tag.name == 'tag1OfOwner2'
        assert tag.ownerId == 'owner2Login'
        assert tag.color == 'pink'
        assert tag.isVisibleInWorkView()
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
