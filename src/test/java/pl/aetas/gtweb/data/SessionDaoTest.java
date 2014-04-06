package pl.aetas.gtweb.data;

import com.mongodb.*;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.aetas.gtweb.service.security.Session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SessionDaoTest {

    public static final String TEST_SESSION_ID = "53416b4d3004a7775326b98f";
    // SUT
    private SessionDao sessionDao;

    @Mock
    private DBCollection sessionCollection;

    @Before
    public void setUp() throws Exception {
        sessionDao = new SessionDao(sessionCollection);

        DBObject sessionDbObject = BasicDBObjectBuilder.start("_id", new ObjectId(TEST_SESSION_ID))
                .add("user_id", "someUsername")
                .add("create_time", DateTime.parse("2014-03-05T21:04:12").toDate())
                .add("last_accessed_time", DateTime.parse("2014-03-05T21:35:06").toDate())
                .get();

        when(sessionCollection.findOne(QueryBuilder.start("_id").is(new ObjectId(TEST_SESSION_ID)).get()))
                .thenReturn(sessionDbObject);
    }

    @Test
    public void shouldRetrieveSingleSessionUsingGivenSessionId() throws Exception {
        sessionDao.findOne(TEST_SESSION_ID);
        verify(sessionCollection).findOne(new BasicDBObject("_id", new ObjectId(TEST_SESSION_ID)));
    }

    @Test
    public void shouldReturnSessionObjectRetrievedFromDb() throws Exception {
        Session session = sessionDao.findOne(TEST_SESSION_ID);
        assertThat(session.getSessionId()).isEqualTo(TEST_SESSION_ID);
    }

    @Test
    public void shouldMapAllValuesFromSessionDbObjectToSessionObject() throws Exception {
        Session session = sessionDao.findOne(TEST_SESSION_ID);
        assertThat(session.getSessionId()).isEqualTo(TEST_SESSION_ID);
        assertThat(session.getUserId()).isEqualTo("someUsername");
        assertThat(session.getCreateTime()).isEqualTo(DateTime.parse("2014-03-05T21:04:12").toDate());
        assertThat(session.getLastAccessedTime()).isEqualTo(DateTime.parse("2014-03-05T21:35:06").toDate());
    }

    @Test
    public void shouldReturnNullWhenSessionWithGivenIdHasNotBeenFound() throws Exception {
        Session session = sessionDao.findOne("53416a07300494c188c3905b");
        assertThat(session).isNull();
    }
}
