package dao;

import model.BorrowRecord;
import exception.BookNotAvailableException;
import exception.BookNotFoundException;
import exception.UserNotFoundException;
import exception.DataAccessException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SqlBorrowRecordDaoImpl implements BorrowRecordDao {

    @Override
    public void borrowBook(String username, int bookId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            String checkUserSql = "SELECT COUNT(*) FROM `user` WHERE username = ?";
            ps = conn.prepareStatement(checkUserSql);
            ps.setString(1, username);
            rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) == 0) {
                conn.rollback();
                throw new UserNotFoundException("用户「" + username + "」不存在！");
            }
            rs.close();
            ps.close();

            String checkBookSql = "SELECT bookname, stock FROM book WHERE id = ?";
            ps = conn.prepareStatement(checkBookSql);
            ps.setInt(1, bookId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                throw new BookNotFoundException("未找到编号为 " + bookId + " 的图书！");
            }
            String bookname = rs.getString("bookname");
            int stock = rs.getInt("stock");
            if (stock <= 0) {
                conn.rollback();
                throw new exception.StockInsufficientException("图书「" + bookname + "」库存不足，无法借阅！");
            }
            rs.close();
            ps.close();

            String decreaseSql = "UPDATE book SET stock = stock - 1 WHERE id = ?";
            ps = conn.prepareStatement(decreaseSql);
            ps.setInt(1, bookId);
            ps.executeUpdate();
            ps.close();

            String insertSql = "INSERT INTO borrow_record (username, book_id, borrow_date) VALUES (?, ?, NOW())";
            ps = conn.prepareStatement(insertSql);
            ps.setString(1, username);
            ps.setInt(2, bookId);
            ps.executeUpdate();

            conn.commit();
        } catch (UserNotFoundException | BookNotFoundException | exception.StockInsufficientException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw e;
        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw new DataAccessException("借阅图书失败！", e);
        } finally {
            DBUtil.close(rs, ps, conn);
        }
    }

    @Override
    public void returnBook(int recordId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            String checkSql = "SELECT book_id, return_date FROM borrow_record WHERE id = ?";
            ps = conn.prepareStatement(checkSql);
            ps.setInt(1, recordId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                throw new BookNotFoundException("未找到编号为 " + recordId + " 的借阅记录！");
            }
            if (rs.getTimestamp("return_date") != null) {
                conn.rollback();
                throw new exception.BookNotAvailableException("该图书已归还，请勿重复操作！");
            }
            int bookId = rs.getInt("book_id");
            rs.close();
            ps.close();

            String updateSql = "UPDATE borrow_record SET return_date = NOW() WHERE id = ?";
            ps = conn.prepareStatement(updateSql);
            ps.setInt(1, recordId);
            ps.executeUpdate();
            ps.close();

            String restoreSql = "UPDATE book SET stock = stock + 1 WHERE id = ?";
            ps = conn.prepareStatement(restoreSql);
            ps.setInt(1, bookId);
            ps.executeUpdate();

            conn.commit();
        } catch (BookNotFoundException | exception.BookNotAvailableException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw e;
        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw new DataAccessException("归还图书失败！", e);
        } finally {
            DBUtil.close(rs, ps, conn);
        }
    }

    @Override
    public int getBorrowCount(String username) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT COUNT(*) FROM borrow_record WHERE username = ? AND return_date IS NULL";
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DataAccessException("查询借阅数量失败！", e);
        } finally {
            DBUtil.close(rs, ps, conn);
        }
    }

    @Override
    public List<BorrowRecord> findByUsername(String username) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<BorrowRecord> records = new ArrayList<>();
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT id, username, book_id, borrow_date, return_date FROM borrow_record WHERE username = ? ORDER BY borrow_date DESC";
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            rs = ps.executeQuery();
            while (rs.next()) {
                records.add(mapRowToRecord(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询借阅记录失败！", e);
        } finally {
            DBUtil.close(rs, ps, conn);
        }
        return records;
    }

    @Override
    public List<BorrowRecord> findUnreturnedByUsername(String username) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<BorrowRecord> records = new ArrayList<>();
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT id, username, book_id, borrow_date, return_date FROM borrow_record WHERE username = ? AND return_date IS NULL ORDER BY borrow_date DESC";
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            rs = ps.executeQuery();
            while (rs.next()) {
                records.add(mapRowToRecord(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询未归还记录失败！", e);
        } finally {
            DBUtil.close(rs, ps, conn);
        }
        return records;
    }

    private BorrowRecord mapRowToRecord(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        int bookId = rs.getInt("book_id");
        Timestamp borrowTs = rs.getTimestamp("borrow_date");
        Timestamp returnTs = rs.getTimestamp("return_date");
        LocalDateTime borrowDate = borrowTs != null ? borrowTs.toLocalDateTime() : null;
        LocalDateTime returnDate = returnTs != null ? returnTs.toLocalDateTime() : null;
        return new BorrowRecord(id, username, bookId, borrowDate, returnDate);
    }

    @Override
    public void deleteByBookId(int bookId) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "DELETE FROM borrow_record WHERE book_id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, bookId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("删除图书的借阅记录失败！", e);
        } finally {
            DBUtil.close(ps, conn);
        }
    }
}
