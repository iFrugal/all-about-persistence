package lazydevs.persistence.util;

import java.util.HashMap;
import java.util.Map;

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
}
