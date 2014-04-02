package pl.aetas.gtweb.integration.data
import com.mongodb.DB
import com.mongodb.DBCollection
import pl.aetas.gtweb.data.MongoConnector
import pl.aetas.gtweb.data.TagDao

class TagDaoTest extends GroovyTestCase {

    // SUT
    TagDao tagDao;

    DBCollection tagsCollection;

    TagDaoTest() {
        DB db = new MongoConnector().getDatabase();
        tagsCollection = db.getCollection("tags")
        tagDao = new TagDao(tagsCollection);
    }

    @Override
    void setUp() {
        prepareTestData();
    }

    @Override
    void tearDown() {
        removeTestData();
    }

    void removeTestData() {

    }

    void testMapTagsFromDbToTagsObjects() {
        tagDao.getAllTagsByOwnerId(123L);
    }


    private void prepareTestData() {
        tagsCollection.insert()
    }
}
