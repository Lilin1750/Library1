
package network;

import dao.*;
import model.Book;
import model.BorrowRecord;
import model.User;
import service.BookService;
import service.BorrowService;
import service.UserService;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Server {

    private static final int DEFAULT_PORT = 8888;
    private static final Charset CHARSET = Protocol.CHARSET;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final int port;
    private final BookService bookService;
    private final UserService userService;
    private final BorrowService borrowService;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public Server(int port, BookService bookService,
                  UserService userService, BorrowService borrowService) {
        this.port = port;
        this.bookService = bookService;
        this.userService = userService;
        this.borrowService = borrowService;
    }

    public void start() {
        running = true;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("========================================");
            System.out.println("  图书管理系统服务端 已启动");
            System.out.println("========================================");
            System.out.println("  监听端口  : " + port);
            try {
                String lanIp = InetAddress.getLocalHost().getHostAddress();
                System.out.println("  局域网地址: " + lanIp + ":" + port);
                System.out.println("  (Android局域网连接用此地址)");
            } catch (Exception ignored) {}
            System.out.println("========================================");
            while (running) {
                Socket client = serverSocket.accept();
                System.out.println("[服务端] 新客户端连接: " + client.getRemoteSocketAddress());
                Thread t = new Thread(() -> handleClient(client));
                t.setDaemon(true);
                t.start();
            }
        } catch (IOException e) {
            if (running) System.err.println("[服务端] 启动失败: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
    }

    // ==================== 客户端处理 ====================

    private void handleClient(Socket client) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), CHARSET));
             OutputStream os = client.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, CHARSET), true)) {

            writer.println("欢迎连接图书管理系统服务端！");
            writer.flush();

            String line;
            while ((line = reader.readLine()) != null) {
                String cmd = line.trim();
                if (cmd.isEmpty()) continue;
                System.out.println("[收到请求] " + cmd);
                ReturnResult result = dispatch(cmd);
                Protocol.writeResult(writer, result);
                System.out.println("[已响应] success=" + result.isSuccess());
                if ("QUIT".equals(getCommand(cmd))) {
                    client.close();
                    return;
                }
            }
        } catch (IOException e) {
            System.err.println("[服务端] 客户端断开: " + e.getMessage());
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    // ==================== 命令路由 ====================

    private ReturnResult dispatch(String request) {
        String cmd = getCommand(request);
        String[] parts = request.split(",", -1);

        try {
            switch (cmd) {
                case "LOGIN":       return doLogin(parts);
                case "REGISTER":    return doRegister(parts);
                case "ADD":         return doAdd(parts);
                case "DELETE_NAME": return doDeleteByName(parts);
                case "DELETE_ID":   return doDeleteById(parts);
                case "UPDATE":      return doUpdate(parts);
                case "SEARCH":      return doSearch(parts);
                case "LIST":        return doList();
                case "FIND_ID":     return doFindById(parts);
                case "BORROW":      return doBorrow(parts);
                case "RETURN":      return doReturn(parts);
                case "UNRETURNED":  return doUnreturned(parts);
                case "RECORDS":     return doRecords(parts);
                case "QUIT":        return ReturnResult.ok("再见！");
                default:            return ReturnResult.error("未知命令: " + cmd);
            }
        } catch (Exception e) {
            return ReturnResult.error(e.getMessage());
        }
    }

    private String getCommand(String request) {
        int idx = request.indexOf(',');
        return idx > 0 ? request.substring(0, idx).trim().toUpperCase()
                : request.trim().toUpperCase();
    }

    // ==================== 命令实现 ====================

    private ReturnResult doLogin(String[] parts) {
        if (parts.length < 3) return ReturnResult.error("格式: LOGIN,用户名,密码");
        String username = parts[1].trim();
        String password = parts[2].trim();
        User user = userService.login(username, password);
        if (user == null) {
            return ReturnResult.error("用户不存在！");
        }

        String isAdmin = user.getClass().getSimpleName().equals("Admin") ? "true" : "false";
        List<String> cols = Arrays.asList("username", "role", "isAdmin");
        List<String> row = Arrays.asList(user.getUsername(), user.getRole(), isAdmin);

        List<List<String>> rows = new ArrayList<>();
        rows.add(row);
        return ReturnResult.data("登录成功", "USER", cols, rows);
    }

    private ReturnResult doRegister(String[] parts) {
        if (parts.length < 4) return ReturnResult.error("格式: REGISTER,用户名,密码,角色");
        userService.register(parts[1].trim(), parts[2].trim(), parts[3].trim());
        return ReturnResult.ok("注册成功");
    }

    private ReturnResult doAdd(String[] parts) {
        if (parts.length < 4) return ReturnResult.error("格式: ADD,书名,作者,价格");
        bookService.addBook(parts[1].trim(), parts[2].trim(), Double.parseDouble(parts[3].trim()));
        return ReturnResult.ok("添加图书成功");
    }

    private ReturnResult doDeleteByName(String[] parts) {
        if (parts.length < 2) return ReturnResult.error("格式: DELETE_NAME,书名");
        bookService.deleteBookByName(parts[1].trim());
        return ReturnResult.ok("删除图书成功");
    }

    private ReturnResult doDeleteById(String[] parts) {
        if (parts.length < 2) return ReturnResult.error("格式: DELETE_ID,编号");
        bookService.deleteBookById(Integer.parseInt(parts[1].trim()));
        return ReturnResult.ok("删除图书成功");
    }

    private ReturnResult doUpdate(String[] parts) {
        if (parts.length < 5) return ReturnResult.error("格式: UPDATE,编号,书名,作者,价格");
        int id = Integer.parseInt(parts[1].trim());
        String name = parts[2].trim().isEmpty() ? null : parts[2].trim();
        String author = parts[3].trim().isEmpty() ? null : parts[3].trim();
        double price = Double.parseDouble(parts[4].trim());
        bookService.updateBook(id, name, author, price);
        return ReturnResult.ok("修改图书成功");
    }

    private ReturnResult doSearch(String[] parts) {
        if (parts.length < 2) return ReturnResult.error("格式: SEARCH,关键词");
        List<Book> books = bookService.searchBooks(parts[1].trim());
        return bookListResult("查询到 " + books.size() + " 本图书", books);
    }

    private ReturnResult doList() {
        List<Book> books = bookService.findAllBooks();
        return bookListResult("共 " + books.size() + " 本图书", books);
    }

    private ReturnResult doFindById(String[] parts) {
        if (parts.length < 2) return ReturnResult.error("格式: FIND_ID,编号");
        Book book = bookService.findBookById(Integer.parseInt(parts[1].trim()));
        return bookListResult("查询成功", Collections.singletonList(book));
    }

    private ReturnResult doBorrow(String[] parts) {
        if (parts.length < 3) return ReturnResult.error("格式: BORROW,用户名,图书编号");
        borrowService.borrowBook(parts[1].trim(), Integer.parseInt(parts[2].trim()));
        return ReturnResult.ok("借阅成功");
    }

    private ReturnResult doReturn(String[] parts) {
        if (parts.length < 3) return ReturnResult.error("格式: RETURN,用户名,记录编号");
        borrowService.returnBook(parts[1].trim(), Integer.parseInt(parts[2].trim()));
        return ReturnResult.ok("归还成功");
    }

    private ReturnResult doUnreturned(String[] parts) {
        if (parts.length < 2) return ReturnResult.error("格式: UNRETURNED,用户名");
        List<BorrowRecord> records = borrowService.getUnreturnedRecords(parts[1].trim());
        return recordListResult("查询成功", records);
    }

    private ReturnResult doRecords(String[] parts) {
        if (parts.length < 2) return ReturnResult.error("格式: RECORDS,用户名");
        List<BorrowRecord> records = borrowService.getBorrowRecords(parts[1].trim());
        return recordListResult("查询成功", records);
    }

    // ==================== 结果转换工具 ====================

    private ReturnResult bookListResult(String msg, List<Book> books) {
        List<String> cols = Arrays.asList("id", "bookname", "author", "price", "stock");
        List<List<String>> rows = new ArrayList<>();
        for (Book b : books) {
            rows.add(Arrays.asList(
                    String.valueOf(b.getId()),
                    b.getBookname(),
                    b.getAuthor(),
                    String.format("%.2f", b.getPrice()),
                    String.valueOf(b.getStock())
            ));
        }
        return ReturnResult.data(msg, "BOOKS", cols, rows);
    }

    private ReturnResult recordListResult(String msg, List<BorrowRecord> records) {
        List<String> cols = Arrays.asList("id", "bookId", "borrowDate", "returnDate");
        List<List<String>> rows = new ArrayList<>();
        for (BorrowRecord r : records) {
            rows.add(Arrays.asList(
                    String.valueOf(r.getId()),
                    String.valueOf(r.getBookId()),
                    r.getBorrowDate() != null ? r.getBorrowDate().format(DTF) : "",
                    r.getReturnDate() != null ? r.getReturnDate().format(DTF) : ""
            ));
        }
        return ReturnResult.data(msg, "RECORDS", cols, rows);
    }

    // ==================== 服务端启动入口 ====================

    public static void main(String[] args) {
        System.out.println("===== 图书管理系统 - 服务端 =====");

        SqlBorrowRecordDaoImpl borrowRecordDao = new SqlBorrowRecordDaoImpl();
        BookDao sqlBookDao = new SqlBookDaoImpl(borrowRecordDao);
        UserDao sqlUserDao = new SqlUserDaoImpl();

        ArrayListBookDao memBookCache = new ArrayListBookDao();
        FileBookDao fileBookCache = new FileBookDao("data/books_cache.txt");
        BookDao bookDao = new CachingBookDao(sqlBookDao, memBookCache, fileBookCache);

        ArrayListUserDao memUserCache = new ArrayListUserDao();
        FileUserDao fileUserCache = new FileUserDao("data/users_cache.txt");
        UserDao userDao = new CachingUserDao(sqlUserDao, memUserCache, fileUserCache);

        BookService bookService = new BookService(bookDao);
        UserService userService = new UserService(userDao);
        BorrowService borrowService = new BorrowService(bookDao, userDao, borrowRecordDao);
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Server server = new Server(port, bookService, userService, borrowService);
        server.start();
    }
}
