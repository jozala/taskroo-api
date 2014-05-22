package pl.aetas.gtweb.data

import com.mongodb.BasicDBObject
import org.bson.types.ObjectId
import pl.aetas.gtweb.domain.Tag
import pl.aetas.gtweb.domain.Task

class TagDaoTest extends DaoTestBase {

    TagDao tagDao
    TaskDao taskDao

    void setup() {
        tagDao = new TagDao(tagsCollection, new DbTagConverter(), tasksCollection)
        taskDao = new TaskDao(tasksCollection, tagDao, new DbTasksConverter())
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
        when:
        def tagExists = tagDao.exists('owner2Login', 'tag1OfOwner2')
        then:
        tagExists
    }

    def "should return false when tags does not exist for specified owner"() {
        when:
        def tagExists = tagDao.exists('owner2Login', 'nonExistingTagName')
        then:
        !tagExists
    }

    def "should throw exception when ownerId is null and check if tag exists"() {
        when:
        tagDao.exists(null, 'someTagName')
        then:
        thrown(NullPointerException)
    }

    def "should return tag when trying to find one with specified name and ownerId"() {
        given: "tag 'abc' for user with id 'userId' exists"
        def tag = new Tag(null, 'userId', 'abc', 'purple', false)
        tagDao.insert(tag)
        when:
        def foundTag = tagDao.findOne('userId', 'abc')
        then:
        foundTag.name == 'abc'
        foundTag.color == 'purple'
        !foundTag.isVisibleInWorkView()
        foundTag.id != null
    }

    def "should return null when tag not found"() {
        when:
        def foundTag = tagDao.findOne('someUser', 'nonExistingTag')
        then:
        foundTag == null
    }

    def "should throw exception when trying to find tag with null ownerId"() {
        when:
        tagDao.findOne(null, 'name')
        then:
        thrown(NullPointerException)
    }

    def "should throw exception when trying to find tag and null name given"() {
        when:
        tagDao.findOne('someUserId', null)
        then:
        thrown(NullPointerException)
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

    def "should remove tag from DB when asked"() {
        given:
        def tag = new Tag(null, 'ownerId', 'someTagToRemove', '#123456', true)
        tagDao.insert(tag)
        when:
        tagDao.remove('ownerId', 'someTagToRemove')
        then:
        tagsCollection.count(new BasicDBObject([owner_id:'ownerId', name:'someTagToRemove'])) == 0
    }

    def "should throw exception when trying to remove non-existing tag"() {
        when:
        tagDao.remove('ownerId', 'nonExistingTagName')
        then:
        thrown(NonExistingResourceOperationException)
    }

    def "should throw exception when trying to remove tag with ownerId as null"() {
        when:
        tagDao.remove(null, 'nonExistingTagName')
        then:
        thrown(NullPointerException)
    }

    def "should throw exception when trying to remove tag with tag's name as null"() {
        when:
        tagDao.remove('ownerId', null)
        then:
        thrown(NullPointerException)
    }

    def "should remove given tag from all tasks when removing tag"() {
        given:
        def tag1 = new Tag(null, 'ownerId', 'one', '#123456', true)
        def tag2 = new Tag(null, 'ownerId', 'two', '#654321', true)
        def tag1AfterInsert = tagDao.insert(tag1)
        tagDao.insert(tag2)
        def task = new Task.TaskBuilder().setOwnerId('ownerId').setTitle('taskTitle').addTag(tag1).addTag(tag2).build()
        taskDao.insert(task)
        when:
        tagDao.remove('ownerId', 'two')
        then:
        def tasksTagsAfterRemoval = tasksCollection.findOne(new BasicDBObject([_id: new ObjectId(task.getId())])).get('tags')
        tasksTagsAfterRemoval.size() == 1
        tasksTagsAfterRemoval.first() == tag1AfterInsert.getId()
    }

    def "should update tag with given name in DB when updating tag"() {
        given:
        def tag = new Tag(null, 'ownerId', 'one', '#123456', true)
        def tagAfterInsert = tagDao.insert(tag)
        when:
        tagDao.update('ownerId', 'one', new Tag(null, 'ownerId', 'two', '#654321', false))
        then:
        def tagAfterUpdate = tagDao.getAllTagsByOwnerId('ownerId').first()
        tagAfterUpdate.id == tagAfterInsert.id
        tagAfterUpdate.name == 'two'
        tagAfterUpdate.color == '#654321'
        !tagAfterUpdate.isVisibleInWorkView()
    }

    def "should return updated tag when updating tag"() {
        given:
        def tag = new Tag(null, 'ownerId', 'one', '#123456', true)
        def tagAfterInsert = tagDao.insert(tag)
        when:
        def updatedTag = tagDao.update('ownerId', 'one', new Tag(null, 'ownerId', 'two', '#654321', false))
        then:
        updatedTag.id == tagAfterInsert.id
        updatedTag.name == 'two'
        updatedTag.color == '#654321'
        !updatedTag.isVisibleInWorkView()
    }

    def "should throw exception when ownerId is not given for update"() {
        when:
        tagDao.update(null, 'one', new Tag(null, 'ownerId', 'two', '#654321', false))
        then:
        thrown(NullPointerException)
    }

    def "should throw exception when tag name is not given for update"() {
        when:
        tagDao.update('ownerId', null, new Tag(null, 'ownerId', 'two', '#654321', false))
        then:
        thrown(NullPointerException)
    }

    def "should throw exception when tag is not given for update"() {
        when:
        tagDao.update('ownerId', 'one', null)
        then:
        thrown(NullPointerException)
    }

    def "should throw exception when tag owner in tag object is different than given"() {
        when:
        tagDao.update('ownerId2', 'one', new Tag(null, 'ownerId', 'two', '#654321', false))
        then:
        thrown(IllegalArgumentException)
    }

    def "should throw exception when trying to update non-existing tag"() {
        when:
        tagDao.update('ownerId', 'nonExistingTag', new Tag(null, 'ownerId', 'two', '#654321', false))
        then:
        thrown(NonExistingResourceOperationException)
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
