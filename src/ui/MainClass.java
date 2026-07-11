package ui;

import controller.LibraryController;
import dao.*;
import service.BookService;
import service.BorrowService;
import service.UserService;
import view.LibraryView;

import javax.swing.*;

public class MainClass {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            BookDao sqlBookDao = new SqlBookDaoImpl();
            UserDao sqlUserDao = new SqlUserDaoImpl();
            BorrowRecordDao borrowRecordDao = new SqlBorrowRecordDaoImpl();

            ArrayListBookDao memBookCache = new ArrayListBookDao();
            FileBookDao fileBookCache = new FileBookDao("data/books_cache.txt");
            BookDao bookDao = new CachingBookDao(sqlBookDao, memBookCache, fileBookCache);

            ArrayListUserDao memUserCache = new ArrayListUserDao();
            FileUserDao fileUserCache = new FileUserDao("data/users_cache.txt");
            UserDao userDao = new CachingUserDao(sqlUserDao, memUserCache, fileUserCache);

            BookService bookService = new BookService(bookDao);
            UserService userService = new UserService(userDao);
            BorrowService borrowService = new BorrowService(bookDao, userDao, borrowRecordDao);

            LibraryView view = new LibraryView();
            new LibraryController(view, bookService, userService, borrowService);
            view.setVisible(true);
        });
    }
}