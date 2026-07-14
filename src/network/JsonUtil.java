package network;

import java.util.*;

public class JsonUtil {

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Boolean) return obj.toString();
        if (obj instanceof Number) return obj.toString();
        if (obj instanceof String) return "\"" + escape((String) obj) + "\"";
        if (obj instanceof Map) return mapToJson((Map<?, ?>) obj);
        if (obj instanceof List) return listToJson((List<?>) obj);
        return "\"" + escape(obj.toString()) + "\"";
    }

    private static String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escape(entry.getKey().toString())).append("\":");
            sb.append(toJson(entry.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static Map<String, String> parseJson(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null || json.trim().isEmpty()) return map;
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) return map;
        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) return map;

        int i = 0;
        while (i < json.length()) {
            while (i < json.length() && json.charAt(i) != '"') i++;
            if (i >= json.length()) break;
            i++;
            StringBuilder key = new StringBuilder();
            while (i < json.length() && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\' && i + 1 < json.length()) {
                    key.append(json.charAt(i + 1));
                    i += 2;
                } else {
                    key.append(json.charAt(i));
                    i++;
                }
            }
            i++;
            while (i < json.length() && json.charAt(i) != ':') i++;
            i++;
            while (i < json.length() && json.charAt(i) == ' ') i++;
            if (i >= json.length()) break;

            String value;
            if (json.charAt(i) == '"') {
                i++;
                StringBuilder val = new StringBuilder();
                while (i < json.length() && json.charAt(i) != '"') {
                    if (json.charAt(i) == '\\' && i + 1 < json.length()) {
                        val.append(json.charAt(i + 1));
                        i += 2;
                    } else {
                        val.append(json.charAt(i));
                        i++;
                    }
                }
                value = val.toString();
                i++;
            } else {
                StringBuilder val = new StringBuilder();
                while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}') {
                    val.append(json.charAt(i));
                    i++;
                }
                value = val.toString().trim();
            }
            map.put(key.toString(), value);
            if (i < json.length() && json.charAt(i) == ',') i++;
        }
        return map;
    }
}
