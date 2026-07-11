package service;

import dao.BookDao;
import dao.BorrowRecordDao;
import dao.UserDao;
import model.Book;
import model.BorrowRecord;
import model.User;
import exception.StockInsufficientException;
import exception.AdminBorrowNotAllowedException;
import exception.BorrowLimitExceededException;
import exception.UserNotFoundException;

import java.util.List;

public class BorrowService {
    private final BookDao bookDao;
    private final UserDao userDao;
    private final BorrowRecordDao borrowRecordDao;

    public BorrowService(BookDao bookDao, UserDao userDao, BorrowRecordDao borrowRecordDao) {
        this.bookDao = bookDao;
        this.userDao = userDao;
        this.borrowRecordDao = borrowRecordDao;
    }

    public void borrowBook(String username, int bookId) {
        User user;
        try {
            user = userDao.findByUsername(username);
        } catch (UserNotFoundException e) {
            throw new UserNotFoundException("用户「" + username + "」不存在！");
        }

        if (!user.canBorrowBook()) {
            throw new AdminBorrowNotAllowedException("管理员不能借阅图书！");
        }

        Book book = bookDao.findBookById(bookId);

        if (bookDao.getStock(bookId) <= 0) {
            throw new StockInsufficientException("图书「" + book.getBookname() + "」库存不足，无法借阅！");
        }

        int currentCount = borrowRecordDao.getBorrowCount(username);
        if (currentCount >= user.getMaxBorrowCount()) {
            throw new BorrowLimitExceededException("您已达到最大借阅数量（" + user.getMaxBorrowCount() + " 本），无法继续借阅！");
        }

        borrowRecordDao.borrowBook(username, bookId);
    }

    public void returnBook(String username, int recordId) {
        borrowRecordDao.returnBook(recordId);
    }

    public List<BorrowRecord> getUnreturnedRecords(String username) {
        return borrowRecordDao.findUnreturnedByUsername(username);
    }

    public List<BorrowRecord> getBorrowRecords(String username) {
        return borrowRecordDao.findByUsername(username);
    }
}
