package lazydevs.mapper.file.flat.dsv;

import lazydevs.mapper.file.flat.FlatFileMapper;
import lazydevs.mapper.file.flat.annotation.Column;
import lazydevs.mapper.file.utils.FileBatchIterator;
import lazydevs.mapper.utils.BatchIterator;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

import static java.lang.String.valueOf;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

/**
 * Created by Abhijeet Rai on 08/27/2018.
 */
public class DsvMapper extends FlatFileMapper<String> {
    private final String delimiter;
    @Setter
    private String commentSymbol = "#";

    public DsvMapper(String delimiter) {
        this.delimiter = delimiter;
    }

    private abstract class DsvFileBatchIterator<T> extends FileBatchIterator<T, String> {
        private final LineIterator lineIterator;

        public DsvFileBatchIterator(LineIterator lineIterator, int batchSize, int noOfLinesToIgnore) {
            super(lineIterator, batchSize, noOfLinesToIgnore);
            this.lineIterator = lineIterator;
        }

        @Override
        public boolean isRowToIgnore(String row) {
            return null != commentSymbol && row.startsWith(commentSymbol);
        }

        @Override
        public void close() {
            if (null != lineIterator) {
                try {
                    lineIterator.close();
                } catch (Exception e) {
                    throw new RuntimeException("Error while closing LineIterator.", e);
                }
            }
        }
    }

    @Override
    protected <T> T convert(String line, Class<T> type) {
        return convert(getColumnIndexToValueMap(line), type);
    }

