package dao;

import model.Book;
import exception.BookNotFoundException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileBookDao implements BookDao {
    private final List<Book> books = new ArrayList<>();
    private int nextId = 1;
    private final String filePath;

    public FileBookDao(String filePath) {
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
                int id = Integer.parseInt(parts[0]);
                String bookname = parts[1];
                String author = parts[2];
                int price = Integer.parseInt(parts[3]);
                int stock = parts.length > 4 ? Integer.parseInt(parts[4]) : 1;

                Book book = new Book();
                book.setId(id);
                book.setBookname(bookname);
                book.setAuthor(author);
                book.setPrice(price / 100.0);
                book.setStock(stock);
                books.add(book);

                if (id >= nextId) {
                    nextId = id + 1;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取图书数据失败：" + e.getMessage(), e);
        }
    }

    private void saveToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Book book : books) {
                writer.write(book.getId() + "|" + book.getBookname() + "|"
                        + book.getAuthor() + "|" + (int)(book.getPrice() * 100)
                        + "|" + book.getStock());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("保存图书数据失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void addBook(Book book) {
        book.setId(nextId++);
        books.add(book);
        saveToFile();
    }

    @Override
    public void deleteBook(int id) {
        Book book = findBookById(id);
        books.remove(book);
        saveToFile();
    }

    @Override
    public void updateBook(Book book) {
        Book existing = findBookById(book.getId());
        existing.setBookname(book.getBookname());
        existing.setAuthor(book.getAuthor());
        existing.setPrice(book.getPrice());
        saveToFile();
    }

    @Override
    public Book findBookById(int id) {
        for (Book book : books) {
            if (book.getId() == id) {
                return book;
            }
        }
        throw new BookNotFoundException("未找到编号为 " + id + " 的图书！");
    }

    @Override
    public Book findBookByName(String bookname) {
        for (Book book : books) {
            if (book.getBookname().equals(bookname)) {
                return book;
            }
        }
        throw new BookNotFoundException("未找到书名为「" + bookname + "」的图书！");
    }

    @Override
    public List<Book> findAllBooks() {
        return new ArrayList<>(books);
    }

    @Override
    public List<Book> searchBooks(String keyword) {
        List<Book> result = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        for (Book book : books) {
            if (book.getBookname().toLowerCase().contains(lowerKeyword)
                    || book.getAuthor().toLowerCase().contains(lowerKeyword)) {
                result.add(book);
            }
        }
        return result;
    }

    @Override
    public boolean exists(String bookname, String author, double price) {
        for (Book book : books) {
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
        Book book = findBookById(bookId);
        return book.getStock();
    }

    @Override
    public void decreaseStock(int bookId) {
        Book book = findBookById(bookId);
        if (book.getStock() > 0) {
            book.setStock(book.getStock() - 1);
        }
        saveToFile();
    }

    @Override
    public void increaseStock(int bookId, int amount) {
        Book book = findBookById(bookId);
        book.setStock(book.getStock() + amount);
        saveToFile();
    }
}

