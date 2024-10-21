package lazydevs.persistence.impl.file.reader;

import org.testng.annotations.BeforeTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FileGeneralReaderTest {
    private static final String DELIMITER = ",";
    private static final int BATCH_SIZE = 10;
    private static final String FILE_NAME = "Test.csv";
    private List<List<Map<String, Object>>> data;
    //private FileGeneralReader fileGeneralReaderSpy;

    @BeforeTest
    public void setUp() throws IOException {
        //data = populateData();
        //fileGeneralReaderSpy = spy(new FileGeneralReader(getFileDownloadStrategy(), getFileParsingStrategy()));
    }

   /* @Test
    public void testFindAllInBatch() throws IOException {
        doReturn(new ClassPathResource(FILE_NAME).getFile()).when(fileGeneralReaderSpy).downloadFile();
        BatchIterator<Map<String, Object>> batchIterator = fileGeneralReaderSpy.findAllInBatch(BATCH_SIZE, new FileQuery(), (Param<Object>) null);
        int index = 0;

        while (batchIterator.hasNext()) {
            List<Map<String, Object>> actualMaps = batchIterator.next();
            assertEquals(actualMaps, data.get(index++));
        }
    }

    private FileParsingStrategy getFileParsingStrategy() {
        return new FileParsingStrategy(new DsvMapper(DELIMITER));
    }

    private FileDownloadStrategy getFileDownloadStrategy() {
        return new FileDownloadStrategy(getSessionFactoryInit(), FILE_NAME, "/upload", "/Users/g0s00q7/temp");
    }

    private SessionFactoryInit getSessionFactoryInit() {
        SessionFactoryInit sessionFactoryInit = new SessionFactoryInit();
        sessionFactoryInit.setFileProtocol("SFTP");
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("host", "10.117.159.206");
        paramsMap.put("port", 22);
        paramsMap.put("user", "wmtglmint");
        paramsMap.put("password", "wmtglmint123");
        sessionFactoryInit.setParams(paramsMap);
        return sessionFactoryInit;
    }

    private List<List<Map<String, Object>>> populateData() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new ClassPathResource(FILE_NAME).getFile()));
        List<List<Map<String, Object>>> data = new ArrayList<>();
        String line = bufferedReader.readLine();

        while (line != null) {
            List<Map<String, Object>> batch = new ArrayList<>(BATCH_SIZE);

            for (int i = 0; i <BATCH_SIZE ; i++) {
                String[] columnValues = line.split(DELIMITER);
                Map<String, Object> rowMap = new HashMap<>(columnValues.length);

                for (int j = 0; j < columnValues.length; j++) {
                    rowMap.put(String.valueOf(j), columnValues[j]);
                }

                batch.add(rowMap);
                line = bufferedReader.readLine();
            }

            data.add(batch);
        }

        return data;
    }*/

}