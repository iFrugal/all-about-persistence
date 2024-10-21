package lazydevs.mapper.file.flat;

import lazydevs.mapper.file.FileMapper;
import lazydevs.mapper.file.flat.annotation.Column;
import lazydevs.mapper.utils.BatchIterator;
import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.engine.TemplateEngine;
import lazydevs.mapper.file.utils.FileBatchIterator;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.formula.functions.T;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public abstract class FlatFileMapper<R> implements FileMapper {
    protected abstract Map<String, Object> convert(R row, String template);

    protected abstract <T> T convert(R row, String template, Class<T> type);

    protected abstract  <T> T convert(R row, Class<T> type);

    protected abstract <T> FileBatchIterator<T,R> getBatchIterator(File file, Charset charset, int batchSize,int noOfLinesToIgnore,Class<T> type,String template);

    protected abstract <T> FileBatchIterator<T,R> getBatchIterator(InputStream inputStream, Charset charset, int batchSize,int noOfLinesToIgnore,Class<T> type,String template);

    protected abstract  FileBatchIterator<Map<String,Object>,R> getBatchIterator(File file, Charset charset, int batchSize,int noOfLinesToIgnore,String template);

    protected abstract  FileBatchIterator<Map<String,Object>,R> getBatchIterator(InputStream inputStream, Charset charset, int batchSize,int noOfLinesToIgnore,String template);

    //protected abstract <T> getColumnIndexToValueMap(T row)

    @Override
    public <T> List<T> readFile(InputStream inputStream, Class<T> type) {
        List<T> list = new ArrayList<>();
        try (FileBatchIterator batchIterator = readFileInBatches(inputStream, type,10_000,null, 0))
            {
                while (batchIterator.hasNext()) {
                    list.addAll(batchIterator.next());
                }
            }
            return list;
    }

    @Override
    public <T> List<T> readFile(InputStream inputStream, Class<T> type, String template, int noOflinesToIgnore) {
        List<T> list = new ArrayList<>();
        try (FileBatchIterator batchIterator = readFileInBatches(inputStream, type,10_000, template, noOflinesToIgnore))
        {
            while (batchIterator.hasNext()) {
                list.addAll(batchIterator.next());
            }
        }
        return list;
    }

    private  <T> FileBatchIterator<T,R> readFileInBatches(InputStream inputStream, Class<T> type,int batchSize,
                                                          String template, int noOflinesToIgnore) {
        return getBatchIterator(inputStream, Charset.forName("UTF-8"), batchSize, noOflinesToIgnore, type, template);
    }

    @Override
    public <T> List<T> readFile(String filePath, Class<T> type) {
        return readFile( filePath,  type,0);
    }

    @Override
    public <T> List<T> readFile(String filePath, Class<T> type,int noOflinesToIgnore) {
        try (FileBatchIterator<T,R> batchIterator = getBatchIterator(new File(filePath), Charset.forName("UTF-8"), 10_000,noOflinesToIgnore,type,null)) {
            List<T> list = new ArrayList<>();
            batchIterator.forEachRemaining(batch -> {
                list.addAll(batch);
            });
            return list;
        }
    }

    @Override
    public List<Map<String, Object>> readFile(String filePath, String rowTransformationTemplate) {
       return readFile( filePath,  rowTransformationTemplate,0);
    }

    @Override
    public List<Map<String, Object>> readFile(String filePath, String rowTransformationTemplate,int noOfLinesToSkip) {
        try (FileBatchIterator<Map<String, Object>,R> batchIterator = getBatchIterator(new File(filePath), Charset.forName("UTF-8"), 10_000,noOfLinesToSkip,rowTransformationTemplate)) {
            List<Map<String, Object>> list = new ArrayList<>();
            batchIterator.forEachRemaining(batch -> list.addAll(batch));
            return list;
        }
    }
    @Override
    public <T> List<T> readFile(String filePath, String rowTransformationTemplate, Class<T> type) {
       return readFile( filePath,  rowTransformationTemplate,  type,0);
    }


    @Override
    public <T> List<T> readFile(String filePath, String rowTransformationTemplate, Class<T> type,int noOfLinesToSkip) {
        try (FileBatchIterator<T,R> batchIterator = getBatchIterator(new File(filePath), Charset.forName("UTF-8"), 10_000,noOfLinesToSkip,type,rowTransformationTemplate)){
            List<T> list = new ArrayList<>();
            batchIterator.forEachRemaining(batch -> list.addAll(batch));
            return list;
        }
    }

    @Override
    public BatchIterator<Map<String, Object>> readFileInBatches(String filePath, String rowTransformationTemplate, int batchSize) {
       return readFileInBatches( filePath,  rowTransformationTemplate,  batchSize,0);
    }

    @Override
    public BatchIterator<Map<String, Object>> readFileInBatches(String filePath, String rowTransformationTemplate, int batchSize,int noOfLinesToSkip) {
        return getBatchIterator(new File(filePath), Charset.forName("UTF-8"), batchSize, noOfLinesToSkip,rowTransformationTemplate);
    }

    @Override
    public BatchIterator<Map<String, Object>> readFileInBatches(InputStream inputStream, String rowTransformationTemplate, int batchSize, int noOflinestoIngore){
        return getBatchIterator(inputStream, Charset.forName("UTF-8"),  batchSize,  noOflinestoIngore, rowTransformationTemplate);
    }

    @Override
    public <T> BatchIterator<T> readFileInBatches(String filePath, String rowTransformationTemplate, Class<T> type, int batchSize) {
       return readFileInBatches( filePath,  rowTransformationTemplate,  type,  batchSize,0);
    }


    @Override
    public <T> BatchIterator<T> readFileInBatches(String filePath, String rowTransformationTemplate, Class<T> type, int batchSize,int noOfLinesToSkip) {
        return getBatchIterator(new File(filePath), Charset.forName("UTF-8"), batchSize,noOfLinesToSkip,type,rowTransformationTemplate);
    }

    @Override
    public <T> BatchIterator<T> readFileInBatches(InputStream inputStream, String rowTransformationTemplate, Class<T> type, int batchSize, int noOflinestoIngore) {
        return readFileInBatches(inputStream, type, batchSize, rowTransformationTemplate, noOflinestoIngore);
    }


    // Below code is to be used for DSV Mapper and Excel mapper only, it would make sense to move it to another interface..
    protected  <T> T convert(Map<Integer, String> columnIndexToValueMap, String template, Class<T> type) {
        TemplateEngine templateEngine = TemplateEngine.getInstance();
        Map<String, Object> datapoints = columnIndexToValueMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> "i" + entry.getKey(),
                        Map.Entry::getValue
                ));
        return SerDe.JSON.deserialize(templateEngine.generate(template, datapoints), type);
    }

    protected  <T> T convert(Map<Integer, String> columnIndexToValueMap, Class<T> type) {
        try {
            T t = (T) type.newInstance();
            List<Field> listOfFields = new ArrayList<>(Arrays.asList(type.getDeclaredFields()));
            listOfFields.addAll(Arrays.asList(type.getSuperclass().getDeclaredFields()));
            for (Field field : listOfFields) {
                Object value = null;
                field.setAccessible(true);
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    String stringVal = columnIndexToValueMap.get(column.index());
                    value = stringVal;
                    if (field.isAnnotationPresent(Column.DeSerialize.class)) {
                        Column.DeSerializer deSerializer = field.getAnnotation(Column.DeSerialize.class).using().newInstance();
                        try {
                            value = deSerializer.deSerialize(stringVal);
                        } catch (Exception e) {
                            throw new RuntimeException(String.format("value = %s. Exception occurred while mapping class = %s,field = %s, deserializeUsing = %s"
                                    , value, type.toString(), field.getName(), deSerializer)
                                    , e);
                        }
                    } else if (field.isAnnotationPresent(Column.ToAndFroSerialize.class)) {
                        Column.ToAndFroSerializer toAndFroSerializer = field.getAnnotation(Column.ToAndFroSerialize.class).using().newInstance();
                        try {
                            value = toAndFroSerializer.deSerialize(stringVal);
                        } catch (Exception e) {
                            throw new RuntimeException(String.format("value = %s. Exception occurred while mapping class = %s,field = %s, deserializeUsing = %s"
                                    , value, type.toString(), field.getName(), toAndFroSerializer)
                                    , e);
                        }
                    } else {
                        try {
                            value = getVal(field.getType(), stringVal, field.getName());
                        } catch (Exception e) {
                            throw new RuntimeException(format("Exception while getting value for field(name='%s', type='%s') from  object val(val='%s', type='%s')", field.getName(), field
                                    .getType(), value, null == value ? null : value.getClass()), e);
                        }
                    }

                } else if (field.isAnnotationPresent(Column.NestedColumn.class)) {
                    value = convert(columnIndexToValueMap, field.getType());
                }
                if (value != null) {
                    field.set(t, value);
                }
            }
            return t;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected Object getVal(Class<?> clzz, String val, String fieldName) {
        String javaTypeName = clzz.getName();
        switch (javaTypeName) {
            case "java.lang.String":
                return val;
            case "java.lang.Long":
                return (null == val || val.isEmpty()) ? null : Long.valueOf(val);
            case "long":
                return (null == val || val.isEmpty()) ? 0 : Long.valueOf(val);
            case "java.lang.Integer":
                return (null == val || val.isEmpty()) ? null : Integer.valueOf(val);
            case "int":
                return (null == val || val.isEmpty()) ? 0 : Integer.valueOf(val);
            case "java.lang.Double":
                return (null == val || val.isEmpty()) ? null : Double.valueOf(val);
            case "double":
                return (null == val || val.isEmpty()) ? 0.0 : Double.valueOf(val);
            //case "java.util.Date" : return Date.;
            default:
                throw new IllegalArgumentException(format("Class = '%s' is not handled while mapping File to Field = '%s'. Hint: Use '@Column.DeSerialize' annotation", clzz, fieldName));
        }
    }


    protected  <T> boolean isValidDto(T t) {
        Class<T> type = (Class<T>) t.getClass();
        boolean isFilterRequired = false;
        Column.FilterDoer filterDoer = null;
        if (type.isAnnotationPresent(Column.Filter.class)) {
            try {
                filterDoer = type.getAnnotation(Column.Filter.class).using().newInstance();
                isFilterRequired = true;
            } catch (Exception e) {

            }
        }
        if ((isFilterRequired && filterDoer.accept(t)) || !isFilterRequired)
            return true;

        return false;
    }

    protected  <T> OutputStreamProperties writeToOutputStream(List<T> list, String[] headers, Class<T> type, OutputStream outputStream, String fileExtension){
        File file = writeFile(list, "/tmp/" + UUID.randomUUID().toString() + "."+fileExtension, headers, type);
        if (null == file) {
            throw new RuntimeException("Cannot write a null file to output stream");
        }
        try (FileInputStream in = new FileInputStream(file)) {
            IOUtils.copy(in, outputStream);
        } catch (Exception e) {
            throw new RuntimeException("Error happened while exporting file", e);
        }
        return OutputStreamProperties.builder()
                .contentType("application/"+fileExtension)
                .contentDisposition("attachment; filename=" + file.getName())
                .contentLength(file.length())
                .build();
    }


    @Builder
    @ToString
    @Getter
    public static class ClassAttributes {
        private Map<Integer, String> columnIndexMap;
        private int maxIndex;
        private String[] headers;
    }

    protected  <T> ClassAttributes getClassAttributes(Class<T> type) {
        Map<Integer, String> columnIndexMap = new HashMap<>();
        int maxIndex = -1;
        List<Field> listOfFields = new ArrayList<>(Arrays.asList(type.getDeclaredFields()));
        listOfFields.addAll(Arrays.asList(type.getSuperclass().getDeclaredFields()));
        for (Field field : listOfFields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                maxIndex = column.index() < maxIndex ? maxIndex : column.index();
                columnIndexMap.put(column.index(), field.getName());
            }
        }
        String[] headers = new String[maxIndex+1];
        for (int i = 0; i <= maxIndex; i++) {
            if (columnIndexMap.containsKey(i)) {
                headers[i] = columnIndexMap.get(i);
            }else{
                headers[i] = "";
            }
        }
        return ClassAttributes.builder().columnIndexMap(columnIndexMap).maxIndex(maxIndex).headers(headers).build();
    }

    protected  <T> ClassAttributes getClassAttributes(Map<Integer, String> columnIndexMap) {
        int maxIndex = -1;
        for(Map.Entry<Integer, String> entry : columnIndexMap.entrySet()){
            maxIndex = Math.max(entry.getKey(), maxIndex);
        }

        String[] headers = new String[maxIndex+1];
        for (int i = 0; i <= maxIndex; i++) {
            if (columnIndexMap.containsKey(i)) {
                headers[i] = columnIndexMap.get(i);
            }else{
                headers[i] = "";
            }
        }
        return ClassAttributes.builder().columnIndexMap(columnIndexMap).maxIndex(maxIndex).headers(headers).build();
    }
}

