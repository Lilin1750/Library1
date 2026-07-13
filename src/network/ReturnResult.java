package network;

import java.util.*;

public class ReturnResult {
    private final boolean success;
    private final String message;
    private final String dataType;
    private final List<String> columns;
    private final List<List<String>> rows;

    private ReturnResult(boolean success, String message, String dataType,
                         List<String> columns, List<List<String>> rows) {
        this.success = success;
        this.message = message;
        this.dataType = dataType;
        this.columns = columns != null ? columns : Collections.emptyList();
        this.rows = rows != null ? rows : Collections.emptyList();
    }

    public static ReturnResult ok(String message) {
        return new ReturnResult(true, message, null, null, null);
    }

    public static ReturnResult error(String message) {
        return new ReturnResult(false, message, null, null, null);
    }

    public static ReturnResult data(String message, String dataType,
                                    List<String> columns, List<List<String>> rows) {
        return new ReturnResult(true, message, dataType, columns, rows);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getDataType() { return dataType; }
    public List<String> getColumns() { return columns; }
    public List<List<String>> getRows() { return rows; }

    public void checkError() {
        if (!success) throw new RuntimeException(message);
    }
}
