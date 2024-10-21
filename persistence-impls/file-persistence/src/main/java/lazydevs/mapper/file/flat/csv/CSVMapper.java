package lazydevs.mapper.file.flat.csv;

import lazydevs.mapper.file.flat.FlatFileMapper;
import lazydevs.mapper.file.flat.dsv.DsvMapper;
import lazydevs.mapper.file.flat.excel.CustomRowIterator;
import lazydevs.mapper.file.utils.FileBatchIterator;
import lazydevs.mapper.utils.BatchIterator;
import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.engine.TemplateEngine;
import lombok.NonNull;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class CSVMapper extends FlatFileMapper<CSVRecord> {


    @Override
    protected Map<String, Object> convert(CSVRecord row, String template) {
        Map<String, Object> convertedMap = new LinkedHashMap<>();
        List<String> headerNames = row.getParser().getHeaderNames();

        for (int i = 0; i < row.size(); i++) {
            String columnName = headerNames.get(i); // Get the column name
            String columnValue = row.get(i); // Get the value of the current column
            convertedMap.put(columnName, columnValue);
        }

        if(template != null) {
            return SerDe.JSON.deserializeToMap(TemplateEngine.getInstance().generate(template, convertedMap));
        }else {
            return convertedMap;
        }
    }


    private abstract  class CSVFileBatchIterator<T> extends FileBatchIterator<T, CSVRecord> {
        private final Reader reader;
        private final CSVParser csvParser;
        public CSVFileBatchIterator(@NonNull Reader reader, @NonNull CSVParser csvParser, int batchSize, int noOfLinesToIgnore) {
            super(csvParser.iterator(), batchSize, noOfLinesToIgnore);
            this.reader = reader;
            this.csvParser = csvParser;
        }

        @Override
        public boolean isRowToIgnore(CSVRecord row) {
            return false;
        }

        @Override
        public void close() {
            try{
               csvParser.close();
               reader.close();
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected FileBatchIterator<Map<String, Object>, CSVRecord> getBatchIterator(File file, Charset charset, int batchSize, int noOfLinesToIgnore, String template) {
        try {
            Reader reader = new FileReader(file);
            CSVParser csvParser =  CSVFormat.POSTGRESQL_CSV.withHeader().withRecordSeparator(",").parse(reader);

            // Get headers
            List<String> headers = csvParser.getHeaderNames();
            System.out.println("Headers: " + headers);
            return  new CSVFileBatchIterator<Map<String, Object>>(reader, csvParser,batchSize, noOfLinesToIgnore) {
                @Override
                public Map<String, Object> map(CSVRecord csvRecord) {
                    return convert(csvRecord, template);
                }
            };
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    protected FileBatchIterator<Map<String, Object>, CSVRecord> getBatchIterator(InputStream inputStream, Charset charset, int batchSize, int noOfLinesToIgnore, String template) {
        throw new UnsupportedOperationException("Not yet implemented ..but 2 -3 lines will be required to implement");
    }









    //WRiter methods

    @Override
    public <T> File writeFile(List<T> list, String absoluteFilePath, String[] headers, Class<T> type) {
        throw new UnsupportedOperationException("Not yet implemented ..");
    }

    @Override
    public File writeFile(List<Map<String, Object>> list, String absoluteFilePath, Map<Integer, String> columnIndexMap, String[] headers) {
        throw new UnsupportedOperationException("Not yet implemented ..");
    }

    @Override
    public File writeFile(List<Map<String, Object>> list, String absoluteFilePath, Map<Integer, String> columnIndexMap) {
        throw new UnsupportedOperationException("Not yet implemented ..");
    }

    @Override
    public File writeFile(BatchIterator<Map<String, Object>> list, String absoluteFilePath, Map<Integer, String> columnIndexMap, String[] headers) {
        throw new UnsupportedOperationException("Not yet implemented ..");
    }

    @Override
    public <T> OutputStreamProperties writeFileToOutputStream(List<T> list, String[] headers, Class<T> type, OutputStream outputStream) {
        throw new UnsupportedOperationException("Not yet implemented ..");
    }



    //reader - not general
    @Override
    protected <T> T convert(CSVRecord row, String template, Class<T> type) {
        throw new UnsupportedOperationException("Not yet implemented ..");
    }

    @Override
    protected <T> T convert(CSVRecord row, Class<T> type) {
        throw new UnsupportedOperationException("Not yet implemented ..");
    }

    @Override
    protected <T> FileBatchIterator<T, CSVRecord> getBatchIterator(File file, Charset charset, int batchSize, int noOfLinesToIgnore, Class<T> type, String template) {
        throw new UnsupportedOperationException("Not yet implemented ..");
    }

    @Override
    protected <T> FileBatchIterator<T, CSVRecord> getBatchIterator(InputStream inputStream, Charset charset, int batchSize, int noOfLinesToIgnore, Class<T> type, String template) {
        throw new UnsupportedOperationException("Not yet implemented ..");
    }
}
