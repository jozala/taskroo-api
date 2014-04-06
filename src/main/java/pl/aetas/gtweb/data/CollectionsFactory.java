package pl.aetas.gtweb.data;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class CollectionsFactory {

    private DB mongoDb;

    @Inject
    public CollectionsFactory(DB mongoDb) {
        this.mongoDb = mongoDb;
    }

    public DBCollection getCollection(String collectionName) {
        return mongoDb.getCollection(collectionName);
    }
}
