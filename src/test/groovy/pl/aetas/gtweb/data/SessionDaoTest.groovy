package pl.aetas.gtweb.data

import com.mongodb.BasicDBObject
import org.bson.types.ObjectId
import org.joda.time.DateTime
import pl.aetas.gtweb.domain.Role

class SessionDaoTest extends DaoTestBase {

    SessionDao sessionDao

    void setup() {
        sessionDao = new SessionDao(sessionsCollection)
        cleanup()
    }

    void cleanup() {
        sessionsCollection.drop()
    }

    def "should change last accessed time of session with given id to now"() {
        given:
        def halfHourAgo = DateTime.now().minusMinutes(30)
        def twentyMinutesAgo = DateTime.now().minusMinutes(20)
        def sessionId = createTestSessionInDb(halfHourAgo, twentyMinutesAgo);
        when:
        sessionDao.findOneAndUpdateLastAccessedTime(sessionId)
        then:
        def sessionDbAfterUpdate = sessionsCollection.findOne(new BasicDBObject('_id', sessionId))
        sessionDbAfterUpdate.get('last_accessed_time').after(DateTime.now().minusSeconds(5).toDate())
        sessionDbAfterUpdate.get('last_accessed_time').before(DateTime.now().plusSeconds(5).toDate())
    }

    def "should retrieve single session using given session ID"() {
        given:
        def halfHourAgo = DateTime.now().minusMinutes(30)
        def twentyMinutesAgo = DateTime.now().minusMinutes(20)
        def sessionId = createTestSessionInDb(halfHourAgo, twentyMinutesAgo);
        when:
        def session = sessionDao.findOneAndUpdateLastAccessedTime(sessionId)
        then:
        session.sessionId == sessionId
        session.roles.toSet() == [Role.USER, Role.ADMIN].toSet()
        session.userId == 'mariusz'
        session.createTime == halfHourAgo.toDate()
    }

    def "should return null when session with given id has not been found"() {
        when:
        def session = sessionDao.findOneAndUpdateLastAccessedTime(ObjectId.get().toString())
        then:
        session == null
    }

    private static String createTestSessionInDb(DateTime createTime, DateTime lastAccessedTime) {
        def sessionId = UUID.randomUUID().toString();
        sessionsCollection.insert(new BasicDBObject([_id: sessionId, roles: [0, 1], user_id: 'mariusz',
                                                     create_time: createTime.toDate(), last_accessed_time: lastAccessedTime.toDate()]))
        return sessionId;
    }
}
