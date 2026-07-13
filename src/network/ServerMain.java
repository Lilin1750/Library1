package network;

import dao.*;
import service.BookService;
import service.BorrowService;
import service.UserService;

public class ServerMain {
    public static void main(String[] args) {
        System.out.println("===== 图书管理系统 - 服务端 =====");

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

        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8888;
        Server server = new Server(port, bookService, userService, borrowService);
        server.start();
    }
}
