package pl.aetas.gtweb.data;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.springframework.stereotype.Repository;
import pl.aetas.gtweb.service.security.Session;

import javax.inject.Inject;
import java.util.Date;

@Repository
public class SessionDao {

    private DBCollection sessionsCollection;

    @Inject
    public SessionDao(DBCollection sessionsCollection) {
        this.sessionsCollection = sessionsCollection;
    }

    // TODO create Session always with TTL for 30 minutes
    public Session findOne(String sessionId) {
        DBObject sessionDbObject = sessionsCollection.findOne(QueryBuilder.start("_id").is(sessionId).get());
        if (sessionDbObject == null) {
            return null;
        }
        return mapDbObjectToSession(sessionDbObject);
    }

    private Session mapDbObjectToSession(DBObject sessionDbObject) {
        String userId = sessionDbObject.get("user_id").toString();
        Date createTime = (Date) sessionDbObject.get("create_time");
        Date lastAccessedTime = (Date) sessionDbObject.get("last_accessed_time");

        Session session = new Session();
        session.setSessionId(sessionDbObject.get("_id").toString());
        session.setUserId(userId);
        session.setCreateTime(createTime);
        session.setLastAccessedTime(lastAccessedTime);

        return session;
    }
}
