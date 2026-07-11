package dao;

import model.Book;
import model.BorrowRecord;
import exception.BookNotFoundException;
import exception.BookNotAvailableException;
import exception.UserNotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ArrayListBorrowRecordDaoImpl implements BorrowRecordDao {
    private final List<BorrowRecord> records = new ArrayList<>();
    private final ArrayListBookDao bookDao;
    private final ArrayListUserDao userDao;
    private int nextId = 1;

    public ArrayListBorrowRecordDaoImpl(ArrayListBookDao bookDao, ArrayListUserDao userDao) {
        this.bookDao = bookDao;
        this.userDao = userDao;
    }

    @Override
    public synchronized void borrowBook(String username, int bookId) {
        try {
            userDao.findByUsername(username);
        } catch (Exception e) {
            throw new UserNotFoundException("用户「" + username + "」不存在！");
        }

        Book book = bookDao.findBookById(bookId);

        if (bookDao.getStock(bookId) <= 0) {
            throw new BookNotAvailableException("图书「" + book.getBookname() + "」库存不足，无法借阅！");
        }

        bookDao.decreaseStock(bookId);

        BorrowRecord record = new BorrowRecord(nextId++, username, bookId, LocalDateTime.now(), null);
        records.add(record);
    }

    @Override
    public int getBorrowCount(String username) {
        int count = 0;
        for (BorrowRecord record : records) {
            if (record.getUsername().equals(username) && !record.isReturned()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public List<BorrowRecord> findByUsername(String username) {
        List<BorrowRecord> result = new ArrayList<>();
        for (BorrowRecord record : records) {
            if (record.getUsername().equals(username)) {
                result.add(record);
            }
        }
        return result;
    }

    @Override
    public synchronized void returnBook(int recordId) {
        for (BorrowRecord record : records) {
            if (record.getId() == recordId) {
                if (record.isReturned()) {
                    throw new BookNotAvailableException("该图书已归还，请勿重复操作！");
                }
                record.setReturnDate(LocalDateTime.now());
                bookDao.increaseStock(record.getBookId(), 1);
                return;
            }
        }
        throw new BookNotFoundException("未找到编号为 " + recordId + " 的借阅记录！");
    }

    @Override
    public List<BorrowRecord> findUnreturnedByUsername(String username) {
        List<BorrowRecord> result = new ArrayList<>();
        for (BorrowRecord record : records) {
            if (record.getUsername().equals(username) && !record.isReturned()) {
                result.add(record);
            }
        }
        return result;
    }
}
