package service;

import dao.BookDao;
import model.Book;
import exception.BookDuplicateException;
import exception.InvalidPriceException;

import java.util.List;

public class BookService {
    private final BookDao bookDao;

    public BookService(BookDao bookDao) {
        this.bookDao = bookDao;
    }

    public void addBook(String bookname, String author, double price) {
        if (bookname == null || bookname.trim().isEmpty()) {
            throw new InvalidPriceException("书名不能为空！");
        }
        if (author == null || author.trim().isEmpty()) {
            throw new InvalidPriceException("作者不能为空！");
        }
        if (price < 0) {
            throw new InvalidPriceException("价格不能为负数！");
        }
        if (bookDao.exists(bookname, author, price)) {
            throw new BookDuplicateException("书名「" + bookname + "」已存在，不能重复添加！");
        }
        Book book = new Book(0, bookname, author, price);
        bookDao.addBook(book);
    }

    public void deleteBookById(int id) {
        bookDao.findBookById(id);
        bookDao.deleteBook(id);
    }

    public void deleteBookByName(String bookname) {
        Book book = bookDao.findBookByName(bookname);
        bookDao.deleteBook(book.getId());
    }

    public void updateBook(int id, String bookname, String author, double price) {
        Book book = bookDao.findBookById(id);
        if (bookname != null && !bookname.trim().isEmpty()) {
            book.setBookname(bookname);
        }
        if (author != null && !author.trim().isEmpty()) {
            book.setAuthor(author);
        }
        if (price >= 0) {
            book.setPrice(price);
        }
        bookDao.updateBook(book);
    }

    public Book findBookById(int id) {
        return bookDao.findBookById(id);
    }

    public Book findBookByName(String bookname) {
        return bookDao.findBookByName(bookname);
    }

    public List<Book> findAllBooks() {
        return bookDao.findAllBooks();
    }
}
