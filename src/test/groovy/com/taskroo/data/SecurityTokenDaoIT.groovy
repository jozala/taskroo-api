package com.taskroo.data

import com.mongodb.BasicDBObject
import org.bson.types.ObjectId
import org.joda.time.DateTime
import com.taskroo.domain.Role

class SecurityTokenDaoIT extends DaoTestBase {

    SecurityTokenDao securityTokenDao

    void setup() {
        securityTokenDao = new SecurityTokenDao(securityTokensCollection)
        cleanup()
    }

    void cleanup() {
        securityTokensCollection.drop()
    }

    def "should change last accessed time of security token with given id to now"() {
        given:
        def halfHourAgo = DateTime.now().minusMinutes(30)
        def twentyMinutesAgo = DateTime.now().minusMinutes(20)
        def securityId = createTestSecurityTokenInDb(halfHourAgo, twentyMinutesAgo);
        when:
        securityTokenDao.findOneAndUpdateLastAccessedTime(securityId)
        then:
        def securityTokenDbAfterUpdate = securityTokensCollection.findOne(new BasicDBObject('_id', securityId))
        securityTokenDbAfterUpdate.get('last_accessed_time').after(DateTime.now().minusSeconds(5).toDate())
        securityTokenDbAfterUpdate.get('last_accessed_time').before(DateTime.now().plusSeconds(5).toDate())
    }

    def "should retrieve single security token using given security token ID"() {
        given:
        def halfHourAgo = DateTime.now().minusMinutes(30)
        def twentyMinutesAgo = DateTime.now().minusMinutes(20)
        def securityId = createTestSecurityTokenInDb(halfHourAgo, twentyMinutesAgo);
        when:
        def securityToken = securityTokenDao.findOneAndUpdateLastAccessedTime(securityId)
        then:
        securityToken.id == securityId
        securityToken.roles.toSet() == [Role.USER, Role.ADMIN].toSet()
        securityToken.userId == 'mariusz'
        securityToken.createTime == halfHourAgo.toDate()
    }

    def "should return null when security token with given id has not been found"() {
        when:
        def securityToken = securityTokenDao.findOneAndUpdateLastAccessedTime(ObjectId.get().toString())
        then:
        securityToken == null
    }

    private static String createTestSecurityTokenInDb(DateTime createTime, DateTime lastAccessedTime) {
        def securityId = UUID.randomUUID().toString();
        securityTokensCollection.insert(new BasicDBObject([_id: securityId, roles: [0, 1], user_id: 'mariusz',
                                                     create_time: createTime.toDate(), last_accessed_time: lastAccessedTime.toDate()]))
        return securityId;
    }
}
