package dao;

import model.Book;
import java.util.List;

public interface BookDao {
    void addBook(Book book);

    void deleteBook(int id);

    void updateBook(Book book);

    Book findBookById(int id);

    Book findBookByName(String bookname);

    List<Book> findAllBooks();

    List<Book> searchBooks(String keyword);

    boolean exists(String bookname, String author, double price);

    int getStock(int bookId);

    void decreaseStock(int bookId);

    void increaseStock(int bookId, int amount);
}
