package network;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dao.*;
import model.Book;
import model.BorrowRecord;
import model.User;
import service.BookService;
import service.BorrowService;
import service.UserService;
import network.JsonUtil;


import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HttpApiServer {

    private static final int DEFAULT_HTTP_PORT = 8080;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final int port;
    private final BookService bookService;
    private final UserService userService;
    private final BorrowService borrowService;
    private HttpServer server;

    public HttpApiServer(int port, BookService bookService,
                         UserService userService, BorrowService borrowService) {
        this.port = port;
        this.bookService = bookService;
        this.userService = userService;
        this.borrowService = borrowService;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/login", this::handleLogin);
        server.createContext("/api/register", this::handleRegister);
        server.createContext("/api/books", this::handleBooks);
        server.createContext("/api/books/search", this::handleSearchBooks);
        server.createContext("/api/borrow", this::handleBorrow);
        server.createContext("/api/return", this::handleReturn);
        server.createContext("/api/records", this::handleRecords);

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("========================================");
        System.out.println("  图书管理系统 HTTP API 已启动");
        System.out.println("========================================");
        System.out.println("  监听端口  : " + port);
        try {
            String lanIp = InetAddress.getLocalHost().getHostAddress();
            System.out.println("  局域网地址: http://" + lanIp + ":" + port + "/api/");
            System.out.println("  (Android 端使用此地址连接)");
        } catch (Exception ignored) {}
        System.out.println("========================================");
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    // ==================== CORS 预检 ====================

    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private boolean handleOptions(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            setCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    // ==================== 路由处理 ====================

    private void handleLogin(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "仅支持 POST 请求");
            return;
        }
        try {
            Map<String, String> body = readJsonBody(exchange);
            String username = body.get("username");
            String password = body.get("password");
            if (username == null || password == null) {
                sendError(exchange, 400, "缺少 username 或 password");
                return;
            }
            User user = userService.login(username, password);
            boolean isAdmin = user.getClass().getSimpleName().equals("Admin");

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("username", user.getUsername());
            data.put("role", user.getRole());
            data.put("isAdmin", isAdmin);
            sendJson(exchange, 200, true, "登录成功", data);
        } catch (Exception e) {
            sendError(exchange, 400, e.getMessage());
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "仅支持 POST 请求");
            return;
        }
        try {
            Map<String, String> body = readJsonBody(exchange);
            String username = body.get("username");
            String password = body.get("password");
            String role = body.getOrDefault("role", "normal");
            if (username == null || password == null) {
                sendError(exchange, 400, "缺少 username 或 password");
                return;
            }
            userService.register(username, password, role);
            sendJson(exchange, 200, true, "注册成功", null);
        } catch (Exception e) {
            sendError(exchange, 400, e.getMessage());
        }
    }

    private void handleBooks(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) return;
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        try {
            switch (method) {
                case "GET": {
                    if (path.matches(".*/api/books/\\d+")) {
                        int id = extractId(path, "/api/books/");
                        Book book = bookService.findBookById(id);
                        sendJson(exchange, 200, true, "查询成功", bookToMap(book));
                    } else {
                        List<Book> books = bookService.findAllBooks();
                        List<Map<String, Object>> list = new ArrayList<>();
                        for (Book b : books) list.add(bookToMap(b));
                        sendJsonList(exchange, 200, "共 " + books.size() + " 本图书", list);
                    }
                    break;
                }
                case "POST": {
                    Map<String, String> body = readJsonBody(exchange);
                    String name = body.get("bookname");
                    String author = body.get("author");
                    double price = Double.parseDouble(body.get("price"));
                    bookService.addBook(name, author, price);
                    sendJson(exchange, 200, true, "添加图书成功", null);
                    break;
                }
                case "PUT": {
                    int id = extractId(path, "/api/books/");
                    Map<String, String> body = readJsonBody(exchange);
                    String name = body.get("bookname");
                    String author = body.get("author");
                    double price = Double.parseDouble(body.get("price"));
                    bookService.updateBook(id, name, author, price);
                    sendJson(exchange, 200, true, "修改图书成功", null);
                    break;
                }
                case "DELETE": {
                    int id = extractId(path, "/api/books/");
                    bookService.deleteBookById(id);
                    sendJson(exchange, 200, true, "删除图书成功", null);
                    break;
                }
                default:
                    sendError(exchange, 405, "不支持的方法: " + method);
            }
        } catch (Exception e) {
            sendError(exchange, 400, e.getMessage());
        }
    }

    private void handleSearchBooks(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "仅支持 GET 请求");
            return;
        }
        try {
            String query = exchange.getRequestURI().getQuery();
            String keyword = getParam(query, "keyword");
            if (keyword == null || keyword.isEmpty()) {
                sendError(exchange, 400, "缺少 keyword 参数");
                return;
            }
            List<Book> books = bookService.searchBooks(keyword);
            List<Map<String, Object>> list = new ArrayList<>();
            for (Book b : books) list.add(bookToMap(b));
            sendJsonList(exchange, 200, "查询到 " + books.size() + " 本图书", list);
        } catch (Exception e) {
            sendError(exchange, 400, e.getMessage());
        }
    }

    private void handleBorrow(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "仅支持 POST 请求");
            return;
        }
        try {
            Map<String, String> body = readJsonBody(exchange);
            String username = body.get("username");
            int bookId = Integer.parseInt(body.get("bookId"));
            borrowService.borrowBook(username, bookId);
            sendJson(exchange, 200, true, "借阅成功", null);
        } catch (Exception e) {
            sendError(exchange, 400, e.getMessage());
        }
    }

    private void handleReturn(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "仅支持 POST 请求");
            return;
        }
        try {
            Map<String, String> body = readJsonBody(exchange);
            String username = body.get("username");
            int recordId = Integer.parseInt(body.get("recordId"));
            borrowService.returnBook(username, recordId);
            sendJson(exchange, 200, true, "归还成功", null);
        } catch (Exception e) {
            sendError(exchange, 400, e.getMessage());
        }
    }

    private void handleRecords(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "仅支持 GET 请求");
            return;
        }
        try {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            String username = getParam(query, "username");
            if (username == null || username.isEmpty()) {
                sendError(exchange, 400, "缺少 username 参数");
                return;
            }

            List<BorrowRecord> records;
            if (path.endsWith("/unreturned")) {
                records = borrowService.getUnreturnedRecords(username);
            } else {
                records = borrowService.getBorrowRecords(username);
            }

            List<Map<String, Object>> list = new ArrayList<>();
            for (BorrowRecord r : records) list.add(recordToMap(r));
            sendJsonList(exchange, 200, "查询成功", list);
        } catch (Exception e) {
            sendError(exchange, 400, e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    private Map<String, String> readJsonBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        return JsonUtil.parseJson(body);
    }

    private void sendJson(HttpExchange exchange, int code, boolean success,
                          String message, Map<String, Object> data) throws IOException {
        setCorsHeaders(exchange);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", success);
        resp.put("message", message);
        if (data != null) resp.put("data", data);

        String json = JsonUtil.toJson(resp);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private void sendJsonList(HttpExchange exchange, int code, String message,
                              List<Map<String, Object>> list) throws IOException {
        setCorsHeaders(exchange);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", message);
        resp.put("data", list);

        String json = JsonUtil.toJson(resp);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        setCorsHeaders(exchange);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", false);
        resp.put("message", message);

        String json = JsonUtil.toJson(resp);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private Map<String, Object> bookToMap(Book b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("bookname", b.getBookname());
        m.put("author", b.getAuthor());
        m.put("price", b.getPrice());
        m.put("stock", b.getStock());
        return m;
    }

    private Map<String, Object> recordToMap(BorrowRecord r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("bookId", r.getBookId());
        m.put("borrowDate", r.getBorrowDate() != null ? r.getBorrowDate().format(DTF) : "");
        m.put("returnDate", r.getReturnDate() != null ? r.getReturnDate().format(DTF) : "");
        return m;
    }

    private int extractId(String path, String prefix) {
        String sub = path.substring(path.indexOf(prefix) + prefix.length());
        int slash = sub.indexOf('/');
        return Integer.parseInt(slash > 0 ? sub.substring(0, slash) : sub);
    }

    private String getParam(String query, String name) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    // ==================== 启动入口 ====================

    public static void main(String[] args) throws IOException {
        System.out.println("===== 图书管理系统 - HTTP API 服务端 =====");

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

        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_HTTP_PORT;
        HttpApiServer httpServer = new HttpApiServer(port, bookService, userService, borrowService);
        httpServer.start();
    }
}