    @Override
    protected <T> FileBatchIterator<T, String> getBatchIterator(File file, Charset charset, int batchSize, int noOfLinesToIgnore, Class<T> type,String template) {
        try {
            return getBatchIterator(FileUtils.lineIterator(file, charset.name()), batchSize, noOfLinesToIgnore, type,template);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected <T> FileBatchIterator<T, String> getBatchIterator(InputStream inputStream, Charset charset, int batchSize, int noOfLinesToIgnore, Class<T> type,String template) {

        return getBatchIterator(IOUtils.lineIterator(inputStream, charset.name()), batchSize, noOfLinesToIgnore, type,template);

    }

    @Override
    protected FileBatchIterator<Map<String, Object>, String> getBatchIterator(File file, Charset charset, int batchSize, int noOfLinesToIgnore,String rowTransformationTemplate) {
        try {
            return getBatchIterator(FileUtils.lineIterator(file, charset.name()), batchSize, noOfLinesToIgnore,rowTransformationTemplate);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected FileBatchIterator<Map<String, Object>, String> getBatchIterator(InputStream inputStream, Charset charset, int batchSize, int noOfLinesToIgnore,String rowTransformationTemplate) {

            return getBatchIterator(IOUtils.lineIterator(inputStream, charset.name()), batchSize, noOfLinesToIgnore,rowTransformationTemplate);

    }

    private <T> FileBatchIterator<T, String> getBatchIterator(LineIterator lineIterator, int batchSize, int noOfLinesToIgnore, Class<T> type,String template) {
        return new DsvFileBatchIterator<T>(lineIterator, batchSize, noOfLinesToIgnore) {
            @Override
            public T map(String line) {
                return null==template?convert(line, type):convert(line,template,type);
            }

        };
    }


    private FileBatchIterator<Map<String,Object>, String> getBatchIterator(LineIterator lineIterator, int batchSize, int noOfLinesToIgnore,String template) {
        return new DsvFileBatchIterator<Map<String,Object>>(lineIterator, batchSize, noOfLinesToIgnore) {
            @Override
            public Map<String,Object> map(String line) {
                return convert(line,template);
            }

        };
    }

    @Override
    protected Map<String, Object> convert(String line, String template) {
        if (template == null)
            return getColumnIndexToValueMap(line).entrySet().stream().collect(toMap(
                    entry -> valueOf(entry.getKey()), Map.Entry::getValue));
        return convert(getColumnIndexToValueMap(line), template, Map.class);
    }

    @Override
    protected <T> T convert(String line, String template, Class<T> type) {
        if (template == null)
            return convert(getColumnIndexToValueMap(line), type);
        return convert(getColumnIndexToValueMap(line), template, type);
    }


    private Map<Integer, String> getColumnIndexToValueMap(String line) {
        Map<Integer, String> map = new HashMap<>();
        String[] arr = line.split(this.delimiter);
        for (int i = 0; i < arr.length; i++) {
            map.put(i, arr[i].replaceAll(";", ","));
        }
        return map;
    }




    protected <T> String getLine(T t, ClassAttributes classAttributes, BiFunction<String, T , Object> valueFunction) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= classAttributes.getMaxIndex(); i++) {
            if (classAttributes.getColumnIndexMap().containsKey(i)) {
                Object val = valueFunction.apply(classAttributes.getColumnIndexMap().get(i), t);
                try {

                    if (null != val) {
                        String s = valueOf(val);
                        sb.append(s.replaceAll(",", ";"));
                    } else {
                        sb.append("");
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Exception while serializing field = " + classAttributes.getColumnIndexMap().get(i), e);
                }
            } else {
                sb.append("");
            }
            sb.append(delimiter);
        }
        return sb.toString();
    }

    @Override
    public <T> File writeFile(List<T> list, String absoluteFilePath, String[] headers, Class<T> type) {
        String headerLine = null == headers || 0 == headers.length ? null : stream(headers).collect(joining(delimiter));
        return writeAFile(list, absoluteFilePath, headerLine, type);
    }

    @Override
    public File writeFile(List<Map<String, Object>> list, String absoluteFilePath, Map<Integer, String> columnIndexMap, String[] headers) {
        throw new UnsupportedOperationException("work needs to be done....");
    }

    @Override
    public File writeFile(List<Map<String, Object>> list, String absoluteFilePath, Map<Integer, String> columnIndexMap) {
        throw new UnsupportedOperationException("work needs to be done....");
    }

    @Override
    public File writeFile(BatchIterator<Map<String, Object>> batchIterator, String absoluteFilePath, Map<Integer, String> columnIndexMap, String[] headers) {
        String headerLine = null == headers || 0 == headers.length ? null : stream(headers).collect(joining(delimiter));
        Path filePath = Paths.get(absoluteFilePath);
        ClassAttributes classAttributes = getClassAttributes(columnIndexMap);
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, Charset.forName("utf-8"))) {
            String line = null == headerLine || headerLine.isEmpty() ? stream(classAttributes.getHeaders()).collect(joining(delimiter)) : headerLine;
            line = replaceNewLineCharacter(line);
            if (null != line) {
                writer.write(line, 0, line.length());
                writer.newLine();
            }
            while(batchIterator.hasNext()){
                List<Map<String, Object>> list = batchIterator.next();
                writeAFile(list, writer, classAttributes, (fieldName, t)-> {
                    return ((Map<String, Object>)t).get(fieldName);
                });
            }
            return filePath.toFile();
        } catch (Exception e) {
            throw new RuntimeException("Exception occured while writing a file", e);
        }
    }


    @Override
    public <T> OutputStreamProperties writeFileToOutputStream(List<T> list, String[] headers, Class<T> type, OutputStream outputStream) {
        return writeToOutputStream(list,headers,type,outputStream,"csv");

    }


    private <T> File writeAFile(List<T> list, String absoluteFilePath, String headerLine, Class<T> type) {
        Path filePath = Paths.get(absoluteFilePath);
        String parentFolderPathStr = filePath.toFile().getParent();
        if (null != parentFolderPathStr && !parentFolderPathStr.isEmpty()) {
            new File(parentFolderPathStr).mkdirs();
        }
        ClassAttributes classAttributes = getClassAttributes(type);
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, Charset.forName("utf-8"))) {
            String line = null == headerLine || headerLine.isEmpty() ? stream(classAttributes.getHeaders()).collect(joining(delimiter)) : headerLine;
            line = replaceNewLineCharacter(line);
            if (null != line) {
                writer.write(line, 0, line.length());
                writer.newLine();
            }
             writeAFile(list, writer, classAttributes, (fieldName, tLocal)-> {
                try {
                    Field field;
                    try {
                        field = tLocal.getClass().getDeclaredField(fieldName);
                    }catch (NoSuchFieldException e){
                        field = tLocal.getClass().getSuperclass().getDeclaredField(fieldName);
                    }
                    field.setAccessible(true);
                    Object val = field.get(tLocal);
                    if (field.isAnnotationPresent(Column.Serialize.class)) {
                        Column.Serializer serializer = field.getAnnotation(Column.Serialize.class).using().newInstance();
                        return serializer.serialize(val);
                    } else if (field.isAnnotationPresent(Column.ToAndFroSerialize.class)) {
                        Column.ToAndFroSerializer serializer = field.getAnnotation(Column.ToAndFroSerialize.class).using().newInstance();
                        return serializer.serialize(val);
                    }
                    return val;
                }catch (Exception e){
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred while writing a file", e);
        }
        return filePath.toFile();
    }

    private <T> void writeAFile(List<T> list, BufferedWriter writer, ClassAttributes classAttributes, BiFunction<String, T , Object> valueFunction) {
        try {
            for (T t : list) {
                String line = replaceNewLineCharacter(getLine(t, classAttributes, valueFunction));
                writer.write(line, 0, line.length());
                writer.newLine();
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception occured while writing a file", e);
        }
    }

    public static String replaceNewLineCharacter(String s) {
        if (null == s)
            return s;
        return s.replaceAll("\\n", "<br> ");
    }
}
