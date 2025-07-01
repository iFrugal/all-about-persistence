package lazydevs.mapper.utils.engine;


import freemarker.ext.util.WrapperTemplateModel;
import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.file.FileUtils;
import freemarker.template.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;


public class TemplateEngine {

    private static final TemplateEngine INSTANCE = new TemplateEngine();

    public static TemplateEngine getInstance(){
        return INSTANCE;
    }
    private Configuration configuration;

    private TemplateEngine() {
        init();
    }

    private void init() {
        this.configuration = new Configuration(new Version(2, 3, 23));
        this.configuration.setDefaultEncoding("UTF-8");
        this.configuration.setLocale(Locale.US);
        this.configuration.setNumberFormat("computer");
        this.configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    public void loadScript(String scriptText){
        ScriptEngines.JAVASCRIPT.loadScript(scriptText);
    }

    public void loadScriptFromFile(String filePath){
        ScriptEngines.JAVASCRIPT.loadScriptFromFile(filePath);
    }

    public String generate(String templateSource, Map<String, Object> data) {
            StringWriter stringWriter = new StringWriter();
            generate(stringWriter, templateSource, data);
            return stringWriter.toString();
    }

    public void generate(Writer writer, String templateSource, Map<String, Object> datapoints) {
        try {
            Map<String, Object> data = new HashMap<>(datapoints);
            data.put("uuid", (TemplateMethodModelEx)(list) -> UUID.randomUUID().toString());
            data.put("eval", new Eval());
            //data.put("evalObject", new EvalObject());
            //data.put("evalJson", new EvalObjectAsJson());
            data.put("file", new FileAsString());
            data.put("trim",new Trim());
            data.put("js", new JavaScript());
            data.put("serde_deserializeToMap", new TemplateMethodModelEx() {
                @Override
                public Object exec(List arguments) throws TemplateModelException {
                    if(arguments.size() != 1 ){
                        throw new IllegalArgumentException("Method 'serde_deserializeToMap' requires exactly 1 String input");
                    }
                    return SerDe.JSON.deserializeToMap(String.valueOf(arguments.get(0)));
                }
            });

            data.put("serde_serialize", new TemplateMethodModelEx() {
                @Override
                public Object exec(List arguments) throws TemplateModelException {
                    if(arguments.size() != 1 ){
                        throw new IllegalArgumentException("Method 'serde_serialize' requires exactly 1 Object");
                    }
                    Object obj = arguments.get(0);

                    // Unwrap FreeMarker's DefaultMapAdapter to get the actual Java object
                    if (obj instanceof WrapperTemplateModel) {
                        try {
                            obj = ((WrapperTemplateModel) obj).getWrappedObject();
                        } catch (Exception e) {
                            throw new TemplateModelException("Failed to unwrap object", e);
                        }
                    }
                    return SerDe.JSON.serialize(obj);
                }
            });

            Template e = new Template("", templateSource, this.configuration);
            e.process(data, writer);
        } catch (TemplateException | IOException e) {
            throw new RuntimeException("Exception when replacing values " + e.getMessage(), e);
        }
    }


    private class JavaScript implements TemplateMethodModelEx{

        @Override
        public Object exec(List arguments){
            if(arguments.size() < 1 ){
                throw new IllegalArgumentException("Method 'js' requires at least 1 String input stating the functionName");
            }
            try {
                String functionName = ((SimpleScalar) arguments.get(0)).getAsString();
                arguments = new ArrayList(arguments);
                Object[] args = new Object[arguments.size() - 1];
                for(int i = 1; i < arguments.size(); i++){
                    args[i-1] = arguments.get(i);
                }
                return ScriptEngines.JAVASCRIPT.invokeFunction(functionName, args);
            } catch (Exception e) {
                throw new RuntimeException("Error while executing javascript function", e);
            }
        }
    }

    private class FileAsString implements TemplateMethodModelEx{

        @Override
        public Object exec(List arguments) throws TemplateModelException {
            if(arguments.size() != 1 ){
                throw new IllegalArgumentException("Method 'file' requires exactly 1 String input");
            }
            String filePath = null;
            try {
                filePath = ((SimpleScalar) arguments.get(0)).getAsString();
                return eval(FileUtils.readFileAsString(filePath));
            } catch (Exception e) {
                throw new RuntimeException("Error While reading file as string from filePath = "+ filePath, e);
            }
        }
    }

    private class Eval implements TemplateMethodModelEx {
        @Override
        public Object exec(List arguments) throws TemplateModelException {
            if(arguments.size() != 1 ){
                throw new IllegalArgumentException("Method 'eval' requires exactly 1 String input");
            }
            return eval(((SimpleScalar) arguments.get(0)).getAsString());
        }
    }

   /* private static class EvalObject implements TemplateMethodModelEx {
        @Override
        public Object exec(List arguments) throws TemplateModelException {
            if(arguments.size() != 1 ){
                throw new IllegalArgumentException("Method 'evalObject' requires exactly 1 String input");
            }
            return evalObject(((SimpleScalar) arguments.get(0)).getAsString());
        }
    }

    private static class EvalObjectAsJson implements TemplateMethodModelEx {
        @Override
        public Object exec(List arguments) throws TemplateModelException {
            if(arguments.size() != 1 ){
                throw new IllegalArgumentException("Method 'evalJson' requires exactly 1 String input");
            }
            return evalJson(((SimpleScalar) arguments.get(0)).getAsString());
        }
    }*/




    private String eval(String template){
        return generate(template, new HashMap<>());
    }

    /*private static Object evalObject(String expression){
        validateExpression(expression);
        String arr[] = expression.trim().replace("$", "").replace("{", "").replace("}", "").split("\\.");
        Map<String, Object> map = INSTANCE;
        for(int i = 0; i < arr.length; i++){
            if(i == arr.length -1){//last element
                return map.get(arr[i]);
            }else{
                map = (Map<String, Object>)map.get(arr[i]);
        }
        return null;
    }*/

    /*public static String evalJson(String expression){
        return JsonUtils.toJson((evalObject(expression)), true);
    }*/

    private static void validateExpression(String expression){
        //validate if it is in format ${[a-z.A-Z.0-9]}
    }



    private class Trim implements TemplateMethodModelEx {


        @Override
        public Object exec(List arguments) throws TemplateModelException {

            if(arguments.size() != 1){
                throw new IllegalArgumentException("Method 'Trim' requires exactly 1 String input");
            }
            return ((SimpleScalar) arguments.get(0)).getAsString().trim();
        }
    }
}
