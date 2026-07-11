package dao;

import model.Admin;
import model.NormalUser;
import model.User;
import exception.UserNotFoundException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileUserDao implements UserDao {
    private final List<User> users = new ArrayList<>();
    private final String filePath;

    public FileUserDao(String filePath) {
        this.filePath = filePath;
        ensureFileExists();
        loadFromFile();
    }

    private void ensureFileExists() {
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("无法创建数据文件：" + filePath, e);
            }
        }
    }

    private void loadFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                String username = parts[0];
                String password = parts[1];
                String role = parts[2];

                User user;
                if ("admin".equalsIgnoreCase(role)) {
                    user = new Admin(username, password);
                } else {
                    user = new NormalUser(username, password);
                }
                users.add(user);
            }
        } catch (IOException e) {
            throw new RuntimeException("读取用户数据失败：" + e.getMessage(), e);
        }
    }

    private void saveToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (User user : users) {
                String role = (user instanceof Admin) ? "admin" : "user";
                writer.write(user.getUsername() + "|" + user.getPassword() + "|" + role);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("保存用户数据失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void addUser(User user) {
        users.add(user);
        saveToFile();
    }

    @Override
    public User findByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        throw new UserNotFoundException("用户「" + username + "」不存在！");
    }

    @Override
    public boolean existsByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public java.util.List<User> findAllUsers() {
        return new java.util.ArrayList<>(users);
    }
}
