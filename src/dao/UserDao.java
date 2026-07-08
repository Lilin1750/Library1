package dao;

import model.User;

public interface UserDao {
    void addUser(User user);

    User findByUsername(String username);

    boolean existsByUsername(String username);
}
