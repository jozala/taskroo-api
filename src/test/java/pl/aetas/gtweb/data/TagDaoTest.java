package pl.aetas.gtweb.data;

import com.mongodb.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.aetas.gtweb.domain.Tag;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TagDaoTest {

    // SUT
    private TagDao tagDao;

    @Mock
    private DBCollection tagsCollection;

    @Mock
    private DBCursor dbCursor;

    @Before
    public void setUp() throws Exception {
        tagDao = new TagDao(tagsCollection);
    }

    @Test
    @Ignore
    public void shouldMapTagsFromDbToTagsObjects() throws Exception {
        DBObject tagDbObject = BasicDBObjectBuilder
                .start("_id", new BasicDBObject("name", "tagName").append("owner_id", "userLogin"))
                .add("color", "white")
                .add("visibleInWorkView", true)
                .get();

        when(dbCursor.hasNext()).thenReturn(false).thenReturn(false);
        when(dbCursor.next()).thenReturn(tagDbObject);
        when(tagsCollection.find(any(BasicDBObject.class))).thenReturn(dbCursor);

        List<Tag> tags = tagDao.getAllTagsByOwnerId("userLogin");

        Tag expectedTag = new Tag.TagBuilder().name("tagName").color("white").visibleInWorkView(true).build();
        assertThat(tags).contains(expectedTag);
    }
}
