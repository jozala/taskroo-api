package pl.aetas.gtweb.service

import pl.aetas.gtweb.data.TagDao
import pl.aetas.gtweb.domain.Tag
import spock.lang.Specification

import javax.ws.rs.core.SecurityContext
import java.security.Principal

class TagsServiceTest extends Specification {

    TagsService tagsService

    // mocks
    TagDao tagDao
    SecurityContext securityContext
    Principal principal

    void setup() {
        tagDao = Mock(TagDao)
        securityContext = Mock(SecurityContext)
        tagsService = new TagsService(tagDao)

        principal = Mock(Principal)
        securityContext.getUserPrincipal() >> principal
    }

    def "should retrieve all tags for user from security context"() {
        given:
        principal.getName() >> 'testUserName'
        when:
        tagsService.getAll(securityContext)
        then:
        1 * tagDao.getAllTagsByOwnerId('testUserName')
    }

    def "should insert tag to DB when creating new tag"() {
        given:
        def tag = new Tag(null, null, 'someTagName', 'orange', true)
        when:
        tagsService.create(securityContext, tag)
        then:
        1 * tagDao.insert(tag) >> tag;
    }

    def "should return 201 and tag after insert when creating new tag"() {
        given:
        def tag = new Tag(null, null, 'someTagName', 'orange', true)
        def tagFromDao = new Tag('id', 'ownerId', 'someTagName', 'orange', true)
        tagDao.insert(tag) >> tagFromDao
        when:
        def response = tagsService.create(securityContext, tag)
        then:
        response.status == 201
        response.entity == tagFromDao
    }

    def "should set ownerId of tag from securityContext principal name when creating new tag"() {
        given:
        def tag = new Tag(null, null, 'someTagName', 'orange', true)
        principal.getName() >> 'testUserName'
        when:
        tagsService.create(securityContext, tag)
        then:
        1 * tagDao.insert({ it.ownerId == 'testUserName'}) >> tag
    }

    def "should return 200 with tag when trying to create tag with name which already exists"() {
        given:
        principal.getName() >> 'ownerId'
        def tag = new Tag(null, null, 'someTagName', 'orange', true)
        tagDao.findOne('ownerId','someTagName') >> new Tag('79821374893', 'ownerId', 'someTagName', 'orange', true)
        tagDao.insert(tag) >> new Tag('id', 'ownerId', 'someTagName', 'orange', true)
        when:
        def response = tagsService.create(securityContext, tag)
        then:
        response.status == 200
    }

    def "should remove tag from DB when deleting tag"() {
        given:
        principal.getName() >> 'ownerId'
        tagDao.findOne('ownerId', 'someTagName') >> Mock(Tag)
        when:
        tagsService.delete(securityContext, 'someTagName')
        then:
        1 * tagDao.remove('ownerId', 'someTagName')
    }

    def "should return 400 when trying to delete tag which does not exist"() {
        given:
        principal.getName() >> 'ownerId'
        tagDao.findOne('ownerId', 'someTagName') >> null
        when:
        def response = tagsService.delete(securityContext, 'someTagName')
        then:
        response.status == 400
    }

    def "should return 204 when tag has been deleted correctly"() {
        given:
        principal.getName() >> 'ownerId'
        tagDao.findOne('ownerId', 'someTagName') >> Mock(Tag)
        when:
        def response = tagsService.delete(securityContext, 'someTagName')
        then:
        response.status == 204
    }
}
