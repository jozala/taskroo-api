package pl.aetas.gtweb.integration.data
import com.mongodb.DB
import com.mongodb.DBCollection
import pl.aetas.gtweb.mongo.MongoConnector
import spock.lang.Specification

class IntegrationTestBase extends Specification {

    private static final DB db = new MongoConnector('mongodb://localhost').getDatabase('gtweb-integration-tests-db')
    public static final DBCollection tagsCollection = db.getCollection('tags')
    public static final DBCollection tasksCollection = db.getCollection('tasks')
    public static final DBCollection sessionCollection = db.getCollection("sessions")
}
