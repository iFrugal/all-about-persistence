package lazydevs.persistence.util;

import lazydevs.mapper.utils.ValueExtractor;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ParseUtils {

    // Your existing methods
    public static List<Map<String, Object>> getList(Map<String, Object> map, @NonNull String attributeName){
        return (List<Map<String, Object>>)get(map, attributeName);
    }

    public static Map<String, Object> getMap(Map<String, Object> map, @NonNull String attributeName){
        return (Map<String, Object>)get(map, attributeName);
    }
    public static String getString(Map<String, Object> map, @NonNull String attributeName){
        return (String)get(map, attributeName);
    }

    // Enhanced get method with proper null safety
    public static Object get(Map<String, Object> map, @NonNull String attributeName){
        if (map == null) return null;

        String[] tokens = attributeName.split("\\.");
        Object value = map;

        for(int i = 0; i < tokens.length; i++){
            if (value == null) return null; // Null safety - stop traversal if value becomes null

            // Ensure value is a Map before trying to access it
            if (!(value instanceof Map)) {
                return null; // Can't traverse further if not a Map
            }

            String token = tokens[i];
            if(token.contains("[") && token.endsWith("]")){
                String arrayKey = token.substring(0, token.indexOf("["));
                String indexStr = token.substring(token.indexOf("[")+1, token.indexOf("]"));

                var list = ((Map<String, Object>)value).get(arrayKey);
                if (list == null) return null; // Null safety

                if(indexStr.matches("-?\\d+")){
                    if(list instanceof List){
                        int index = Integer.parseInt(indexStr);
                        List<?> listObj = (List<?>) list;
                        if (index >= 0 && index < listObj.size()) {
                            value = listObj.get(index);
                        } else {
                            return null; // Index out of bounds
                        }
                    }else{
                        return null; // Not a list, can't access by index
                    }
                }else{
                    try {
                        Map<String, String> obj = ValueExtractor.extractValues(indexStr, "containsKey='${key}'");
                        String key = obj.get("key");
                        if(list instanceof List){
                            Optional<?> optional = ((List<Map<String, Object>>) list).stream()
                                    .filter(e -> e != null && e.containsKey(key))
                                    .findAny();
                            if(optional.isEmpty()){
                                return null; // Not found instead of exception
                            }
                            value = optional.get();
                        }else{
                            return null; // Not a list
                        }
                    } catch (Exception e) {
                        return null; // Error in ValueExtractor, return null instead of throwing
                    }
                }
            }else{
                value = ((Map<String, Object>)value).get(token);
            }
        }
        return value;
    }

    // Additional helper methods for condition evaluation

    /**
     * Get with default value if null or not found
     */
    public static Object getOrDefault(Map<String, Object> map, @NonNull String attributeName, Object defaultValue) {
        Object result = get(map, attributeName);
        return result != null ? result : defaultValue;
    }

    /**
     * Check if field exists (not null)
     */
    public static boolean exists(Map<String, Object> map, @NonNull String attributeName) {
        return get(map, attributeName) != null;
    }

    /**
     * Get as Integer with null safety
     */
    public static Integer getInteger(Map<String, Object> map, @NonNull String attributeName) {
        Object value = get(map, attributeName);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get as Double with null safety
     */
    public static Double getDouble(Map<String, Object> map, @NonNull String attributeName) {
        Object value = get(map, attributeName);
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get as Boolean with null safety
     */
    public static Boolean getBoolean(Map<String, Object> map, @NonNull String attributeName) {
        Object value = get(map, attributeName);
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;

        String str = value.toString().toLowerCase().trim();
        if ("true".equals(str) || "1".equals(str) || "yes".equals(str)) return true;
        if ("false".equals(str) || "0".equals(str) || "no".equals(str)) return false;

        return null;
    }

    /**
     * Safe string conversion
     */
    public static String getStringOrDefault(Map<String, Object> map, @NonNull String attributeName, String defaultValue) {
        Object value = get(map, attributeName);
        return value != null ? value.toString() : defaultValue;
    }
}