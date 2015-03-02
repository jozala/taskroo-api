package com.taskroo.acceptance

import com.mongodb.BasicDBObject
import com.mongodb.DB
import com.mongodb.DBCollection
import com.mongodb.QueryBuilder
import groovyx.net.http.RESTClient
import com.taskroo.mongo.MongoConnector
import spock.lang.Specification

abstract class AcceptanceTestBase extends Specification {

    static RESTClient client = new RESTClient('http://localhost:8080/')
    private static final DB db = new MongoConnector('localhost', '27017').getDatabase('taskroo')
    public static final DBCollection tagsCollection = db.getCollection('tags')
    public static final DBCollection tasksCollection = db.getCollection('tasks')
    public static final DBCollection securityTokensCollection = db.getCollection("securityTokens")

    public static final String TEST_USER_ID = 'userName'


    def setupSpec() {
        client.handler.failure = { it }
    }

    def cleanupSpec() {
        securityTokensCollection.remove(QueryBuilder.start('user_id').is(TEST_USER_ID).get())
        tasksCollection.remove(QueryBuilder.start('owner_id').is(TEST_USER_ID).get())
        tagsCollection.remove(QueryBuilder.start('_id.owner_id').is(TEST_USER_ID).get())
    }


    protected String createSecurityTokenWithUser(String userId) {
        def securityTokenId = UUID.randomUUID().toString()
        securityTokensCollection.insert(new BasicDBObject(
                [_id: securityTokenId, user_id: userId, roles: [1], create_time: new Date(), last_accessed_time: new Date()]))
        return securityTokenId
    }

    protected String generateAuthorizationHeader(String tokenKey) {
        def randomString = UUID.randomUUID().toString()
        return "TaskRooAuth realm=\"taskroo@aetas.pl\", tokenKey=\"$tokenKey\", cnonce=\"$randomString\""
    }
}
