package com.taskroo.data;

import com.mongodb.*;
import org.springframework.stereotype.Repository;
import com.taskroo.domain.Role;
import com.taskroo.service.security.SecurityToken;

import javax.inject.Inject;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Repository
public class SecurityTokenDao {

    private DBCollection securityTokensCollection;

    @Inject
    public SecurityTokenDao(DBCollection securityTokensCollection) {
        this.securityTokensCollection = securityTokensCollection;
    }

    public SecurityToken findOneAndUpdateLastAccessedTime(String securityTokenId) {
        DBObject secTokenDbObject = securityTokensCollection.findAndModify(QueryBuilder.start("_id").is(securityTokenId).get(), null,
                null, false, new BasicDBObject("$set", new BasicDBObject("last_accessed_time", new Date())), true, false);

        if (secTokenDbObject == null) {
            return null;
        }
        return mapDbObjectToSecurityToken(secTokenDbObject);
    }

    private SecurityToken mapDbObjectToSecurityToken(DBObject securityTokenDbObject) {
        String userId = securityTokenDbObject.get("user_id").toString();
        Date createTime = (Date) securityTokenDbObject.get("create_time");
        Date lastAccessedTime = (Date) securityTokenDbObject.get("last_accessed_time");
        Set<Role> roles = new HashSet<>(2);
        for (Object roleInInteger : (BasicDBList) securityTokenDbObject.get("roles")) {
            roles.add(Role.getByInt((Integer)roleInInteger));
        }

        return new SecurityToken(securityTokenDbObject.get("_id").toString(), userId, roles, createTime, lastAccessedTime);
    }
}
