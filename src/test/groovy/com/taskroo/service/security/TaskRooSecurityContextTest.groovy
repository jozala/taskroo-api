package com.taskroo.service.security

import com.taskroo.domain.Role
import com.taskroo.domain.User
import spock.lang.Specification

import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response

class TaskRooSecurityContextTest extends Specification {

    def "should throw exception with Forbidden response when session is not set"() {
        when:
        def sc = new TaskRooSecurityContext(null, Mock(User), "");
        sc.isUserInRole('user')
        then:
        def ex = thrown(WebApplicationException)
        ex.response.status == Response.Status.FORBIDDEN.statusCode
    }

    def "should return WWW-authenticate header with Forbidden response when session is not set"() {
        when:
        def sc = new TaskRooSecurityContext(null, Mock(User), "");
        sc.isUserInRole('user')
        then:
        def ex = thrown(WebApplicationException)
        ex.response.getHeaderString('WWW-Authenticate').contains('TaskRooAuth realm="taskroo@aetas.pl"')
    }

    def "should return WWW-Authenticate header with domain pointing to authentication service"() {
        when:
        def sc = new TaskRooSecurityContext(null, Mock(User), "http://localhost:8089");
        sc.isUserInRole('user')
        then:
        def ex = thrown(WebApplicationException)
        ex.response.getHeaderString('WWW-Authenticate').contains('domain="http://localhost:8089"')
    }

    def "should check if user has given role"() {
        given:
        def session = Mock(Session)
        def user = Mock(User)
        user.getRoles() >> userRoles
        def sc = new TaskRooSecurityContext(session, user, "");
        expect:
        userIsInRole == sc.isUserInRole(roleToCheck)
        where:
        userRoles                 | roleToCheck | userIsInRole
        [Role.ADMIN]              | 'admin'     | true
        [Role.ADMIN]              | 'user'      | false
        [Role.USER, Role.ADMIN]   | 'admin'     | true

    }
}
