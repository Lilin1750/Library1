package ui;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import dao.BookDao;
import dao.FileBookDao;
import dao.UserDao;
import dao.FileUserDao;
import model.Book;
import model.User;
import service.BookService;
import service.UserService;
import exception.BookException;
import exception.UserException;

public class MainClass {
    private final BookService bookService;
    private final UserService userService;
    private final Scanner scanner = new Scanner(System.in);
    private User currentUser;

    public MainClass() {
        BookDao bookDao =new FileBookDao("data/books.txt");
        UserDao userDao = new FileUserDao("data/users.txt");
        this.bookService = new BookService(bookDao);
        this.userService = new UserService(userDao);

        showLoginMenu();
    }

    public static void main(String[] args) {
        new MainClass();
    }

    private void showLoginMenu() {
        while (true) {
            System.out.println("\n========== 欢迎使用图书管理系统 ==========");
            System.out.println("登录请选-----------------1");
            System.out.println("注册请选-----------------2");
            System.out.println("退出系统请选-------------0");
            System.out.println("请选择：");

            int choice = scanner.nextInt();
            scanner.nextLine();

            try {
                switch (choice) {
                    case 1:
                        login();
                        break;
                    case 2:
                        register();
                        break;
                    case 0:
                        System.out.println("感谢使用，再见！");
                        System.exit(0);
                        break;
                    default:
                        System.out.println("输入无效，请重新选择！");
                        break;
                }
            } catch (UserException | InputMismatchException e) {
                System.out.println("操作失败：" + e.getMessage());
            }
        }
    }

    private void login() {
        System.out.println("========== 用户登录 ==========");
        System.out.println("请输入用户名：");
        String username = scanner.nextLine();
        System.out.println("请输入密码：");
        String password = scanner.nextLine();

        currentUser = userService.login(username, password);
        System.out.println("登录成功！欢迎，" + currentUser.getRole() + "：" + currentUser.getUsername());
        showMainMenu();
    }

    private void register() {
        System.out.println("========== 用户注册 ==========");
        System.out.println("请输入用户名：");
        String username = scanner.nextLine();
        System.out.println("请输入密码（至少6位）：");
        String password = scanner.nextLine();
        System.out.println("请选择角色（管理员输入admin，普通用户输入user）：");
        String role = scanner.nextLine();

        userService.register(username, password, role);
        System.out.println("注册成功！请登录：");
    }

    private void showMainMenu() {
        while (true) {
            System.out.println("\n========== 图书管理系统 ==========");
            System.out.println("当前用户：" + currentUser.getUsername() + "（" + currentUser.getRole() + "）");
            System.out.println("增加图书请选---------------1");
            System.out.println("删除图书请选---------------2");
            System.out.println("修改图书请选---------------3");
            System.out.println("查询图书请选---------------4");
            System.out.println("查看所有图书请选-----------5");
            System.out.println("退出登录请选---------------0");
            System.out.println("请选择：");

            int choice = scanner.nextInt();
            scanner.nextLine();

            try {
                switch (choice) {
                    case 1:
                        int continueChoice;
                        do {
                            addBook();
                            System.out.println("是否继续添加图书？继续输1：");
                            continueChoice = scanner.nextInt();
                            scanner.nextLine();
                        } while (continueChoice == 1);
                        break;
                    case 2:
                        int continueDelete;
                        do {
                            deleteBook();
                            System.out.println("是否继续删除图书？继续输1：");
                            continueDelete = scanner.nextInt();
                            scanner.nextLine();
                        } while (continueDelete == 1);
                        break;
                    case 3:
                        int continueUpdate;
                        do {
                            updateBook();
                            System.out.println("是否继续修改图书？继续输1：");
                            continueUpdate = scanner.nextInt();
                            scanner.nextLine();
                        } while (continueUpdate == 1);
                        break;
                    case 4:
                        int continueSearch;
                        do {
                            searchBook();
                            System.out.println("是否继续查询图书？继续输1：");
                            continueSearch = scanner.nextInt();
                            scanner.nextLine();
                        } while (continueSearch == 1);
                        break;
                    case 5:
                        listAllBooks();
                        break;
                    case 0:
                        System.out.println("已退出登录！");
                        currentUser = null;
                        showLoginMenu();
                        return;
                    default:
                        System.out.println("输入无效，请重新选择！");
                        break;
                }
            } catch (BookException | InputMismatchException e) {
                System.out.println("操作失败：" + e.getMessage());
            }
        }
    }

    private void addBook() {
        System.out.println("========== 添加图书 ==========");
        System.out.println("请输入书名：");
        String bookName = scanner.nextLine();
        System.out.println("请输入作者：");
        String author = scanner.nextLine();
        System.out.println("请输入价格：");
        double price = scanner.nextDouble();
        scanner.nextLine();

        bookService.addBook(bookName, author, price);
        System.out.println("添加图书成功！");
    }

    private void deleteBook() {
        System.out.println("========== 删除图书 ==========");
        System.out.println("请输入需要删除的书名：");
        String bookname = scanner.nextLine();

        bookService.deleteBookByName(bookname);
        System.out.println("删除图书成功！");
    }

    private void updateBook() {
        System.out.println("========== 修改图书 ==========");
        System.out.println("请输入需要修改的书名：");
        String bookname = scanner.nextLine();

        Book book = bookService.findBookByName(bookname);
        System.out.println("找到图书：" + book);
        System.out.println("修改图书名称请选---1");
        System.out.println("修改图书作者请选---2");
        System.out.println("修改图书价格请选---3");
        System.out.println("请选择：");

        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1:
                System.out.println("请输入新的书名：");
                String newName = scanner.nextLine();
                bookService.updateBook(book.getId(), newName, null, -1);
                break;
            case 2:
                System.out.println("请输入新的作者：");
                String newAuthor = scanner.nextLine();
                bookService.updateBook(book.getId(), null, newAuthor, -1);
                break;
            case 3:
                System.out.println("请输入新的价格：");
                double newPrice = scanner.nextDouble();
                scanner.nextLine();
                bookService.updateBook(book.getId(), null, null, newPrice);
                break;
            default:
                System.out.println("输入错误！");
                return;
        }
        System.out.println("修改图书成功！");
    }

    private void searchBook() {
        System.out.println("========== 查询图书 ==========");
        System.out.println("请输入书名：");
        String bookname = scanner.nextLine();

        Book book = bookService.findBookByName(bookname);
        System.out.println("查询到的图书信息为：");
        System.out.println(book);
    }

    private void listAllBooks() {
        System.out.println("========== 所有图书 ==========");
        List<Book> books = bookService.findAllBooks();
        if (books.isEmpty()) {
            System.out.println("暂无图书！");
            return;
        }
        for (Book book : books) {
            System.out.println(book);
        }
    }
}