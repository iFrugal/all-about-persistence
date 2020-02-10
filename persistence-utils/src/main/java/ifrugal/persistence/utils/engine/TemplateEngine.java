package ifrugal.persistence.utils.engine;

import freemarker.template.*;
import ifrugal.persistence.utils.JsonUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TemplateEngine {
    public static TemplateEngine DEFAULT = new TemplateEngine();
    private final Configuration configuration;

    private TemplateEngine(){
        this(getDefault());
    }

    public TemplateEngine(@NonNull Configuration configuration){
        this.configuration = configuration;
    }

    private static Configuration getDefault(){
        Configuration configuration = new Configuration(new Version(2, 3, 29));
        configuration.setDefaultEncoding("UTF-8");
        configuration.setLocale(Locale.US);
        configuration.setNumberFormat("computer");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        return configuration;
    }

    public String generate(String templateAsString, Map<String, Object> data, Codex... codexes){
        try{
            if(null != codexes && codexes.length > 0){
                Arrays.stream(codexes).forEach(codex -> data.put(codex.getName(), codex.getTemplateMethodModelEx()));
            }
            Template template = new Template("", templateAsString, this.configuration);
            StringWriter sw = new StringWriter();
            template.process(data, sw);
            return sw.toString();
        }catch (Exception e){
            throw new RuntimeException(String.format("Error while generating from template = (see below) \n%s\n and datapoints = %s ",templateAsString, data), e);
        }
    }

    public <T> T generate(String templateAsString, Map<String, Object> data, Class<T> clazz, Codex... codexes){
        return JsonUtils.fromJson(generate(templateAsString, data, codexes), clazz);
    }

    public <T> List<T> generateToList(String templateAsString, Map<String, Object> data, Class<T> clazz, Codex... codexes){
        return JsonUtils.fromJsonToList(generate(templateAsString, data, codexes), clazz);
    }

    @Builder @Getter
    public static class Codex{
        private final String name;
        private final TemplateMethodModelEx templateMethodModelEx;
    }
}
