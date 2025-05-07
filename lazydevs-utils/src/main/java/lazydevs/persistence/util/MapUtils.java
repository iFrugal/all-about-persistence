package lazydevs.persistence.util;

import java.util.*;

/**
 * @author Abhijeet Rai
 */
public class MapUtils {

    public static <K, V> Map<K, V> getMap(K k, V v){
        Map<K, V> map = new HashMap<>();
        map.put(k, v);
        return map;
    }

    public static <K, V> Map<K, V> getMap(K k1, V v1, K k2, V v2 ){
        Map<K, V> map = getMap(k1,v1);
        map.put(k2, v2);
        return map;
    }

    public static Map<String, Object> filterFields(Map<String, Object> input, Map<String, Integer> projections) {
        Set<String> includes = new HashSet<>();
        Set<String> excludes = new HashSet<>();

        for (Map.Entry<String, Integer> entry : projections.entrySet()) {
            if (entry.getValue() == 1) {
                includes.add(entry.getKey());
            } else if (entry.getValue() == 0) {
                excludes.add(entry.getKey());
            }
        }

        Map<String, Object> result = new HashMap<>();

        if (!includes.isEmpty()) {
            for (String path : includes) {
                List<String> parts = Arrays.asList(path.split("\\."));
                Object value = getValueAtPath(input, parts);
                if (value != null) {
                    insertValue(result, parts, value);
                }
            }
        } else {
            result.putAll(input);
        }

        for (String path : excludes) {
            List<String> parts = Arrays.asList(path.split("\\."));
            removeValue(result, parts);
        }

        return result;
    }

    private static Object getValueAtPath(Object current, List<String> pathParts) {
        if (pathParts.isEmpty() || current == null) return current;

        String currentKey = pathParts.get(0);
        List<String> remainingPath = pathParts.subList(1, pathParts.size());

        if (current instanceof Map) {
            Map<?, ?> currentMap = (Map<?, ?>) current;
            if (!currentMap.containsKey(currentKey)) return null;
            return getValueAtPath(currentMap.get(currentKey), remainingPath);

        } else if (current instanceof List) {
            List<Object> list = (List<Object>) current;
            List<Object> results = new ArrayList<>();
            for (Object item : list) {
                Object subResult = getValueAtPath(item, pathParts);
                if (subResult != null) results.add(subResult);
            }
            return results;
        }
        return null;
    }

    private static void insertValue(Map<String, Object> current, List<String> pathParts, Object value) {
        String currentKey = pathParts.get(0);
        if (pathParts.size() == 1) {
            current.put(currentKey, value);
        } else {
            Map<String, Object> child = (Map<String, Object>) current.get(currentKey);
            if (child == null) {
                child = new HashMap<>();
                current.put(currentKey, child);
            }
            insertValue(child, pathParts.subList(1, pathParts.size()), value);
        }
    }

    private static void removeValue(Map<String, Object> current, List<String> pathParts) {
        if (current == null || pathParts.isEmpty()) return;

        String currentKey = pathParts.get(0);
        if (!current.containsKey(currentKey)) return;

        if (pathParts.size() == 1) {
            current.remove(currentKey);
        } else {
            Object child = current.get(currentKey);
            if (child instanceof Map) {
                removeValue((Map<String, Object>) child, pathParts.subList(1, pathParts.size()));
            } else if (child instanceof List) {
                List<Object> newList = new ArrayList<>();
                for (Object item : (List<?>) child) {
                    if (item instanceof Map) {
                        Map<String, Object> copiedItem = new HashMap<>((Map<String, Object>) item);
                        removeValue(copiedItem, pathParts.subList(1, pathParts.size()));
                        newList.add(copiedItem);
                    } else {
                        newList.add(item);
                    }
                }
                current.put(currentKey, newList);
            }
        }
    }


}
