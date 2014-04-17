package pl.aetas.gtweb.service

import pl.aetas.gtweb.data.TagDao
import spock.lang.Specification

import javax.ws.rs.core.SecurityContext
import java.security.Principal

class TagsServiceTest extends Specification {

    TagsService tagsService

    // mocks
    TagDao tagDao
    SecurityContext securityContext

    void setup() {
        tagDao = Mock(TagDao)
        securityContext = Mock(SecurityContext)
        tagsService = new TagsService(tagDao)

    }

    def "should retrieve all tags for user from security context"() {
        given:
        def principal = Mock(Principal)
        principal.getName() >> 'testUserName'
        securityContext.getUserPrincipal() >> principal
        when:
        tagsService.getAll(securityContext)
        then:
        1 * tagDao.getAllTagsByOwnerId('testUserName')

    }
}
