package dao;

import model.BorrowRecord;
import java.util.List;

public interface BorrowRecordDao {
    void borrowBook(String username, int bookId);

    void returnBook(int recordId);

    int getBorrowCount(String username);

    List<BorrowRecord> findByUsername(String username);

    List<BorrowRecord> findUnreturnedByUsername(String username);

    void deleteByBookId(int bookId);
}
