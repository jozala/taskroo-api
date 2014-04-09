package pl.aetas.gtweb.data;

import com.mongodb.BasicDBList;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.springframework.stereotype.Repository;
import pl.aetas.gtweb.domain.Role;
import pl.aetas.gtweb.domain.User;

import javax.inject.Inject;

@Repository
public class UserDao {
    private final DBCollection usersCollection;

    @Inject
    public UserDao(DBCollection usersCollection) {
        this.usersCollection = usersCollection;
    }

    // TODO do not return here the user with password - this is not secure
    public User findOne(String userId) {

        String username = "owner1Login";
        String email = "test@example.com";
        String firstname = "foo";
        String lastname = "bar";
        String password = "123456";
        Role role = Role.USER;
        return User.UserBuilder.start().username(username).email(email).firstName(firstname).lastName(lastname)
                .password(password).role(role).setEnabled(true).build();
    }

    private User mapUserDbObjectToUser(DBObject userDbObject) {

        User.UserBuilder userBuilder =
                User.UserBuilder.start()
                        .username(userDbObject.get("_id").toString())
                        .setEnabled((Boolean)userDbObject.get("enabled"))
                        .firstName(userDbObject.get("first_name").toString())
                        .lastName(userDbObject.get("last_name").toString())
                        .password(userDbObject.get("password").toString())
                        .email(userDbObject.get("email").toString());

        BasicDBList rolesStrings = (BasicDBList) userDbObject.get("roles");
        for (Object roleString : rolesStrings) {
            Role role = Role.valueOf(roleString.toString());
            userBuilder.role(role);
        }

        return userBuilder.build();

    }
}
