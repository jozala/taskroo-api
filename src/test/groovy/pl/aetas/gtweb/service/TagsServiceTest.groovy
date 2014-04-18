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
        def tag = new Tag(null, null, 'someTagName', 'orange', true)
        tagDao.exists(tag) >> true
        when:
        def response = tagsService.create(securityContext, tag)
        then:
        response.status == 200
    }
}
