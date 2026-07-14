package dao;

import model.Book;
import exception.BookNotFoundException;

import java.util.*;

public class ArrayListBookDao implements BookDao {
    private final TreeMap<Integer, Book> books = new TreeMap<>();
    private final TreeSet<Integer> usedIds = new TreeSet<>();

    @Override
    public void addBook(Book book) {
        if (book.getId() <= 0) {
            int newId = 1;
            for (int id : usedIds) {
                if (id == newId) {
                    newId++;
                } else {
                    break;
                }
            }
            book.setId(newId);
        }
        books.put(book.getId(), book);
        usedIds.add(book.getId());
    }

    @Override
    public void deleteBook(int id) {
        if (!books.containsKey(id)) {
            throw new BookNotFoundException("未找到编号为 " + id + " 的图书！");
        }
        books.remove(id);
        usedIds.remove(id);
    }

    @Override
    public void updateBook(Book book) {
        if (!books.containsKey(book.getId())) {
            throw new BookNotFoundException("未找到编号为 " + book.getId() + " 的图书！");
        }
        books.put(book.getId(), book);
    }

    @Override
    public Book findBookById(int id) {
        Book book = books.get(id);
        if (book == null) {
            throw new BookNotFoundException("未找到编号为 " + id + " 的图书！");
        }
        return book;
    }

    @Override
    public Book findBookByName(String bookname) {
        for (Book book : books.values()) {
            if (book.getBookname().equals(bookname)) {
                return book;
            }
        }
        throw new BookNotFoundException("未找到书名为「" + bookname + "」的图书！");
    }

    @Override
    public List<Book> findAllBooks() {
        return new ArrayList<>(books.values());
    }

    @Override
    public List<Book> searchBooks(String keyword) {
        List<Book> result = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        for (Book book : books.values()) {
            if (book.getBookname().toLowerCase().contains(lowerKeyword)
                    || book.getAuthor().toLowerCase().contains(lowerKeyword)) {
                result.add(book);
            }
        }
        return result;
    }

    @Override
    public boolean exists(String bookname, String author, double price) {
        for (Book book : books.values()) {
            if (book.getBookname().equals(bookname)
                    && book.getAuthor().equals(author)
                    && book.getPrice() == price) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getStock(int bookId) {
        return findBookById(bookId).getStock();
    }

    @Override
    public void decreaseStock(int bookId) {
        Book book = findBookById(bookId);
        if (book.getStock() > 0) {
            book.setStock(book.getStock() - 1);
        }
    }

    @Override
    public void increaseStock(int bookId, int amount) {
        Book book = findBookById(bookId);
        book.setStock(book.getStock() + amount);
    }
}
