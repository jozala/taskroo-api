package pl.aetas.gtweb.integration.data

import pl.aetas.gtweb.data.DBConnectionException
import pl.aetas.gtweb.data.MongoConnector

class MongoConnectorTest extends GroovyTestCase {

    // SUT
    private MongoConnector mongoConnector;

    void testShouldThrowDbExceptionWhenUnableToConnectToDb() {
        mongoConnector = new MongoConnector('mongodb://incorrectHostname')

        assert shouldFail(DBConnectionException.class, {
            mongoConnector.getGtWebDatabase();
        })

    }

    void testShouldReturnDbWhenCorrectHostnameGiven() {
        mongoConnector = new MongoConnector('mongodb://localhost')
        assert mongoConnector.getGtWebDatabase() != null
    }
}
