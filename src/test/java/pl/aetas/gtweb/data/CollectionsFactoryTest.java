package pl.aetas.gtweb.data;

import com.mongodb.DB;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CollectionsFactoryTest {

    // SUT
    private CollectionsFactory collectionsFactory;

    @Mock
    private DB mongoDb;

    @Before
    public void setUp() throws Exception {
        collectionsFactory = new CollectionsFactory(mongoDb);
    }

    @Test
    public void shouldRetrieveCollectionFromGtWebDatabase() throws Exception {
        collectionsFactory.getCollection("someCollectionName");

        verify(mongoDb).getCollection("someCollectionName");
    }
}
