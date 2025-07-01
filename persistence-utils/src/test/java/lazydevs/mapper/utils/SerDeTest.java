package lazydevs.mapper.utils;

import lazydevs.mapper.utils.file.FileUtils;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

/**
 * @author Abhijeet Rai
 */
@Test
public class SerDeTest {

    public void testJson(){
        testComprehensive(SerDe.JSON);
    }

    public void testYaml(){
        testComprehensive(SerDe.YAML);
    }

    public void testXml(){
        testSerDe(SerDe.XML);
    }

    private void testSerDe(SerDe serDe){
        ABC abc = new ABC("a1", "b1", Arrays.asList("c1", "c2", "c3", "c4"));
        System.out.println(serDe.serialize(abc));
        ABC abc1 = serDe.deserialize(serDe.serialize(abc), ABC.class);
        assertEquals(abc1, abc);
    }

    private void testComprehensive(SerDe serDe){
        ABC abc = new ABC("a1", "b1", Arrays.asList("c1", "c2", "c3", "c4"));
        System.out.println(serDe.serialize(abc));
        ABC abc1 = serDe.deserialize(serDe.serialize(abc), ABC.class);
        assertEquals(abc1, abc);
        FileUtils.writeToFile("abc.txt", serDe.serialize(abc, true));
        ABC abc2 = serDe.deserialize(new File("abc.txt"), ABC.class);
        assertEquals(abc2, abc);
        FileUtils.delete("abc.txt");
        testDeserializeToMap(serDe);
        testDeserializeToListOfMap(serDe);
    }

    private void testDeserializeToMap(SerDe serDe){
        ABC abc1 = new ABC("a1", "b1", Arrays.asList("c1-1", "c1-2", "c1-3", "c1-4"));
        ABC abc2 =  new ABC("a2", "b2", Arrays.asList("c2-1", "c2-2", "c2-3", "c2-4"));
        Map<String, ABC> map = new HashMap<>();
        map.put("1", abc1);
        map.put("2", abc2);
        String string =  serDe.serialize(map, true);
        System.out.println(string);


        Map<String, Object> genericMap = serDe.deserializeToMap(string);
        Map<String, ABC> typeSpecificMap = serDe.deserializeToMap(string, String.class, ABC.class);
        assertEquals(abc1, typeSpecificMap.get("1"));
        assertEquals(abc2, typeSpecificMap.get("2"));
        assertEquals(abc1, serDe.fromMap((Map<String, Object>)genericMap.get("1"), ABC.class));
        assertEquals(abc2, serDe.fromMap((Map<String, Object>)genericMap.get("2"), ABC.class));
    }

    private void testDeserializeToListOfMap(SerDe serDe){
        ABC abc1 = new ABC("a1", "b1", Arrays.asList("c1-1", "c1-2", "c1-3", "c1-4"));
        ABC abc2 =  new ABC("a2", "b2", Arrays.asList("c2-1", "c2-2", "c2-3", "c2-4"));
        List<ABC> list = new ArrayList<>(Arrays.asList(abc1, abc2));
        String string =  serDe.serialize(list, true);
        System.out.println(string);


        List<Map<String, Object>> listOfMap = serDe.deserializeToListOfMap(string);
        Map<String, Object> row1 = listOfMap.get(0);
        Map<String, Object> row2 = listOfMap.get(1);
        assertEquals(abc1, serDe.fromMap(row1, ABC.class));
        assertEquals(abc2, serDe.fromMap(row2, ABC.class));
    }

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor @EqualsAndHashCode
    private static class ABC {
        private String a, b;
        private List<String> c;
    }



}