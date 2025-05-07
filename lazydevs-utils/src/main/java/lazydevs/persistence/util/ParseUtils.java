package lazydevs.persistence.util;

import lazydevs.mapper.utils.ValueExtractor;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ParseUtils {

    public static List<Map<String, Object>> getList(Map<String, Object> map, @NonNull String attributeName){
        return (List<Map<String, Object>>)get(map, attributeName);
    }

    public static String getString(Map<String, Object> map, @NonNull String attributeName){
        return (String)get(map, attributeName);
    }



    public static Object get(Map<String, Object> map, @NonNull String attributeName){
        String[] tokens = attributeName.split("\\.");
        Object value = map;
        for(int i = 0; i < tokens.length; i++){
            String token = tokens[i];
            if(token.contains("[") && token.endsWith("]")){
                String arrayKey = token.substring(0, token.indexOf("["));
                String indexStr = token.substring(token.indexOf("[")+1, token.indexOf("]"));
                var list = ((Map<String, Object>)value).get(arrayKey);
                if(indexStr.matches("-?\\d+")){
                    if(list instanceof List){
                        value = ((List) list).get(Integer.parseInt(indexStr));
                    }else{
                        throw new IllegalArgumentException(arrayKey+"  is not a List");
                    }
                }else{
                    Map<String, String> obj = ValueExtractor.extractValues(indexStr, "containsKey='${key}'");
                    String key = obj.get("key");
                    if(list instanceof List){
                        Optional<?> optional = ((List<Map<String, Object>>) list).stream().filter( e -> e.containsKey(key)).findAny();
                        if(optional.isEmpty()){
                            throw new IllegalArgumentException("No records exists which contains key = "+ key);
                        }
                        value = optional.get();
                    }else{
                        throw new IllegalArgumentException(arrayKey+"  is not a List");
                    }
                }
            }else{
                value = ((Map<String, Object>)value).get(token);
            }
        }
        return value;
    }
}
