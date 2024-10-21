package lazydevs.persistence.impl.file.general.fileMapper.remote;

import lazydevs.mapper.file.flat.excel.ExcelFileMapper;
import lazydevs.persistence.impl.file.general.ReadInstruction;
import lazydevs.persistence.impl.file.general.fileMapper.GeneralFileMapperReader;
import lazydevs.persistence.impl.file.general.fileMapper.remote.FileDownloadStrategy.SessionFactoryInit;
import lazydevs.persistence.reader.GeneralTransformer;
import org.springframework.integration.file.remote.session.Session;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * @author Abhijeet Rai
 */
public class RemoteGeneralFileMapperReaderTest {

    RemoteGeneralFileMapperReader reader = new RemoteGeneralFileMapperReader(getFileDownloadStrategy(), new ExcelFileMapper());

    @Test
    public void testFindAll() {
        File file = createFile();
        ReadInstruction readInstruction = new ReadInstruction();
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
        File file = fileMapper.writeFile(Arrays.asList(
                getMap("a=a1,b=b1"),
                getMap("a=a2,b=b2")
        ), "abcd.xlsx", map, new String[]{"AAA", "BBB"});
        try(Session session = reader.getFileSessionProvider().getConnection()){
            try (FileInputStream fis = new FileInputStream(file)) {
                session.write(fis, getFileDownloadStrategy().getSourceDirectory()+"/"+getFileDownloadStrategy().getFileName());
            } catch (Exception e) {
                throw new RuntimeException("Exception occurred while moving the file from remote to local-temp", e );
            }
        }
        return file;
    }

    static Map<String, Object> getMap(String s){
        Map<String, Object> map = new LinkedHashMap<>();
        Arrays.stream(s.split(",")).forEach(token-> {
            String[] keyValPair = token.split("=");
            map.put(keyValPair[0].trim(), keyValPair[1].trim());
        });
        return map;
    }

    private FileDownloadStrategy getFileDownloadStrategy() {
        return new FileDownloadStrategy(getSessionFactoryInit(), "abcd_remote.xlsx", "/upload", ".");
    }

    private SessionFactoryInit getSessionFactoryInit() {
        SessionFactoryInit sessionFactoryInit = new SessionFactoryInit();
        sessionFactoryInit.setFileProtocol("SFTP");
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("host", "XXXX");
        paramsMap.put("port", 22);
        paramsMap.put("user", "XXXX");
        paramsMap.put("password", "XXXXX");
        sessionFactoryInit.setParams(paramsMap);
        return sessionFactoryInit;
    }
}