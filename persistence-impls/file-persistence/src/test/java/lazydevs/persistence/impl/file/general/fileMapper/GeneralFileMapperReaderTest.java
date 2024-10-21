package lazydevs.persistence.impl.file.general.fileMapper;

import lazydevs.mapper.file.flat.csv.CSVMapper;
import lazydevs.mapper.file.flat.excel.ExcelFileMapper;
import lazydevs.persistence.impl.file.general.ReadInstruction;
import lazydevs.persistence.reader.GeneralTransformer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * @author Abhijeet Rai
 */
public class GeneralFileMapperReaderTest {



    @Test
    public void testFindAllCsv(){
        File file = new File("/Users/abhijeetrai/NG/bitbucket/Database/datascience/s3/user.csv");
        GeneralFileMapperReader reader = new GeneralFileMapperReader(new CSVMapper());
        ReadInstruction readInstruction = new ReadInstruction();
        readInstruction.setFilePath(file.getAbsolutePath());
        List<Map<String, Object>> list = reader.findAll(readInstruction);
        int i = 0;
        for (Map<String, Object> r : list) {
            System.out.println(i++ + " - "+ r);
        }
    }
    @Test
    public void testFindAll() {
        File file = createFile();
        GeneralFileMapperReader reader = new GeneralFileMapperReader(new ExcelFileMapper());
        ReadInstruction readInstruction = new ReadInstruction();
        readInstruction.setFilePath(file.getAbsolutePath());
        readInstruction.setNoOfLinesToIgnore(1);
        List<Map<String, Object>> list = reader.findAll(readInstruction);
        assertEquals(list.size(), 2);
        assertEquals(list.get(0).get("i0"), "a1");
        assertEquals(list.get(0).get("i1"), "b1");
        assertEquals(list.get(1).get("i0"), "a2");
        assertEquals(list.get(1).get("i1"), "b2");

        GeneralTransformer generalTransformer = new GeneralTransformer();
        generalTransformer.setTemplate("{\n" +
                "            \"aa\" : \"converted-${i0}\",\n" +
                "            \"bb\" : \"converted-${i1}\",\n" +
                "            \"cc\" : \"converted-cc\"\n" +
                "          }");
        list = reader.findAll(readInstruction, null, generalTransformer);
        assertEquals(list.size(), 2);
        assertEquals(list.get(0).get("aa"), "converted-a1");
        assertEquals(list.get(0).get("bb"), "converted-b1");
        assertEquals(list.get(0).get("cc"), "converted-cc");

        assertEquals(list.get(1).get("aa"), "converted-a2");
        assertEquals(list.get(1).get("bb"), "converted-b2");
        assertEquals(list.get(1).get("cc"), "converted-cc");

    }

    @Test
    public void testFindAllInBatch() {
    }

    private File createFile(){
        ExcelFileMapper fileMapper = new ExcelFileMapper();
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "a");
        map.put(1, "b");
        return fileMapper.writeFile(Arrays.asList(
                getMap("a=a1,b=b1"),
                getMap("a=a2,b=b2")
        ), "abcd.xlsx", map, new String[]{"AAA", "BBB"});

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