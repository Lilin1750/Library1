package view;

import model.Book;
import model.BorrowRecord;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LibraryView extends JFrame {

    public interface Listener {
        void onLogin(String username, String password);
        void onRegister(String username, String password, String role);
        void onLogout();
        void onAddBook(String name, String author, double price);
        void onDeleteBook(String bookname);
        void onUpdateBook(int id, String name, String author, double price);
        void onSearch(String keyword);
        void onBorrowSearch(String keyword);
        void onBorrowBooks(List<Book> books);
        void onReturnBooks(List<Integer> recordIds);
        void onViewRecords();
        void onRefreshBooks();
    }

    private Listener listener;

    private JTextField loginUsernameField;
    private JPasswordField loginPasswordField;
    private JTextField registerUsernameField;
    private JPasswordField registerPasswordField;
    private JComboBox<String> registerRoleBox;
    private JTabbedPane loginTabbedPane;

    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private static final String CARD_LOGIN = "LOGIN";
    private static final String CARD_DASHBOARD = "DASHBOARD";

    private JLabel welcomeLabel;
    private DefaultTableModel bookTableModel;
    private JTable bookTable;
    private JButton btnAdd;
    private JButton btnDelete;
    private JButton btnUpdate;
    private JButton btnBorrow;
    private JButton btnReturn;
    private JButton btnViewRecords;

    public LibraryView() {
        setTitle("图书管理系统");
        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.add(buildLoginPanel(), CARD_LOGIN);
        mainPanel.add(buildDashboardPanel(), CARD_DASHBOARD);
        add(mainPanel);
        showLogin();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    // ==================== 导航 ====================

    public void showLogin() {
        cardLayout.show(mainPanel, CARD_LOGIN);
    }

    public void showDashboard() {
        cardLayout.show(mainPanel, CARD_DASHBOARD);
    }

    // ==================== 数据展示 ====================

    public void displayBooks(List<Book> books) {
        bookTableModel.setRowCount(0);
        for (Book book : books) {
            bookTableModel.addRow(new Object[]{
                    book.getId(), book.getBookname(), book.getAuthor(),
                    String.format("%.2f", book.getPrice()), book.getStock()
            });
        }
    }

    public void clearBooks() {
        bookTableModel.setRowCount(0);
    }

    public void updatePermissions(String username, String role, boolean isAdmin) {
        welcomeLabel.setText("当前用户：" + username + "（" + role + "）");
        btnAdd.setVisible(isAdmin);
        btnDelete.setVisible(isAdmin);
        btnUpdate.setVisible(isAdmin);
        btnBorrow.setVisible(!isAdmin);
        btnReturn.setVisible(!isAdmin);
        btnViewRecords.setVisible(!isAdmin);
    }

    public void displayBorrowRecords(List<BorrowRecord> records) {
        if (records.isEmpty()) {
            showInfo("暂无借阅记录！");
            return;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String[] columns = {"记录编号", "图书编号", "借阅时间", "归还状态"};
        String[][] data = new String[records.size()][4];
        for (int i = 0; i < records.size(); i++) {
            BorrowRecord r = records.get(i);
            data[i][0] = String.valueOf(r.getId());
            data[i][1] = String.valueOf(r.getBookId());
            data[i][2] = r.getBorrowDate() != null ? r.getBorrowDate().format(fmt) : "";
            data[i][3] = r.isReturned() ? r.getReturnDate().format(fmt) : "未归还";
        }
        JTable table = new JTable(data, columns);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        table.setRowHeight(25);
        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(500, 300));
        JOptionPane.showMessageDialog(this, sp, "我的借阅记录", JOptionPane.INFORMATION_MESSAGE);
    }

    // ==================== 消息提示 ====================

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "错误", JOptionPane.ERROR_MESSAGE);
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showBatchResult(String successMsg, StringBuilder failed) {
        StringBuilder msg = new StringBuilder(successMsg);
        if (failed.length() > 0) msg.append("\n\n以下操作失败：\n").append(failed);
        showInfo(msg.toString());
    }

    // ==================== 用户输入采集 ====================

    public String askKeyword(String title, String prompt) {
        String input = JOptionPane.showInputDialog(this, prompt, title, JOptionPane.PLAIN_MESSAGE);
        return (input == null) ? null : input.trim();
    }

    public boolean confirm(String message, String title) {
        return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    public double[] askBookInput(String defaultName, String defaultAuthor, String defaultPrice) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nameField = new JTextField(defaultName != null ? defaultName : "", 15);
        JTextField authorField = new JTextField(defaultAuthor != null ? defaultAuthor : "", 15);
        JTextField priceField = new JTextField(defaultPrice != null ? defaultPrice : "", 15);

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("书名："), gbc);
        gbc.gridx = 1; panel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("作者："), gbc);
        gbc.gridx = 1; panel.add(authorField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("价格（元）："), gbc);
        gbc.gridx = 1; panel.add(priceField, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "图书信息",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return null;

        try {
            double price = defaultPrice == null ? Double.parseDouble(priceField.getText().trim()) : -1;
            if (defaultPrice != null) {
                try { price = Double.parseDouble(priceField.getText().trim()); } catch (NumberFormatException e) { price = -1; }
            }
            return new double[]{
                    nameField.getText().trim().isEmpty() ? -1 : 0,
                    authorField.getText().trim().isEmpty() ? -1 : 0,
                    price
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String[] askAddBookInput() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nameField = new JTextField(15);
        JTextField authorField = new JTextField(15);
        JTextField priceField = new JTextField(15);

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("书名："), gbc);
        gbc.gridx = 1; panel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("作者："), gbc);
        gbc.gridx = 1; panel.add(authorField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("价格（元）："), gbc);
        gbc.gridx = 1; panel.add(priceField, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "添加图书",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return null;
        return new String[]{nameField.getText().trim(), authorField.getText().trim(), priceField.getText().trim()};
    }

    public List<Book> showSearchResultsAndGetSelection(List<Book> books, boolean isAdmin) {
        String[] columns = {"编号", "书名", "作者", "价格（元）", "库存"};
        String[][] data = new String[books.size()][5];
        for (int i = 0; i < books.size(); i++) {
            Book b = books.get(i);
            data[i][0] = String.valueOf(b.getId());
            data[i][1] = b.getBookname();
            data[i][2] = b.getAuthor();
            data[i][3] = String.format("%.2f", b.getPrice());
            data[i][4] = String.valueOf(b.getStock());
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        table.setRowHeight(26);
        table.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 13));
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(600, 300));

        JPanel panel = new JPanel(new BorderLayout(5, 10));
        JLabel tip = new JLabel("共找到 " + books.size() + " 本图书，可多选后操作");
        tip.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        panel.add(tip, BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        Font f = new Font("微软雅黑", Font.PLAIN, 13);

        JButton btnAll = new JButton("全选"); btnAll.setFont(f);
        btnAll.addActionListener(e -> table.selectAll());
        JButton btnClear = new JButton("取消"); btnClear.setFont(f);
        btnClear.addActionListener(e -> table.clearSelection());

        final List<Book>[] selected = new List[]{null};
        JButton btnConfirm = new JButton(isAdmin ? "操作选中" : "借阅选中");
        btnConfirm.setFont(f);
        btnConfirm.addActionListener(e -> {
            int[] rows = table.getSelectedRows();
            if (rows.length == 0) { showError("请先选中图书！"); return; }
            selected[0] = new ArrayList<>();
            for (int r : rows) selected[0].add(books.get(r));
            SwingUtilities.getWindowAncestor(btnConfirm).dispose();
        });

        btns.add(btnAll); btns.add(btnClear); btns.add(btnConfirm);
        panel.add(btns, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(this, "搜索结果", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return selected[0];
    }

    public List<Integer> showReturnDialog(List<BorrowRecord> records) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String[] columns = {"记录编号", "图书编号", "借阅时间"};
        String[][] data = new String[records.size()][3];
        for (int i = 0; i < records.size(); i++) {
            BorrowRecord r = records.get(i);
            data[i][0] = String.valueOf(r.getId());
            data[i][1] = String.valueOf(r.getBookId());
            data[i][2] = r.getBorrowDate() != null ? r.getBorrowDate().format(fmt) : "";
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        table.setRowHeight(26);
        table.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 13));
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(450, 250));

        JPanel panel = new JPanel(new BorderLayout(5, 10));
        JLabel tip = new JLabel("共 " + records.size() + " 本未归还图书，选中后点击归还");
        tip.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        panel.add(tip, BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        Font f = new Font("微软雅黑", Font.PLAIN, 13);

        JButton btnAll = new JButton("全选"); btnAll.setFont(f);
        btnAll.addActionListener(e -> table.selectAll());
        JButton btnClear = new JButton("取消"); btnClear.setFont(f);
        btnClear.addActionListener(e -> table.clearSelection());

        final List<Integer>[] result = new List[]{null};
        JButton btnConfirm = new JButton("归还选中");
        btnConfirm.setFont(f);
        btnConfirm.addActionListener(e -> {
            int[] rows = table.getSelectedRows();
            if (rows.length == 0) { showError("请先选中要归还的图书！"); return; }
            result[0] = new ArrayList<>();
            for (int r : rows) result[0].add(records.get(r).getId());
            SwingUtilities.getWindowAncestor(btnConfirm).dispose();
        });

        btns.add(btnAll); btns.add(btnClear); btns.add(btnConfirm);
        panel.add(btns, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(this, "归还图书", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return result[0];
    }

    public String[] askUpdateValues(Book book) {
        String newName = (String) JOptionPane.showInputDialog(this,
                "书名（当前：" + book.getBookname() + "）：", "修改图书",
                JOptionPane.PLAIN_MESSAGE, null, null, book.getBookname());
        if (newName == null) return null;

        String newAuthor = (String) JOptionPane.showInputDialog(this,
                "作者（当前：" + book.getAuthor() + "）：", "修改图书",
                JOptionPane.PLAIN_MESSAGE, null, null, book.getAuthor());
        if (newAuthor == null) return null;

        String priceStr = (String) JOptionPane.showInputDialog(this,
                "价格（当前：" + String.format("%.2f", book.getPrice()) + "）：", "修改图书",
                JOptionPane.PLAIN_MESSAGE, null, null, String.format("%.2f", book.getPrice()));
        if (priceStr == null) return null;

        return new String[]{newName.trim(), newAuthor.trim(), priceStr.trim()};
    }

    public String askAdminAction(int count) {
        return (String) JOptionPane.showInputDialog(this,
                "请选择操作：", "操作选中图书",
                JOptionPane.PLAIN_MESSAGE, null,
                new String[]{"批量删除", "修改（仅单本）"}, "批量删除");
    }

    public void clearLoginPassword() {
        loginPasswordField.setText("");
    }

    public void switchToLoginTab(String username) {
        registerUsernameField.setText("");
        registerPasswordField.setText("");
        loginUsernameField.setText(username);
        loginTabbedPane.setSelectedIndex(0);
    }

    // ==================== 构建 UI ====================

    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 150, 30, 150));

        JLabel title = new JLabel("欢迎使用图书管理系统", SwingConstants.CENTER);
        title.setFont(new Font("微软雅黑", Font.BOLD, 24));
        panel.add(title, BorderLayout.NORTH);

        loginTabbedPane = new JTabbedPane();
        loginTabbedPane.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        loginTabbedPane.addTab("登录", buildLoginTab());
        loginTabbedPane.addTab("注册", buildRegisterTab());
        panel.add(loginTabbedPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildLoginTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        loginUsernameField = new JTextField(18);
        loginPasswordField = new JPasswordField(18);

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("用户名："), gbc);
        gbc.gridx = 1; panel.add(loginUsernameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("密  码："), gbc);
        gbc.gridx = 1; panel.add(loginPasswordField, gbc);

        JButton btn = new JButton("登  录");
        btn.setFont(new Font("微软雅黑", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 8, 8, 8);
        panel.add(btn, gbc);

        btn.addActionListener(e -> {
            if (listener != null)
                listener.onLogin(loginUsernameField.getText().trim(), new String(loginPasswordField.getPassword()));
        });
        loginPasswordField.addActionListener(e -> {
            if (listener != null)
                listener.onLogin(loginUsernameField.getText().trim(), new String(loginPasswordField.getPassword()));
        });
        return panel;
    }

    private JPanel buildRegisterTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        registerUsernameField = new JTextField(18);
        registerPasswordField = new JPasswordField(18);
        registerRoleBox = new JComboBox<>(new String[]{"普通用户", "管理员"});

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("用户名："), gbc);
        gbc.gridx = 1; panel.add(registerUsernameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("密  码（≥6位）："), gbc);
        gbc.gridx = 1; panel.add(registerPasswordField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("角  色："), gbc);
        gbc.gridx = 1; panel.add(registerRoleBox, gbc);

        JButton btn = new JButton("注  册");
        btn.setFont(new Font("微软雅黑", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 8, 8, 8);
        panel.add(btn, gbc);

        btn.addActionListener(e -> {
            if (listener != null) {
                String role = registerRoleBox.getSelectedIndex() == 0 ? "user" : "admin";
                listener.onRegister(registerUsernameField.getText().trim(),
                        new String(registerPasswordField.getPassword()), role);
            }
        });
        return panel;
    }

    private JPanel buildDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(buildTopBar(), BorderLayout.NORTH);
        panel.add(buildCenterArea(), BorderLayout.CENTER);
        panel.add(buildBottomBar(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        bar.setBorder(BorderFactory.createEtchedBorder());
        welcomeLabel = new JLabel();
        welcomeLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        bar.add(welcomeLabel);
        return bar;
    }

    private JPanel buildCenterArea() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        Font f = new Font("微软雅黑", Font.PLAIN, 13);

        JButton btnRefresh = new JButton("刷新列表"); btnRefresh.setFont(f);
        btnAdd = new JButton("添加图书"); btnAdd.setFont(f);
        btnDelete = new JButton("删除图书"); btnDelete.setFont(f);
        btnUpdate = new JButton("修改图书"); btnUpdate.setFont(f);
        JButton btnSearch = new JButton("搜索图书"); btnSearch.setFont(f);
        btnBorrow = new JButton("借阅图书"); btnBorrow.setFont(f);
        btnReturn = new JButton("归还图书"); btnReturn.setFont(f);
        btnViewRecords = new JButton("我的借阅记录"); btnViewRecords.setFont(f);

        btnRefresh.addActionListener(e -> { if (listener != null) listener.onRefreshBooks(); });
        btnAdd.addActionListener(e -> handleAddBook());
        btnDelete.addActionListener(e -> handleDeleteBook());
        btnUpdate.addActionListener(e -> handleUpdateBook());
        btnSearch.addActionListener(e -> handleSearch());
        btnBorrow.addActionListener(e -> handleBorrow());
        btnReturn.addActionListener(e -> { if (listener != null) listener.onReturnBooks(null); });
        btnViewRecords.addActionListener(e -> { if (listener != null) listener.onViewRecords(); });

        for (JButton b : new JButton[]{btnRefresh, btnAdd, btnDelete, btnUpdate, btnSearch, btnBorrow, btnReturn, btnViewRecords})
            btns.add(b);

        panel.add(btns, BorderLayout.NORTH);

        bookTableModel = new DefaultTableModel(
                new String[]{"编号", "书名", "作者", "价格（元）", "库存"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        bookTable = new JTable(bookTableModel);
        bookTable.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        bookTable.setRowHeight(28);
        bookTable.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 14));
        panel.add(new JScrollPane(bookTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        bar.setBorder(BorderFactory.createEtchedBorder());
        JButton btn = new JButton("退出登录");
        btn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        btn.addActionListener(e -> { if (listener != null) listener.onLogout(); });
        bar.add(btn);
        return bar;
    }

    // ==================== View 内部交互编排 ====================

    private void handleAddBook() {
        if (listener == null) return;
        String[] input = askAddBookInput();
        if (input == null) return;
        try {
            double price = Double.parseDouble(input[2]);
            listener.onAddBook(input[0], input[1], price);
        } catch (NumberFormatException e) {
            showError("价格格式不正确！");
        }
    }

    private void handleDeleteBook() {
        if (listener == null) return;
        String name = askKeyword("删除图书", "请输入要删除的书名：");
        if (name == null || name.isEmpty()) return;
        if (confirm("确定要删除图书「" + name + "」吗？", "确认删除")) {
            listener.onDeleteBook(name);
        }
    }

    private void handleUpdateBook() {
        if (listener == null) return;
        String keyword = askKeyword("查找图书", "请输入书名（支持模糊搜索）：");
        if (keyword == null || keyword.isEmpty()) return;
        listener.onSearch(keyword);
    }

    private void handleSearch() {
        if (listener == null) return;
        String keyword = askKeyword("查询图书", "请输入关键词（支持模糊搜索书名/作者）：");
        if (keyword == null || keyword.isEmpty()) return;
        listener.onSearch(keyword);
    }

    private void handleBorrow() {
        if (listener == null) return;
        String input = askKeyword("借阅图书", "请输入图书编号或关键词：");
        if (input == null || input.isEmpty()) return;
        listener.onBorrowSearch(input);
    }
}
