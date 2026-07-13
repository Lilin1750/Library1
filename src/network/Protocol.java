package network;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class Protocol {

    static final Charset CHARSET = Charset.forName("GBK");
    static final String END_MARKER = "END";
    static final String DATA_PREFIX = "DATA=";
    static final String STATUS_PREFIX = "STATUS=";
    static final String MSG_PREFIX = "MSG=";

    // ==================== 请求打包 ====================

    public static String packLogin(String username, String password) {
        return "LOGIN," + username + "," + password;
    }

    public static String packRegister(String username, String password, String role) {
        return "REGISTER," + username + "," + password + "," + role;
    }

    public static String packAddBook(String name, String author, double price) {
        return "ADD," + name + "," + author + "," + price;
    }

    public static String packDeleteBookByName(String bookname) {
        return "DELETE_NAME," + bookname;
    }

    public static String packDeleteBookById(int id) {
        return "DELETE_ID," + id;
    }

    public static String packUpdateBook(int id, String name, String author, double price) {
        return "UPDATE," + id + "," + nvl(name) + "," + nvl(author) + "," + price;
    }

    public static String packSearchBooks(String keyword) {
        return "SEARCH," + keyword;
    }

    public static String packListBooks() {
        return "LIST";
    }

    public static String packFindBookById(int id) {
        return "FIND_ID," + id;
    }

    public static String packBorrowBook(String username, int bookId) {
        return "BORROW," + username + "," + bookId;
    }

    public static String packReturnBook(String username, int recordId) {
        return "RETURN," + username + "," + recordId;
    }

    public static String packGetUnreturnedRecords(String username) {
        return "UNRETURNED," + username;
    }

    public static String packGetBorrowRecords(String username) {
        return "RECORDS," + username;
    }

    // ==================== 响应解析 ====================

    public static ReturnResult parseResponse(BufferedReader reader) throws IOException {
        String statusLine = reader.readLine();
        if (statusLine == null) throw new IOException("连接已断开");

        boolean success = "OK".equals(statusLine.substring(STATUS_PREFIX.length()).trim());

        String msgLine = reader.readLine();
        if (msgLine == null) throw new IOException("响应不完整");
        String message = decodeMsgValue(msgLine.substring(MSG_PREFIX.length()));

        String dataLine = reader.readLine();
        if (dataLine == null) throw new IOException("响应不完整");

        if (dataLine.startsWith(DATA_PREFIX)) {
            String dataType = dataLine.substring(DATA_PREFIX.length()).trim();
            if ("NONE".equals(dataType)) {
                return success ? ReturnResult.ok(message) : ReturnResult.error(message);
            }

            String colLine = reader.readLine();
            List<String> columns = parseLine(colLine);

            List<List<String>> rows = new ArrayList<>();
            while (true) {
                String rowLine = reader.readLine();
                if (rowLine == null) throw new IOException("响应不完整");
                if (END_MARKER.equals(rowLine.trim())) break;
                rows.add(parseLine(rowLine));
            }

            return ReturnResult.data(message, dataType, columns, rows);
        } else {
            return success ? ReturnResult.ok(message) : ReturnResult.error(message);
        }
    }

    // ==================== 响应序列化（服务端用） ====================

    public static void writeResult(PrintWriter out, ReturnResult result) {
        out.println(result.isSuccess() ? "STATUS=OK" : "STATUS=ERROR");
        out.println(MSG_PREFIX + encodeMsgValue(result.getMessage()));

        if (result.getDataType() == null || result.getRows().isEmpty()) {
            out.println(DATA_PREFIX + "NONE");
        } else {
            out.println(DATA_PREFIX + result.getDataType());
            out.println(join(result.getColumns()));
            for (List<String> row : result.getRows()) {
                out.println(join(row));
            }
            out.println(END_MARKER);
        }
        out.flush();
    }

    // ==================== 工具方法 ====================

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static List<String> parseLine(String line) {
        List<String> list = new ArrayList<>();
        if (line == null || line.isEmpty()) return list;
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escape) {
                sb.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '|') {
                list.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        list.add(sb.toString());
        return list;
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append('|');
            sb.append(parts.get(i).replace("\\", "\\\\").replace("|", "\\|"));
        }
        return sb.toString();
    }

    private static String encodeMsgValue(String msg) {
        return msg.replace("\\", "\\\\").replace("\n", "\\n");
    }

    private static String decodeMsgValue(String raw) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\' && i + 1 < raw.length()) {
                char next = raw.charAt(i + 1);
                if (next == 'n') { sb.append('\n'); i++; continue; }
                if (next == '\\') { sb.append('\\'); i++; continue; }
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
