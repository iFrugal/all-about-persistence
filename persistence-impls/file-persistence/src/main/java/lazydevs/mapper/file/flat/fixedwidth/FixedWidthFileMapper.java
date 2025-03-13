package lazydevs.mapper.file.flat.fixedwidth;

import freemarker.template.SimpleNumber;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import lazydevs.mapper.file.flat.FlatFileMapper;
import lazydevs.mapper.file.utils.FileBatchIterator;
import lazydevs.mapper.utils.BatchIterator;
import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.engine.TemplateEngine;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FixedWidthFileMapper extends FlatFileMapper<String>{

    @Setter private int numberOfRowsToIgnore = 0;
    @Setter private String commentSymbol = "#";

    private abstract class FixedWidthIterator<T> extends FileBatchIterator<T, String> {
        private final LineIterator lineIterator;

        public FixedWidthIterator(LineIterator lineIterator, int batchSize, int noOfLinesToIgnore) {
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
    public Map<String, Object> convert(String line, String template){
        return SerDe.JSON.deserialize(generate(line, template), Map.class);
    }

    @Override
    public <T> T convert(String line, String template, Class<T> type){
        return SerDe.JSON.deserialize(generate(line, template), type);
    }

    @Override
    public <T> T convert(String line, Class<T> type) {
        throw new UnsupportedOperationException("Type bound mapping is still not operational.. Work needs to be done. Annotations needs to be modified to accept substr params.");
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
        return new FixedWidthIterator<T>(lineIterator, batchSize, noOfLinesToIgnore) {
            @Override
            public T map(String line) {
                return null==template?convert(line, type):convert(line,template,type);
            }

        };
    }


    private FileBatchIterator<Map<String,Object>, String> getBatchIterator(LineIterator lineIterator, int batchSize, int noOfLinesToIgnore,String template) {
        return new FixedWidthIterator<Map<String,Object>>(lineIterator, batchSize, noOfLinesToIgnore) {
            @Override
            public Map<String,Object> map(String line) {
                return convert(line,template);
            }

        };
    }

    private String generate(String line, String template){
        TemplateEngine templateEngine = TemplateEngine.getInstance();
        Map<String, Object> datapoints = new HashMap<>();
        datapoints.put("substr", new Substring(line));
        return templateEngine.generate(template, datapoints);
    }


    private class Substring implements TemplateMethodModelEx {
        private final String line;

        private Substring(String line) {
            this.line = line;
        }

        @Override
        public Object exec(List arguments) throws TemplateModelException {
            if(arguments.size() > 2 ){
                throw new IllegalArgumentException("Method 'substr' requires less than 2 input Method signature = public String substr(int start, int end) or public String substr(int start);");
            }
            int start = -1, end = -1;
            try {
                start = ((SimpleNumber)arguments.get(0)).getAsNumber().intValue();
                if(arguments.size()==2) {
                    end = ((SimpleNumber) arguments.get(1)).getAsNumber().intValue();
                    return line.substring(start-1, end);
                }
                return line.substring(start-1);
            } catch (Exception e) {
                throw new RuntimeException(
                        String.format("Error while doing substring. line = '%s', startIndex = %s, endIndex = %s", line, start, end)
                        , e);
            }
        }
    }

    @Override
    public <T> File writeFile(List<T> list, String absoluteFilePath, String[] headers, Class<T> type) {
        throw new UnsupportedOperationException("work needs to be done....");
    }

    @Override
    public File writeFile(List<Map<String, Object>> list, String absoluteFilePath, Map<Integer, String> columnIndexMap, String[] headers) {
        throw new UnsupportedOperationException("work needs to be done....");
    }

    @Override
    public File writeFile(BatchIterator<Map<String, Object>> list, String absoluteFilePath, Map<Integer, String> columnIndexMap, String[] headers) {
        throw new UnsupportedOperationException("work needs to be done....");
    }

    @Override
    public File writeFile(List<Map<String, Object>> list, String absoluteFilePath, Map<Integer, String> columnIndexMap) {
        throw new UnsupportedOperationException("work needs to be done....");
    }

    @Override
    public <T> OutputStreamProperties writeFileToOutputStream(List<T> list, String[] headers, Class<T> type, OutputStream outputStream) {
        throw new UnsupportedOperationException("work needs to be done....");
    }

}
