package dao;

import model.Book;
import exception.BookNotFoundException;
import exception.BookDuplicateException;
import exception.DataAccessException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqlBookDaoImpl implements BookDao {

    @Override
    public void addBook(Book book) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();

            if (exists(book.getBookname(), book.getAuthor(), book.getPrice())) {
                throw new BookDuplicateException("书名「" + book.getBookname() + "」已存在，不能重复添加！");
            }

            String sql = "INSERT INTO book (bookname, author, price, stock) VALUES (?, ?, ?, ?)";
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, book.getBookname());
            ps.setString(2, book.getAuthor());
            ps.setInt(3, (int) (book.getPrice() * 100));
            ps.setInt(4, book.getStock());
            ps.executeUpdate();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                book.setId(rs.getInt(1));
            }
        } catch (BookDuplicateException e) {
            throw e;
        } catch (SQLException e) {
            throw new DataAccessException("添加图书失败！", e);
        } finally {
            DBUtil.close(rs, ps, conn);
        }
    }

    @Override
    public void deleteBook(int id) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBUtil.getConnection();
            findBookById(id);

            String sql = "DELETE FROM book WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (BookNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DataAccessException("删除图书失败！", e);
        } finally {
            DBUtil.close(ps, conn);
        }
    }

    @Override
    public void updateBook(Book book) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBUtil.getConnection();
            findBookById(book.getId());

            String sql = "UPDATE book SET bookname = ?, author = ?, price = ? WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, book.getBookname());
            ps.setString(2, book.getAuthor());
            ps.setInt(3, (int) (book.getPrice() * 100));
            ps.setInt(4, book.getId());
            ps.executeUpdate();
        } catch (BookNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DataAccessException("更新图书失败！", e);
        } finally {
            DBUtil.close(ps, conn);
        }
    }

    @Override
    public Book findBookById(int id) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT id, bookname, author, price, stock FROM book WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            rs = ps.executeQuery();

            if (rs.next()) {
                return mapRowToBook(rs);
            }
            throw new BookNotFoundException("未找到编号为 " + id + " 的图书！");
        } catch (BookNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DataAccessException("查询图书失败！", e);
        } finally {
            DBUtil.close(rs, ps, conn);
        }
    }

    @Override
    public Book findBookByName(String bookname) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT id, bookname, author, price, stock FROM book WHERE bookname = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, bookname);
            rs = ps.executeQuery();

            if (rs.next()) {
                return mapRowToBook(rs);
            }
            throw new BookNotFoundException("未找到书名为「" + bookname + "」的图书！");
        } catch (BookNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DataAccessException("查询图书失败！", e);
        } finally {
            DBUtil.close(rs, ps, conn);
        }
    }

    @Override
    public List<Book> findAllBooks() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Book> books = new ArrayList<>();
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT id, bookname, author, price, stock FROM book";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                books.add(mapRowToBook(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询所有图书失败！", e);
        } finally {
            DBUtil.close(rs, ps, conn);
        }
        return books;
    }

    @Override
    public List<Book> searchBooks(String keyword) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Book> books = new ArrayList<>();
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT id, bookname, author, price, stock FROM book WHERE bookname LIKE ? OR author LIKE ?";
            ps = conn.prepareStatement(sql);
            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            rs = ps.executeQuery();

            while (rs.next()) {
                books.add(mapRowToBook(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("搜索图书失败！", e);
        } finally {
            DBUtil.close(rs, ps, conn);
        }
        return books;
    }

    @Override
    public boolean exists(String bookname, String author, double price) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT COUNT(*) FROM book WHERE bookname = ? AND author = ? AND price = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, bookname);
            ps.setString(2, author);
            ps.setInt(3, (int) (price * 100));
            rs = ps.executeQuery();

            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new DataAccessException("检查图书是否存在失败！", e);
        } finally {
            DBUtil.close(rs, ps, conn);
        }
    }

    @Override
    public int getStock(int bookId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT stock FROM book WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, bookId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("stock");
            }
            return 0;
        } catch (SQLException e) {
            throw new DataAccessException("查询库存失败！", e);
        } finally {
            DBUtil.close(rs, ps, conn);
        }
    }

    @Override
    public void decreaseStock(int bookId) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "UPDATE book SET stock = stock - 1 WHERE id = ? AND stock > 0";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, bookId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("扣减库存失败！", e);
        } finally {
            DBUtil.close(ps, conn);
        }
    }

    @Override
    public void increaseStock(int bookId, int amount) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "UPDATE book SET stock = stock + ? WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, amount);
            ps.setInt(2, bookId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("增加库存失败！", e);
        } finally {
            DBUtil.close(ps, conn);
        }
    }

    private Book mapRowToBook(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String bookname = rs.getString("bookname");
        String author = rs.getString("author");
        int priceCents = rs.getInt("price");
        int stock = rs.getInt("stock");
        Book book = new Book(id, bookname, author, priceCents / 100.0);
        book.setStock(stock);
        return book;
    }
}
