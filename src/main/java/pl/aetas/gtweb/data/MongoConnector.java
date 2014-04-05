package pl.aetas.gtweb.data;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import java.net.UnknownHostException;

public class MongoConnector {

    public DB getDatabase(String dbName) throws UnknownHostException {
        String mongoURIString = "mongodb://localhost";

        final MongoClient mongoClient = new MongoClient(new MongoClientURI(mongoURIString));
        return mongoClient.getDB(dbName);
    }
}
