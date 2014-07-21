package com.taskroo.data

import com.mongodb.BasicDBObject
import org.bson.types.ObjectId
import com.taskroo.domain.Tag
import com.taskroo.domain.Task

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
        tasksCollection.drop()
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

    def "should get number of unfinished tasks for each tag when getting all tags"() {
        given:
        def tag1 = new Tag(null, 'ownerId', 'one', '#123456', true)
        def tag1AfterInsert = tagDao.insert(tag1)
        def task1 = new Task.TaskBuilder().setOwnerId('ownerId').setTitle('taskTitle').addTag(tag1AfterInsert).build()
        def task2 = new Task.TaskBuilder().setOwnerId('ownerId').setTitle('taskTitle').addTag(tag1AfterInsert)
                .setFinished(true).build()
        def task3 = new Task.TaskBuilder().setOwnerId('ownerId').setTitle('taskTitle').addTag(tag1AfterInsert).build()
        taskDao.insert(task1)
        taskDao.insert(task2)
        taskDao.insert(task3)
        when:
        def tagsFromDao = tagDao.getAllTagsByOwnerId('ownerId')
        then:
        tagsFromDao.first().size == 2
    }

    def "should return tag when trying to find one with specified name and ownerId"() {
        given: "tag 'abc' for user with id 'userId' exists"
        def tag = new Tag(null, 'userId', 'abc', 'purple', false)
        tagDao.insert(tag)
        when:
        def foundTag = tagDao.findByName('userId', 'abc')
        then:
        foundTag.name == 'abc'
        foundTag.color == 'purple'
        !foundTag.isVisibleInWorkView()
        foundTag.id != null
    }

    def "should return null when tag not found"() {
        when:
        def foundTag = tagDao.findByName('someUser', 'nonExistingTag')
        then:
        foundTag == null
    }

    def "should throw exception when trying to find tag with null ownerId"() {
        when:
        tagDao.findByName(null, 'name')
        then:
        thrown(NullPointerException)
    }

    def "should throw exception when trying to find tag and null name given"() {
        when:
        tagDao.findByName('someUserId', null)
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

    def "should remove tag of given id from DB when asked"() {
        given:
        def tag = new Tag(null, 'ownerId', 'someTagToRemove', '#123456', true)
        tag = tagDao.insert(tag)
        when:
        tagDao.remove('ownerId', tag.id)
        then:
        tagsCollection.count(new BasicDBObject([owner_id:'ownerId', name:'someTagToRemove'])) == 0
    }

    def "should throw exception when trying to remove non-existing tag"() {
        when:
        tagDao.remove('ownerId', ObjectId.get().toString())
        then:
        thrown(NonExistingResourceOperationException)
    }

    def "should throw exception when trying to remove tag with ownerId as null"() {
        when:
        tagDao.remove(null, ObjectId.get().toString())
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
        def tag2AfterInsert = tagDao.insert(tag2)
        def task = new Task.TaskBuilder().setOwnerId('ownerId').setTitle('taskTitle').addTag(tag1).addTag(tag2).build()
        taskDao.insert(task)
        when:
        tagDao.remove('ownerId', tag2AfterInsert.id)
        then:
        def tasksTagsAfterRemoval = tasksCollection.findOne(new BasicDBObject([_id: new ObjectId(task.getId())])).get('tags')
        tasksTagsAfterRemoval.size() == 1
        tasksTagsAfterRemoval.first() == tag1AfterInsert.getId()
    }

    def "should update tag with given id in DB when updating tag"() {
        given:
        def tag = new Tag(null, 'ownerId', 'one', '#123456', true)
        def tagAfterInsert = tagDao.insert(tag)
        when:
        tagDao.update('ownerId', tagAfterInsert.id, new Tag(null, 'ownerId', 'two', '#654321', false))
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
        def updatedTag = tagDao.update('ownerId', tagAfterInsert.id, new Tag(null, 'ownerId', 'two', '#654321', false))
        then:
        updatedTag.id == tagAfterInsert.id
        updatedTag.name == 'two'
        updatedTag.color == '#654321'
        !updatedTag.isVisibleInWorkView()
    }

    def "should throw exception when ownerId is not given for update"() {
        when:
        tagDao.update(null, ObjectId.get().toString(), new Tag(null, 'ownerId', 'two', '#654321', false))
        then:
        thrown(NullPointerException)
    }

    def "should throw exception when tag id is not given for update"() {
        when:
        tagDao.update('ownerId', null, new Tag(null, 'ownerId', 'two', '#654321', false))
        then:
        thrown(NullPointerException)
    }

    def "should throw exception when tag is not given for update"() {
        given:
        def tag = new Tag(null, 'ownerId', 'one', '#123456', true)
        def tagAfterInsert = tagDao.insert(tag)
        when:
        tagDao.update('ownerId', tagAfterInsert.id, null)
        then:
        thrown(NullPointerException)
    }

    def "should throw exception when tag owner in tag object is different than given"() {
        given:
        def tag = tagDao.findByName('owner1Login', 'tag2OfOwner1')
        when:
        tagDao.update('owner1Login', tag.id, new Tag(null, 'owner2Login', 'two', '#654321', false))
        then:
        thrown(IllegalArgumentException)
    }

    def "should throw exception when trying to update non-existing tag"() {
        when:
        tagDao.update('ownerId', ObjectId.get().toString(), new Tag(null, 'ownerId', 'two', '#654321', false))
        then:
        thrown(NonExistingResourceOperationException)
    }

    def "should return set of ids of non existing tags"() {
        def existingTagsIds = tagsCollection.find(new BasicDBObject(), new BasicDBObject([_id: true]))
                .toArray().collect {it._id.toString()}
        def nonExistingTagsIds = [ObjectId.get().toString(), ObjectId.get().toString(), ObjectId.get().toString()].toSet()
        when:
        def result = tagDao.findNonExistingTags((existingTagsIds + nonExistingTagsIds).toSet())
        then:
        result == nonExistingTagsIds
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
