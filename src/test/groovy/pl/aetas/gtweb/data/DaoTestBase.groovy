package pl.aetas.gtweb.data
import com.mongodb.DB
import com.mongodb.DBCollection
import pl.aetas.gtweb.mongo.MongoConnector
import spock.lang.Specification

class DaoTestBase extends Specification {

    private static final DB db = new MongoConnector('mongodb://localhost').getDatabase('gtweb-dao-tests-db')
    public static final DBCollection tagsCollection = db.getCollection('tags')
    public static final DBCollection tasksCollection = db.getCollection('tasks')
    public static final DBCollection sessionsCollection = db.getCollection('sessions')
}
