package controller;

import model.Admin;
import model.Book;
import model.BorrowRecord;
import model.User;
import service.BookService;
import service.BorrowService;
import service.UserService;
import view.LibraryView;

import java.util.ArrayList;
import java.util.List;

public class LibraryController implements LibraryView.Listener {

    private final LibraryView view;
    private final BookService bookService;
    private final UserService userService;
    private final BorrowService borrowService;
    private User currentUser;

    public LibraryController(LibraryView view, BookService bookService,
                             UserService userService, BorrowService borrowService) {
        this.view = view;
        this.bookService = bookService;
        this.userService = userService;
        this.borrowService = borrowService;
        view.setListener(this);
    }

    @Override
    public void onLogin(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            view.showError("用户名和密码不能为空！");
            return;
        }
        try {
            currentUser = userService.login(username, password);
            view.showDashboard();
            view.updatePermissions(currentUser.getUsername(), currentUser.getRole(),
                    currentUser instanceof Admin);
            onRefreshBooks();
        } catch (Exception e) {
            view.clearLoginPassword();
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onRegister(String username, String password, String role) {
        if (username.isEmpty() || password.isEmpty()) {
            view.showError("用户名和密码不能为空！");
            return;
        }
        try {
            userService.register(username, password, role);
            view.switchToLoginTab(username);
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onLogout() {
        currentUser = null;
        view.clearBooks();
        view.showLogin();
    }

    @Override
    public void onAddBook(String name, String author, double price) {
        try {
            bookService.addBook(name, author, price);
            view.showInfo("添加图书成功！");
            onRefreshBooks();
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onDeleteBook(String bookname) {
        try {
            bookService.deleteBookByName(bookname);
            view.showInfo("删除图书成功！");
            onRefreshBooks();
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onUpdateBook(int id, String name, String author, double price) {
        try {
            bookService.updateBook(id, name, author, price);
            view.showInfo("修改图书成功！");
            onRefreshBooks();
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onSearch(String keyword) {
        try {
            List<Book> results = searchBooks(keyword);
            if (results.isEmpty()) {
                view.showInfo("未找到匹配的图书！");
                return;
            }
            boolean isAdmin = currentUser instanceof Admin;
            List<Book> selected = view.showSearchResultsAndGetSelection(results, isAdmin);
            if (selected == null) return;

            if (isAdmin) {
                handleAdminAction(selected);
            } else {
                borrowService_loop(selected);
            }
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onBorrowSearch(String keyword) {
        try {
            List<Book> results;
            try {
                int bookId = Integer.parseInt(keyword);
                results = new ArrayList<>();
                results.add(bookService.findBookById(bookId));
            } catch (NumberFormatException e) {
                results = searchBooks(keyword);
            }
            if (results.isEmpty()) {
                view.showInfo("未找到匹配的图书！");
                return;
            }
            List<Book> selected = view.showSearchResultsAndGetSelection(results, false);
            if (selected == null) return;
            borrowService_loop(selected);
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onBorrowBooks(List<Book> books) {
        borrowService_loop(books);
    }

    @Override
    public void onReturnBooks(List<Integer> recordIds) {
        try {
            List<BorrowRecord> unreturned = borrowService.getUnreturnedRecords(currentUser.getUsername());
            if (unreturned.isEmpty()) {
                view.showInfo("您当前没有未归还的图书！");
                return;
            }
            List<Integer> selectedIds = view.showReturnDialog(unreturned);
            if (selectedIds == null) return;

            int success = 0;
            StringBuilder failed = new StringBuilder();
            for (int recordId : selectedIds) {
                try {
                    borrowService.returnBook(currentUser.getUsername(), recordId);
                    success++;
                } catch (Exception e) {
                    failed.append("记录#").append(recordId).append(": ").append(e.getMessage()).append("\n");
                }
            }
            view.showBatchResult("成功归还 " + success + " 本图书！", failed);
            onRefreshBooks();
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onViewRecords() {
        try {
            List<BorrowRecord> records = borrowService.getBorrowRecords(currentUser.getUsername());
            view.displayBorrowRecords(records);
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onRefreshBooks() {
        try {
            view.displayBooks(bookService.findAllBooks());
        } catch (Exception e) {
            view.showError("加载图书列表失败：" + e.getMessage());
        }
    }

    // ==================== 内部方法 ====================

    private List<Book> searchBooks(String keyword) {
        return bookService.searchBooks(keyword);
    }

    private void handleAdminAction(List<Book> selected) {
        if (selected.size() == 1 && !(currentUser instanceof Admin)) {
            borrowService_loop(selected);
            return;
        }

        String action = view.askAdminAction(selected.size());

        if (action == null) return;

        if ("批量删除".equals(action)) {
            if (!view.confirm("确定要删除选中的 " + selected.size() + " 本图书吗？", "确认批量删除")) return;
            int success = 0;
            StringBuilder failed = new StringBuilder();
            for (Book book : selected) {
                try {
                    bookService.deleteBookById(book.getId());
                    success++;
                } catch (Exception e) {
                    failed.append("「").append(book.getBookname()).append("」: ").append(e.getMessage()).append("\n");
                }
            }
            view.showBatchResult("成功删除 " + success + " 本图书！", failed);
            onRefreshBooks();
        } else if ("修改（仅单本）".equals(action)) {
            if (selected.size() != 1) {
                view.showError("修改操作只能对单本图书进行！");
                return;
            }
            Book book = selected.get(0);
            String[] vals = view.askUpdateValues(book);
            if (vals == null) return;
            double newPrice = -1;
            try { newPrice = Double.parseDouble(vals[2]); } catch (NumberFormatException ignored) {}
            onUpdateBook(book.getId(),
                    vals[0].isEmpty() ? null : vals[0],
                    vals[1].isEmpty() ? null : vals[1],
                    newPrice);
        }
    }

    private void borrowService_loop(List<Book> books) {
        int success = 0;
        StringBuilder failed = new StringBuilder();
        for (Book book : books) {
            try {
                borrowService.borrowBook(currentUser.getUsername(), book.getId());
                success++;
            } catch (Exception e) {
                failed.append("「").append(book.getBookname()).append("」: ").append(e.getMessage()).append("\n");
            }
        }
        view.showBatchResult("成功借阅 " + success + " 本图书！", failed);
        onRefreshBooks();
    }
}
