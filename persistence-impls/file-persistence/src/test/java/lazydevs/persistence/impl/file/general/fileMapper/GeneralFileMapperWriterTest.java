package lazydevs.persistence.impl.file.general.fileMapper;

import lazydevs.mapper.file.flat.excel.ExcelFileMapper;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * @author Abhijeet Rai
 */
public class GeneralFileMapperWriterTest {

    //@Test
    public void testCreate() {
        ExcelFileMapper fileMapper = new ExcelFileMapper();
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "a");
        map.put(1, "b");
        fileMapper.writeFile(Arrays.asList(
                getMap("a=a1,b=b1"),
                getMap("a=a2,b=b2")
        ), "abcd.xlsx", map, new String[]{"AAA", "BBB"});

    }

    @Test
    public void testTestCreate() {
    }

    static Map<String, Object> getMap(String s){
        Map<String, Object> map = new LinkedHashMap<>();
        Arrays.stream(s.split(",")).forEach(token-> {
            String[] keyValPair = token.split("=");
            map.put(keyValPair[0].trim(), keyValPair[1].trim());
        });
        return map;
    }
}