package ui;

import controller.LibraryController;
import network.Client;
import view.LibraryView;

import javax.swing.*;

public class MainClass {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            try {
                Client client = new Client("127.0.0.1", 8888);
                LibraryView view = new LibraryView();
                new LibraryController(view, client);
                view.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "无法连接服务端：" + e.getMessage() + "\n请确保服务端已启动。",
                        "连接失败", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}