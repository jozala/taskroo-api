package pl.aetas.gtweb.data;

import org.springframework.stereotype.Repository;
import pl.aetas.gtweb.domain.Role;
import pl.aetas.gtweb.domain.User;

@Repository
public class UserDao {
    public User findOne(String userId) {

        String username = "owner1Login";
        String email = "test@example.com";
        String firstname = "foo";
        String lastname = "bar";
        String password = "123456";
        Role role = Role.USER;
        return new User.UserBuilder().username(username).email(email).firstName(firstname).lastName(lastname)
                .password(password).role(role).setEnabled(true).build();
    }
}
