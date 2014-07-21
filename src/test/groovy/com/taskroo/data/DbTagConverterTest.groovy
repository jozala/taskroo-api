package com.taskroo.data

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import spock.lang.Specification

class DbTagConverterTest extends Specification {

    DbTagConverter dbTagConverter

    void setup() {
        dbTagConverter = new DbTagConverter();
    }

    def "should convert db object to tag"() {
        given:
        def dbTag = new BasicDBObject([_id: '123', name: 'tagName', owner_id: 'tagOwner', color: 'blue', visible_in_workview: true])
        when:
        def tag = dbTagConverter.convertDbObjectToTag(dbTag)
        then:
        tag.id == '123'
        tag.name == 'tagName'
        tag.ownerId == 'tagOwner'
        tag.isVisibleInWorkView() == true
    }

    def "should convert list of db objects to list of tags"() {
        given:
        List<DBObject> dbTags = []
        dbTags << new BasicDBObject([_id: '1', name: 'tag1OfOwner1', owner_id: 'owner1Login', color: 'blue', visible_in_workview: true])
        dbTags << new BasicDBObject([_id: '2', name: 'tag2OfOwner1', owner_id: 'owner1Login', color: 'white', visible_in_workview: false])
        dbTags << new BasicDBObject([_id: '3', name: 'tag1OfOwner2', owner_id: 'owner2Login', color: 'pink', visible_in_workview: true])
        dbTags << new BasicDBObject([_id: '4', name: 'tag3OfOwner1', owner_id: 'owner1Login', color: 'black', visible_in_workview: false])
        when:
        def tagsSet = dbTagConverter.convertDbObjectsToSetOfTags(dbTags)
        then:
        tagsSet.size() == 4
        dbTags.each { assert tagsSet.collect { it.name }.contains(it.get('name'))  }
    }
}
