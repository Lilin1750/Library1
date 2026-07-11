package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BorrowRecord {
    private int id;
    private String username;
    private int bookId;
    private LocalDateTime borrowDate;
    private LocalDateTime returnDate;

    public BorrowRecord() {
    }

    public BorrowRecord(int id, String username, int bookId, LocalDateTime borrowDate, LocalDateTime returnDate) {
        this.id = id;
        this.username = username;
        this.bookId = bookId;
        this.borrowDate = borrowDate;
        this.returnDate = returnDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public LocalDateTime getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDateTime borrowDate) { this.borrowDate = borrowDate; }

    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }

    public boolean isReturned() {
        return returnDate != null;
    }

    @Override
    public String toString() {
        String formatter = "yyyy-MM-dd HH:mm:ss";
        String borrow = borrowDate != null ? borrowDate.format(DateTimeFormatter.ofPattern(formatter)) : "";
        String ret = returnDate != null ? returnDate.format(DateTimeFormatter.ofPattern(formatter)) : "未归还";
        return "借阅记录 [编号：" + id + ", 用户：" + username + ", 图书编号：" + bookId
                + ", 借阅时间：" + borrow + ", 归还时间：" + ret + "]";
    }
}
