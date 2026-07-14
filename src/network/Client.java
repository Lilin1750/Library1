package network;

import model.Book;
import model.BorrowRecord;
import model.User;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Client implements Closeable {

    private static final Charset CHARSET = Protocol.CHARSET;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;

    public Client(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), CHARSET));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), CHARSET), true);
        reader.readLine();
    }

    public synchronized ReturnResult send(String request) {
        writer.println(request);
        writer.flush();
        try {
            return Protocol.parseResponse(reader);
        } catch (IOException e) {
            return ReturnResult.error("通信异常：" + e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    // ==================== 高层业务方法 ====================

    public ReturnResult login(String username, String password) {
        return send(Protocol.packLogin(username, password));
    }

    public ReturnResult register(String username, String password, String role) {
        return send(Protocol.packRegister(username, password, role));
    }

    public ReturnResult addBook(String name, String author, double price) {
        return send(Protocol.packAddBook(name, author, price));
    }

    public ReturnResult deleteBookByName(String bookname) {
        return send(Protocol.packDeleteBookByName(bookname));
    }

    public ReturnResult deleteBookById(int id) {
        return send(Protocol.packDeleteBookById(id));
    }

    public ReturnResult updateBook(int id, String name, String author, double price) {
        return send(Protocol.packUpdateBook(id, name, author, price));
    }

    public ReturnResult searchBooks(String keyword) {
        return send(Protocol.packSearchBooks(keyword));
    }

    public ReturnResult listBooks() {
        return send(Protocol.packListBooks());
    }

    public ReturnResult findBookById(int id) {
        return send(Protocol.packFindBookById(id));
    }

    public ReturnResult borrowBook(String username, int bookId) {
        return send(Protocol.packBorrowBook(username, bookId));
    }

    public ReturnResult returnBook(String username, int recordId) {
        return send(Protocol.packReturnBook(username, recordId));
    }

    public ReturnResult getUnreturnedRecords(String username) {
        return send(Protocol.packGetUnreturnedRecords(username));
    }

    public ReturnResult getBorrowRecords(String username) {
        return send(Protocol.packGetBorrowRecords(username));
    }

    // ==================== 响应数据转换 ====================

    public static List<Book> toBookList(ReturnResult result) {
        List<Book> books = new ArrayList<>();
        for (List<String> row : result.getRows()) {
            Book book = new Book();
            book.setId(Integer.parseInt(row.get(0)));
            book.setBookname(row.get(1));
            book.setAuthor(row.get(2));
            book.setPrice(Double.parseDouble(row.get(3)));
            book.setStock(Integer.parseInt(row.get(4)));
            books.add(book);
        }
        return books;
    }

    public static List<BorrowRecord> toBorrowRecordList(ReturnResult result) {
        List<BorrowRecord> records = new ArrayList<>();
        for (List<String> row : result.getRows()) {
            BorrowRecord r = new BorrowRecord();
            r.setId(Integer.parseInt(row.get(0)));
            r.setBookId(Integer.parseInt(row.get(1)));
            r.setBorrowDate("".equals(row.get(2)) ? null : LocalDateTime.parse(row.get(2), DTF));
            r.setReturnDate("".equals(row.get(3)) ? null : LocalDateTime.parse(row.get(3), DTF));
            records.add(r);
        }
        return records;
    }
}
