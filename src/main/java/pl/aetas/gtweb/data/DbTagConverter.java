package pl.aetas.gtweb.data;

import com.mongodb.DBObject;
import org.springframework.stereotype.Component;
import pl.aetas.gtweb.domain.Tag;

import java.util.ArrayList;
import java.util.List;

@Component
public class DbTagConverter {

    public Tag convertDbObjectToTag(DBObject dbTag) {
        String id = dbTag.get(TagDao.ID_KEY).toString();
        String name = dbTag.get(TagDao.NAME_KEY).toString();
        String ownerId = dbTag.get(TagDao.OWNER_ID_KEY).toString();
        String color = (String) dbTag.get(TagDao.COLOR_KEY);
        boolean isVisibleInWorkView = (boolean) dbTag.get(TagDao.VISIBLE_IN_WORK_VIEW_KEY);

        return Tag.TagBuilder.start(ownerId, name)
                .id(id)
                .color(color)
                .visibleInWorkView(isVisibleInWorkView)
                .build();
    }

    public List<Tag> convertDbObjectsToSetOfTags(List<DBObject> dbTags) {
        List<Tag> tags = new ArrayList<>(dbTags.size());
        for (DBObject dbTag : dbTags) {
            Tag tag = convertDbObjectToTag(dbTag);
            tags.add(tag);
        }
        return tags;
    }
}
