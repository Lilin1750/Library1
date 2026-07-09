package dao;

import model.Admin;
import model.NormalUser;
import model.User;
import exception.UserNotFoundException;
import exception.UserDuplicateException;

import java.sql.*;

public class SqlUserDaoImpl implements UserDao {

    @Override
    public void addUser(User user) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBUtil.getConnection();

            if (existsByUsername(user.getUsername())) {
                throw new UserDuplicateException("用户名「" + user.getUsername() + "」已存在！");
            }

            String sql = "INSERT INTO `user` (username, password, role) VALUES (?, ?, ?)";
            ps = conn.prepareStatement(sql);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("添加用户失败！", e);
        } finally {
            DBUtil.close(ps, conn);
        }
    }

    @Override
    public User findByUsername(String username) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT username, password, role FROM `user` WHERE username = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            rs = ps.executeQuery();

            if (rs.next()) {
                return mapRowToUser(rs);
            }
            throw new UserNotFoundException("用户「" + username + "」不存在！");
        } catch (SQLException e) {
            throw new RuntimeException("查询用户失败！", e);
        } finally {
            DBUtil.close(rs, ps, conn);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT COUNT(*) FROM `user` WHERE username = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            rs = ps.executeQuery();

            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("检查用户是否存在失败！", e);
        } finally {
            DBUtil.close(rs, ps, conn);
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        String username = rs.getString("username");
        String password = rs.getString("password");
        String role = rs.getString("role");

        if ("管理员".equals(role)) {
            return new Admin(username, password);
        } else {
            return new NormalUser(username, password);
        }
    }
}
