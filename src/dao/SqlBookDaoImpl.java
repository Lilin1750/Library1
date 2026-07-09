package dao;

import model.Book;
import exception.BookNotFoundException;
import exception.BookDuplicateException;
import exception.InvalidPriceException;

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

            if (book.getPrice() < 0) {
                throw new InvalidPriceException("价格不能为负数！");
            }

            String sql = "INSERT INTO book (bookname, author, price) VALUES (?, ?, ?)";
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, book.getBookname());
            ps.setString(2, book.getAuthor());
            ps.setInt(3, (int) (book.getPrice() * 100));
            ps.executeUpdate();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                book.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("添加图书失败！", e);
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

            Book book = findBookById(id);

            String sql = "DELETE FROM book WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("删除图书失败！", e);
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

            if (book.getPrice() < 0) {
                throw new InvalidPriceException("价格不能为负数！");
            }

            String sql = "UPDATE book SET bookname = ?, author = ?, price = ? WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, book.getBookname());
            ps.setString(2, book.getAuthor());
            ps.setInt(3, (int) (book.getPrice() * 100));
            ps.setInt(4, book.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("更新图书失败！", e);
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
            String sql = "SELECT id, bookname, author, price FROM book WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            rs = ps.executeQuery();

            if (rs.next()) {
                return mapRowToBook(rs);
            }
            throw new BookNotFoundException("未找到编号为 " + id + " 的图书！");
        } catch (SQLException e) {
            throw new RuntimeException("查询图书失败！", e);
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
            String sql = "SELECT id, bookname, author, price FROM book WHERE bookname = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, bookname);
            rs = ps.executeQuery();

            if (rs.next()) {
                return mapRowToBook(rs);
            }
            throw new BookNotFoundException("未找到书名为「" + bookname + "」的图书！");
        } catch (SQLException e) {
            throw new RuntimeException("查询图书失败！", e);
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
            String sql = "SELECT id, bookname, author, price FROM book";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                books.add(mapRowToBook(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询所有图书失败！", e);
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
            throw new RuntimeException("检查图书是否存在失败！", e);
        } finally {
            DBUtil.close(rs, ps, conn);
        }
    }

    private Book mapRowToBook(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String bookname = rs.getString("bookname");
        String author = rs.getString("author");
        int priceCents = rs.getInt("price");
        return new Book(id, bookname, author, priceCents / 100.0);
    }
}
