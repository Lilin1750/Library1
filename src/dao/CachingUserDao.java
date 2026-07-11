package dao;

import model.User;
import exception.UserNotFoundException;
import exception.DataAccessException;

public class CachingUserDao implements UserDao {

    private final UserDao sqlDao;
    private final ArrayListUserDao memoryCache;
    private final FileUserDao fileCache;
    private boolean memoryLoaded = false;

    public CachingUserDao(UserDao sqlDao, ArrayListUserDao memoryCache, FileUserDao fileCache) {
        this.sqlDao = sqlDao;
        this.memoryCache = memoryCache;
        this.fileCache = fileCache;
    }

    private void ensureMemoryLoaded() {
        if (!memoryLoaded) {
            try {
                java.util.List<User> users = fileCache.findAllUsers();
                for (User u : users) {
                    memoryCache.addUser(u);
                }
            } catch (Exception e) {
                // file cache不可用，后续按需从SQL加载
            }
            memoryLoaded = true;
        }
    }

    @Override
    public void addUser(User user) {
        sqlDao.addUser(user);
        memoryCache.addUser(user);
        fileCache.addUser(user);
    }

    @Override
    public User findByUsername(String username) {
        ensureMemoryLoaded();
        try {
            return memoryCache.findByUsername(username);
        } catch (UserNotFoundException e) {
            User user = sqlDao.findByUsername(username);
            memoryCache.addUser(user);
            try { fileCache.addUser(user); } catch (Exception ignored) {}
            return user;
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        ensureMemoryLoaded();
        return memoryCache.existsByUsername(username);
    }
}
