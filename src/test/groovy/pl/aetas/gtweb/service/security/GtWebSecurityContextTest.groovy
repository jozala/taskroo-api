package pl.aetas.gtweb.service.security

import pl.aetas.gtweb.domain.Role
import pl.aetas.gtweb.domain.User
import spock.lang.Specification

import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response

class GtWebSecurityContextTest extends Specification {

    def "should throw exception with Forbidden response when session is not set"() {
        when:
        def sc = new GtWebSecurityContext(null, Mock(User));
        sc.isUserInRole('user')
        then:
        def ex = thrown(WebApplicationException)
        ex.response.status == Response.Status.FORBIDDEN.statusCode
    }

    def "should check if user has given role"() {
        given:
        def session = Mock(Session)
        def user = Mock(User)
        user.getRoles() >> userRoles
        def sc = new GtWebSecurityContext(session, user);
        expect:
        userIsInRole == sc.isUserInRole(roleToCheck)
        where:
        userRoles                 | roleToCheck | userIsInRole
        [Role.ADMIN]              | 'admin'     | true
        [Role.ADMIN]              | 'user'      | false
        [Role.USER, Role.ADMIN]   | 'admin'     | true

    }
}
