package lazydevs.mapper.file;

import lazydevs.mapper.utils.BatchIterator;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface FileMapper {

    <T> List<T> readFile(String filePath, Class<T> type);
    <T> List<T> readFile(String filePath, Class<T> type,int noOflinestoIngore);

    <T> List<T> readFile(InputStream inputStream, Class<T> type, String rowTransformationTemplate, int noOflinestoIngore);

    <T> List<T> readFile(InputStream inputStream, Class<T> type);

    <T> List<T> readFile(String file, String rowTransformationTemplate, Class<T> type);
    <T> List<T> readFile(String file, String rowTransformationTemplate, Class<T> type,int noOflinestoIngore);


    List<Map<String, Object>> readFile(String filePath, String rowTransformationTemplate);
    List<Map<String, Object>> readFile(String filePath, String rowTransformationTemplate, int noOflinestoIngore);

    BatchIterator<Map<String, Object>> readFileInBatches(String filePath, String rowTransformationTemplate, int batchSize);
    BatchIterator<Map<String, Object>> readFileInBatches(String filePath, String rowTransformationTemplate, int batchSize,int noOflinestoIngore);
    BatchIterator<Map<String, Object>> readFileInBatches(InputStream inputStream, String rowTransformationTemplate, int batchSize,int noOflinestoIngore);



    <T> BatchIterator<T> readFileInBatches(String filePath, String rowTransformationTemplate, Class<T> type, int batchSize);
    <T> BatchIterator<T> readFileInBatches(String filePath, String rowTransformationTemplate, Class<T> type, int batchSize,int noOflinestoIngore);
    <T> BatchIterator<T> readFileInBatches(InputStream inputStream, String rowTransformationTemplate, Class<T> type, int batchSize,int noOflinestoIngore);


    <T> File writeFile(List<T> list, String absoluteFilePath, String[] headers, Class<T> type);
    File writeFile(List<Map<String, Object>> list, String absoluteFilePath, Map<Integer, String> columnIndexMap, String[] headers);
    File writeFile(List<Map<String, Object>> list, String absoluteFilePath, Map<Integer, String> columnIndexMap);
    File writeFile(BatchIterator<Map<String, Object>> list, String absoluteFilePath, Map<Integer, String> columnIndexMap, String[] headers);
    default   File writeFile(List<Map<String, Object>> list, String absoluteFilePath, String[] headers,String template,String delimiter)
    {
        throw new UnsupportedOperationException("work needs to be done....");
    };
    <T> OutputStreamProperties writeFileToOutputStream(List<T> list, String[] headers, Class<T> type, OutputStream outputStream);

    @Builder
    @Getter
    @ToString
    public static class OutputStreamProperties {
        private String contentType;
        private String contentDisposition;
        private long contentLength;
    }
}
