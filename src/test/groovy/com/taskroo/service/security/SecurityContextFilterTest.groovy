package com.taskroo.service.security

import com.taskroo.data.SecurityTokenDao
import com.taskroo.domain.Role
import spock.lang.Specification

import javax.ws.rs.container.ContainerRequestContext

class SecurityContextFilterTest extends Specification {

    SecurityContextFilter securityContextFilter

    // mocks
    SecurityTokenDao securityTokenDao

    void setup() {
        securityTokenDao = Mock(SecurityTokenDao)
        securityContextFilter = new SecurityContextFilter(securityTokenDao, "http://taskroo.com/auth")
    }

    def "should set security context without security token when authorization header is not available"() {
        given:
        ContainerRequestContext containerRequestContext = Mock(ContainerRequestContext)
        when:
        securityContextFilter.filter(containerRequestContext)
        then:
        1 * containerRequestContext.setSecurityContext({ it.securityToken == null && it.userPrincipal == null })
    }

    def "should set security context without security token when authorization header does not contain tokenKey"() {
        given:
        ContainerRequestContext containerRequestContext = Mock(ContainerRequestContext)
        containerRequestContext.getHeaderString('Authorization') >> 'TaskRooAuth realm="taskroo@aetas.pl",cnonce="uniqueValue"'
        when:
        securityContextFilter.filter(containerRequestContext)
        then:
        1 * containerRequestContext.setSecurityContext({ it.securityToken == null && it.userPrincipal == null })
    }

    def "should set security context with security token set when authorization header contains correct tokenKey"() {
        given:
        ContainerRequestContext containerRequestContext = Mock(ContainerRequestContext)
        containerRequestContext.getHeaderString('Authorization') >> 'TaskRooAuth realm="taskroo@aetas.pl",cnonce="uniqueValue",tokenKey="someTokenKey"'
        def securityToken = new SecurityToken('someTokenKey', 'userId', [Role.USER].toSet(), null, null)
        securityTokenDao.findOneAndUpdateLastAccessedTime('someTokenKey') >> securityToken
        when:
        securityContextFilter.filter(containerRequestContext)
        then:
        1 * containerRequestContext.setSecurityContext({ it.securityToken == securityToken && it.userPrincipal.name == 'userId'})
    }
}
