package pl.aetas.gtweb.service.security

import pl.aetas.gtweb.data.SessionDao
import spock.lang.Specification

import javax.ws.rs.container.ContainerRequestContext

class SecurityContextFilterTest extends Specification {

    SecurityContextFilter securityContextFilter

    // mocks
    SessionDao sessionDao

    void setup() {
        sessionDao = Mock(SessionDao)
        securityContextFilter = new SecurityContextFilter(sessionDao)

    }

    def "should set security context without session when session id is not set"() {
        given:
        ContainerRequestContext containerRequestContext = Mock(ContainerRequestContext)
        when:
        securityContextFilter.filter(containerRequestContext)
        then:
        1 * containerRequestContext.setSecurityContext({ it.session == null && it.userPrincipal == null })
    }

    def "should set security context with session set when session id contains correct value"() {
        given:
        ContainerRequestContext containerRequestContext = Mock(ContainerRequestContext)
        containerRequestContext.getHeaderString('Session-Id') >> 'someSessionId'
        def session = new Session(userId: 'userId', sessionId: 'someSessionId')
        sessionDao.findOne('someSessionId') >> session
        when:
        securityContextFilter.filter(containerRequestContext)
        then:
        1 * containerRequestContext.setSecurityContext({ it.session == session && it.userPrincipal.name == 'userId'})
    }
}
