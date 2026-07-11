package dao;

import model.Book;
import exception.BookNotFoundException;
import exception.DataAccessException;

import java.util.ArrayList;
import java.util.List;

public class CachingBookDao implements BookDao {

    private final BookDao sqlDao;
    private final ArrayListBookDao memoryCache;
    private final FileBookDao fileCache;
    private boolean memoryLoaded = false;

    public CachingBookDao(BookDao sqlDao, ArrayListBookDao memoryCache, FileBookDao fileCache) {
        this.sqlDao = sqlDao;
        this.memoryCache = memoryCache;
        this.fileCache = fileCache;
    }

    private void ensureMemoryLoaded() {
        if (!memoryLoaded) {
            try {
                List<Book> books = fileCache.findAllBooks();
                for (Book b : books) {
                    memoryCache.addBook(b);
                }
            } catch (Exception e) {
                try {
                    List<Book> books = sqlDao.findAllBooks();
                    for (Book b : books) {
                        memoryCache.addBook(b);
                    }
                    syncFileFromMemory();
                } catch (Exception ex) {
                    throw new DataAccessException("加载缓存数据失败！", ex);
                }
            }
            memoryLoaded = true;
        }
    }

    private void syncFileFromMemory() {
        List<Book> all = memoryCache.findAllBooks();
        for (Book b : all) {
            try { fileCache.updateBook(b); } catch (BookNotFoundException e) { fileCache.addBook(b); }
        }
    }

    private Book copyBook(Book src) {
        Book copy = new Book(src.getId(), src.getBookname(), src.getAuthor(), src.getPrice(), src.getStock());
        return copy;
    }

    @Override
    public void addBook(Book book) {
        sqlDao.addBook(book);
        Book cacheCopy = copyBook(book);
        memoryCache.addBook(cacheCopy);
        fileCache.addBook(copyBook(book));
    }

    @Override
    public void deleteBook(int id) {
        sqlDao.deleteBook(id);
        try { memoryCache.deleteBook(id); } catch (Exception ignored) {}
        try { fileCache.deleteBook(id); } catch (Exception ignored) {}
    }

    @Override
    public void updateBook(Book book) {
        sqlDao.updateBook(book);
        try {
            memoryCache.updateBook(book);
        } catch (BookNotFoundException e) {
            memoryCache.addBook(copyBook(book));
        }
        try {
            fileCache.updateBook(book);
        } catch (BookNotFoundException e) {
            fileCache.addBook(copyBook(book));
        }
    }

    @Override
    public Book findBookById(int id) {
        ensureMemoryLoaded();
        try {
            return memoryCache.findBookById(id);
        } catch (BookNotFoundException e) {
            Book book = sqlDao.findBookById(id);
            memoryCache.addBook(copyBook(book));
            try { fileCache.addBook(copyBook(book)); } catch (Exception ignored) {}
            return book;
        }
    }

    @Override
    public Book findBookByName(String bookname) {
        ensureMemoryLoaded();
        try {
            return memoryCache.findBookByName(bookname);
        } catch (BookNotFoundException e) {
            Book book = sqlDao.findBookByName(bookname);
            memoryCache.addBook(copyBook(book));
            try { fileCache.addBook(copyBook(book)); } catch (Exception ignored) {}
            return book;
        }
    }

    @Override
    public List<Book> findAllBooks() {
        ensureMemoryLoaded();
        List<Book> all = memoryCache.findAllBooks();
        if (!all.isEmpty()) return all;
        List<Book> books = sqlDao.findAllBooks();
        for (Book b : books) {
            memoryCache.addBook(copyBook(b));
        }
        syncFileFromMemory();
        return memoryCache.findAllBooks();
    }

    @Override
    public List<Book> searchBooks(String keyword) {
        ensureMemoryLoaded();
        return memoryCache.searchBooks(keyword);
    }

    @Override
    public boolean exists(String bookname, String author, double price) {
        ensureMemoryLoaded();
        return memoryCache.exists(bookname, author, price);
    }

    @Override
    public int getStock(int bookId) {
        ensureMemoryLoaded();
        return memoryCache.getStock(bookId);
    }

    @Override
    public void decreaseStock(int bookId) {
        sqlDao.decreaseStock(bookId);
        try { memoryCache.decreaseStock(bookId); } catch (Exception ignored) {}
        try { fileCache.decreaseStock(bookId); } catch (Exception ignored) {}
    }

    @Override
    public void increaseStock(int bookId, int amount) {
        sqlDao.increaseStock(bookId, amount);
        try { memoryCache.increaseStock(bookId, amount); } catch (Exception ignored) {}
        try { fileCache.increaseStock(bookId, amount); } catch (Exception ignored) {}
    }
}
