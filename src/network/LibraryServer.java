package network;

import model.Book;
import model.BorrowRecord;
import service.BookService;
import service.BorrowService;
import service.UserService;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class LibraryServer {

    private final int port;
    private final BookService bookService;
    private final UserService userService;
    private final BorrowService borrowService;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public LibraryServer(int port, BookService bookService,
                         UserService userService, BorrowService borrowService) {
        this.port = port;
        this.bookService = bookService;
        this.userService = userService;
        this.borrowService = borrowService;
    }

    public void start() {
        running = true;
        Thread listenThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("[网络服务] 已启动，监听端口: " + port);
                while (running) {
                    Socket client = serverSocket.accept();
                    System.out.println("[网络服务] 新客户端连接: " + client.getRemoteSocketAddress());
                    Thread clientThread = new Thread(() -> handleClient(client));
                    clientThread.setDaemon(true);
                    clientThread.start();
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("[网络服务] 启动失败: " + e.getMessage());
                }
            }
        });
        listenThread.setDaemon(true);
        listenThread.start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private void handleClient(Socket client) {
        System.out.println("[网络服务] 开始处理客户端: " + client.getRemoteSocketAddress());
        try (InputStream is = client.getInputStream();
             OutputStream os = client.getOutputStream();
             PrintWriter out = new PrintWriter(new OutputStreamWriter(os, "GBK"), true)) {

            out.println("欢迎连接图书管理系统网络服务！输入 help 查看可用命令。");
            out.flush();
            System.out.println("[网络服务] 已发送欢迎消息");

            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) != -1) {
                String cmd = new String(buf, 0, len, "GBK").trim();
                if (cmd.isEmpty()) continue;

                System.out.println("[收到命令] " + cmd);
                String response = executeCommand(cmd);
                out.print(response + "\r\n");
                out.flush();
                System.out.println("[已回复客户端]");

                if ("quit".equalsIgnoreCase(cmd)) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("[网络服务] 客户端断开: " + e.getMessage());
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    private String executeCommand(String raw) {
        String[] parts = raw.split(",", -1);
        String cmd = parts[0].trim().toLowerCase();

        try {
            switch (cmd) {
                case "help":
                    return "可用命令:\n"
                            + "  add,书名,作者,价格         - 添加图书\n"
                            + "  delete,书名               - 删除图书\n"
                            + "  update,编号,书名,作者,价格  - 修改图书\n"
                            + "  search,关键词             - 搜索图书\n"
                            + "  list                      - 列出所有图书\n"
                            + "  borrow,用户名,图书编号     - 借阅图书\n"
                            + "  return,用户名,记录编号     - 归还图书\n"
                            + "  register,用户名,密码,角色  - 注册用户\n"
                            + "  login,用户名,密码         - 验证登录\n"
                            + "  quit                      - 断开连接";

                case "add":
                    return doAdd(parts);

                case "delete":
                    return doDelete(parts);

                case "update":
                    return doUpdate(parts);

                case "search":
                    return doSearch(parts);

                case "list":
                    return doList();

                case "borrow":
                    return doBorrow(parts);

                case "return":
                    return doReturn(parts);

                case "register":
                    return doRegister(parts);

                case "login":
                    return doLogin(parts);

                case "quit":
                    return "再见！";

                default:
                    return "错误: 未知命令 '" + cmd + "'，输入 help 查看帮助。";
            }
        } catch (Exception e) {
            return "错误: " + e.getMessage();
        }
    }

    // ==================== 命令实现 ====================

    private String doAdd(String[] parts) {
        if (parts.length < 4) return "错误: 格式应为 add,书名,作者,价格";
        String name = parts[1].trim();
        String author = parts[2].trim();
        double price = Double.parseDouble(parts[3].trim());
        bookService.addBook(name, author, price);
        return "成功: 图书「" + name + "」添加成功！";
    }

    private String doDelete(String[] parts) {
        if (parts.length < 2) return "错误: 格式应为 delete,书名";
        String name = parts[1].trim();
        bookService.deleteBookByName(name);
        return "成功: 图书「" + name + "」删除成功！";
    }

    private String doUpdate(String[] parts) {
        if (parts.length < 5) return "错误: 格式应为 update,编号,书名,作者,价格";
        int id = Integer.parseInt(parts[1].trim());
        String name = parts[2].trim().isEmpty() ? null : parts[2].trim();
        String author = parts[3].trim().isEmpty() ? null : parts[3].trim();
        String priceStr = parts[4].trim();
        double price = priceStr.isEmpty() ? -1 : Double.parseDouble(priceStr);
        bookService.updateBook(id, name, author, price);
        return "成功: 图书编号 " + id + " 修改成功！";
    }

    private String doSearch(String[] parts) {
        if (parts.length < 2) return "错误: 格式应为 search,关键词";
        String keyword = parts[1].trim();
        List<Book> books = bookService.searchBooks(keyword);
        if (books.isEmpty()) return "未找到匹配的图书。";
        return formatBookList(books);
    }

    private String doList() {
        List<Book> books = bookService.findAllBooks();
        if (books.isEmpty()) return "当前没有图书。";
        return formatBookList(books);
    }

    private String doBorrow(String[] parts) {
        if (parts.length < 3) return "错误: 格式应为 borrow,用户名,图书编号";
        String username = parts[1].trim();
        int bookId = Integer.parseInt(parts[2].trim());
        borrowService.borrowBook(username, bookId);
        return "成功: 用户「" + username + "」借阅图书编号 " + bookId + " 成功！";
    }

    private String doReturn(String[] parts) {
        if (parts.length < 3) return "错误: 格式应为 return,用户名,记录编号";
        String username = parts[1].trim();
        int recordId = Integer.parseInt(parts[2].trim());
        borrowService.returnBook(username, recordId);
        return "成功: 记录编号 " + recordId + " 归还成功！";
    }

    private String doRegister(String[] parts) {
        if (parts.length < 4) return "错误: 格式应为 register,用户名,密码,角色(user/admin)";
        String username = parts[1].trim();
        String password = parts[2].trim();
        String role = parts[3].trim();
        userService.register(username, password, role);
        return "成功: 用户「" + username + "」注册成功！";
    }

    private String doLogin(String[] parts) {
        if (parts.length < 3) return "错误: 格式应为 login,用户名,密码";
        String username = parts[1].trim();
        String password = parts[2].trim();
        userService.login(username, password);
        return "成功: 用户「" + username + "」登录验证通过！";
    }

    // ==================== 工具方法 ====================

    private String formatBookList(List<Book> books) {
        StringBuilder sb = new StringBuilder();
        sb.append("共 ").append(books.size()).append(" 本图书:\n");
        sb.append(String.format("%-6s %-20s %-15s %-10s %-6s%n", "编号", "书名", "作者", "价格", "库存"));
        sb.append("-".repeat(60)).append("\n");
        for (Book book : books) {
            sb.append(String.format("%-6d %-20s %-15s %-10.2f %-6d%n",
                    book.getId(), book.getBookname(), book.getAuthor(),
                    book.getPrice(), book.getStock()));
        }
        return sb.toString();
    }
}
