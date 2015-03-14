package com.taskroo.data
import com.mongodb.DB
import com.mongodb.DBCollection
import com.taskroo.mongo.MongoConnector
import spock.lang.Specification

class DaoTestBase extends Specification {

    private static final DB db = new MongoConnector(System.properties.getProperty('MONGO_PORT_27017_TCP_ADDR', 'localhost'), System.properties.getProperty('MONGO_PORT_27017_TCP_PORT', '27017'))
            .getDatabase('taskroo-dao-tests-db')
    public static final DBCollection tagsCollection = db.getCollection('tags')
    public static final DBCollection tasksCollection = db.getCollection('tasks')
    public static final DBCollection securityTokensCollection = db.getCollection('securityTokens')
}
