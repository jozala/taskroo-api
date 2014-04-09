package pl.aetas.gtweb.acceptance
import com.mongodb.DB
import com.mongodb.DBCollection
import pl.aetas.gtweb.mongo.MongoConnector
import spock.lang.Specification

abstract class AcceptanceTestBase extends Specification {
    static final String APP_URL = 'http://localhost:8080';
    static final DB db = new MongoConnector('mongodb://localhost').getDatabase('gtweb')
    public static final DBCollection tagsCollection = db.getCollection('tags')
    public static final DBCollection sessionCollection = db.getCollection("sessions")



}
