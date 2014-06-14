package pl.aetas.gtweb.service.security

import pl.aetas.gtweb.data.SessionDao
import pl.aetas.gtweb.domain.Role
import spock.lang.Specification

import javax.ws.rs.container.ContainerRequestContext

class SecurityContextFilterTest extends Specification {

    SecurityContextFilter securityContextFilter

    // mocks
    SessionDao sessionDao

    void setup() {
        sessionDao = Mock(SessionDao)
        securityContextFilter = new SecurityContextFilter(sessionDao, "http://auth.gtweb.aetas.pl")
    }

    def "should set security context without session when authorization header is not available"() {
        given:
        ContainerRequestContext containerRequestContext = Mock(ContainerRequestContext)
        when:
        securityContextFilter.filter(containerRequestContext)
        then:
        1 * containerRequestContext.setSecurityContext({ it.session == null && it.userPrincipal == null })
    }

    def "should set security context without session when authorization header does not contain tokenKey"() {
        given:
        ContainerRequestContext containerRequestContext = Mock(ContainerRequestContext)
        containerRequestContext.getHeaderString('Authorization') >> 'GTWebAuth realm="gtweb@aetas.pl",cnonce="uniqueValue"'
        when:
        securityContextFilter.filter(containerRequestContext)
        then:
        1 * containerRequestContext.setSecurityContext({ it.session == null && it.userPrincipal == null })
    }

    def "should set security context with session set when authorization header contains correct tokenKey"() {
        given:
        ContainerRequestContext containerRequestContext = Mock(ContainerRequestContext)
        containerRequestContext.getHeaderString('Authorization') >> 'GTWebAuth realm="gtweb@aetas.pl",cnonce="uniqueValue",tokenKey="someTokenKey"'
        def session = new Session('someTokenKey', 'userId', [Role.USER].toSet(), null, null)
        sessionDao.findOne('someTokenKey') >> session
        when:
        securityContextFilter.filter(containerRequestContext)
        then:
        1 * containerRequestContext.setSecurityContext({ it.session == session && it.userPrincipal.name == 'userId'})
    }
}
