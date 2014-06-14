package pl.aetas.gtweb.data;

import com.mongodb.*;
import org.springframework.stereotype.Repository;
import pl.aetas.gtweb.domain.Role;
import pl.aetas.gtweb.service.security.Session;

import javax.inject.Inject;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Repository
public class SessionDao {

    private DBCollection sessionsCollection;

    @Inject
    public SessionDao(DBCollection sessionsCollection) {
        this.sessionsCollection = sessionsCollection;
    }

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
        Set<Role> roles = new HashSet<>(2);
        for (Object roleInInteger : (BasicDBList) sessionDbObject.get("roles")) {
            roles.add(Role.getByInt((Integer)roleInInteger));
        }

        return new Session(sessionDbObject.get("_id").toString(), userId, roles, createTime, lastAccessedTime);
    }
}
