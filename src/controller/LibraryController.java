package controller;

import model.Book;
import model.BorrowRecord;
import network.Client;
import network.ReturnResult;
import view.LibraryView;

import java.util.ArrayList;
import java.util.List;

public class LibraryController implements LibraryView.Listener {

    private final LibraryView view;
    private final Client client;
    private String currentUsername;
    private String currentRole;
    private boolean isAdmin;

    public LibraryController(LibraryView view, Client client) {
        this.view = view;
        this.client = client;
        view.setListener(this);
    }

    @Override
    public void onLogin(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            view.showError("用户名和密码不能为空！");
            return;
        }
        try {
            ReturnResult result = client.login(username, password);
            result.checkError();

            List<List<String>> rows = result.getRows();
            currentUsername = rows.get(0).get(0);
            currentRole = rows.get(0).get(1);
            isAdmin = "true".equals(rows.get(0).get(2));

            view.showDashboard();
            view.updatePermissions(currentUsername, currentRole, isAdmin);
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
            ReturnResult result = client.register(username, password, role);
            result.checkError();
            view.switchToLoginTab(username);
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onLogout() {
        currentUsername = null;
        currentRole = null;
        isAdmin = false;
        view.clearBooks();
        view.showLogin();
    }

    @Override
    public void onAddBook(String name, String author, double price) {
        try {
            ReturnResult result = client.addBook(name, author, price);
            result.checkError();
            view.showInfo("添加图书成功！");
            onRefreshBooks();
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onDeleteBook(String bookname) {
        try {
            ReturnResult result = client.deleteBookByName(bookname);
            result.checkError();
            view.showInfo("删除图书成功！");
            onRefreshBooks();
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onUpdateBook(int id, String name, String author, double price) {
        try {
            ReturnResult result = client.updateBook(id, name, author, price);
            result.checkError();
            view.showInfo("修改图书成功！");
            onRefreshBooks();
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onSearch(String keyword) {
        try {
            ReturnResult result = client.searchBooks(keyword);
            result.checkError();
            List<Book> results = Client.toBookList(result);
            if (results.isEmpty()) {
                view.showInfo("未找到匹配的图书！");
                return;
            }
            List<Book> selected = view.showSearchResultsAndGetSelection(results, isAdmin);
            if (selected == null) return;

            if (isAdmin) {
                handleAdminAction(selected);
            } else {
                doBorrowBooks(selected);
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
                ReturnResult result = client.findBookById(bookId);
                result.checkError();
                results = Client.toBookList(result);
            } catch (NumberFormatException e) {
                ReturnResult result = client.searchBooks(keyword);
                result.checkError();
                results = Client.toBookList(result);
            }
            if (results.isEmpty()) {
                view.showInfo("未找到匹配的图书！");
                return;
            }
            List<Book> selected = view.showSearchResultsAndGetSelection(results, false);
            if (selected == null) return;
            doBorrowBooks(selected);
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onBorrowBooks(List<Book> books) {
        doBorrowBooks(books);
    }

    @Override
    public void onReturnBooks(List<Integer> recordIds) {
        try {
            ReturnResult urResult = client.getUnreturnedRecords(currentUsername);
            urResult.checkError();
            List<BorrowRecord> unreturned = Client.toBorrowRecordList(urResult);

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
                    ReturnResult result = client.returnBook(currentUsername, recordId);
                    result.checkError();
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
            ReturnResult result = client.getBorrowRecords(currentUsername);
            result.checkError();
            List<BorrowRecord> records = Client.toBorrowRecordList(result);
            view.displayBorrowRecords(records);
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    @Override
    public void onRefreshBooks() {
        try {
            ReturnResult result = client.listBooks();
            result.checkError();
            view.displayBooks(Client.toBookList(result));
        } catch (Exception e) {
            view.showError("加载图书列表失败：" + e.getMessage());
        }
    }

    // ==================== 内部方法 ====================

    private void handleAdminAction(List<Book> selected) {
        if (selected.size() == 1 && !isAdmin) {
            doBorrowBooks(selected);
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
                    ReturnResult result = client.deleteBookById(book.getId());
                    result.checkError();
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

    private void doBorrowBooks(List<Book> books) {
        int success = 0;
        StringBuilder failed = new StringBuilder();
        for (Book book : books) {
            try {
                ReturnResult result = client.borrowBook(currentUsername, book.getId());
                result.checkError();
                success++;
            } catch (Exception e) {
                failed.append("「").append(book.getBookname()).append("」: ").append(e.getMessage()).append("\n");
            }
        }
        view.showBatchResult("成功借阅 " + success + " 本图书！", failed);
        onRefreshBooks();
    }
}
