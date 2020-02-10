package ifrugal.persistence.utils.engine;

import ifrugal.persistence.utils.JsonUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

public class TemplateEngineTest {

    private TemplateEngine templateEngine = TemplateEngine.DEFAULT;

    private String str =
            "{\n" +
                    "  \"httpMethod\" : \"${httpMethod}\",\n" +
                    "  \"url\" : \"u\",\n" +
                    "  \"headers\" : {\n" +
                    "    \"a\" : \"a1\",\n" +
                    "    \"b\" : \"b1\"\n" +
                    "  },\n" +
                    "  \"payload\" : \"p\"\n" +
                    "}";

    @Test
    public void testGenerate(){
        Map<String, Object> map = new HashMap<>();
        map.put("httpMethod", "GET");
        Assert.assertEquals(JsonUtils.fromJson(templateEngine.generate(str, map), Map.class).get("httpMethod"), "GET");
    }

    @Test
    public void testGenerateUsingFileFucntion() throws IOException {
        String fileName = "abc.txt";
        FileUtils.writeToFile(fileName, "POSTO");
        str = str.replace("${httpMethod}", String.format("${file('%s')}", fileName));
        System.out.println(str);
        Map<String, Object> map = new HashMap<>();
        //map.put("httpMethod", "${file('abc.txt')}");
        Assert.assertEquals(JsonUtils.fromJson(templateEngine.generate(str, map), Map.class).get("httpMethod"), "POSTO");
        FileUtils.delete(fileName);
    }

    @Test
    public void testGenerateUsingFileFucntion1(){
        String fileName = "abc.txt";
        FileUtils.writeToFile(fileName, "POSTO-1");
        str = str.replace("${httpMethod}", "${file('${fileName}')}");
        System.out.println(str);
        Map<String, Object> map = new HashMap<>();
        map.put("fileName", fileName);
        //map.put("httpMethod", "${file('abc.txt')}");
        Assert.assertEquals(JsonUtils.fromJson(templateEngine.generate(str, map), Map.class).get("httpMethod"), "POSTO-1");
        FileUtils.delete(fileName);
    }

    @Test
    public void testGenerateUsingJavaScriptFunction() throws Exception{
        ScriptEngines.JAVASCRIPT.loadScript(FileUtils.readFileAsString(getClass().getClassLoader().getResource("script.js").getPath()));
        //assertNull(TemplateEngine.getInstance().generate("${js('f1')}", new HashMap<>()));
        assertEquals(templateEngine.generate("${js('f2')}", new HashMap<>()), "F2");
        assertEquals(templateEngine.generate("${js('f3', 'Abhijeet')}", new HashMap<>()), "F3-Abhijeet");
    }

    @Test
    public void testTrim() throws Exception {

        String str1 =
                "{\n" +
                        "  \"httpMethod\" : \"${trim('${httpMethod}')}\",\n" +
                        "  \"url\" : \"u\",\n" +
                        "  \"headers\" : {\n" +
                        "    \"a\" : \"a1\",\n" +
                        "    \"b\" : \"b1\"\n" +
                        "  },\n" +
                        "  \"payload\" : \"p\"\n" +
                        "}";

        Map<String, Object> map = new HashMap<>();
        map.put("httpMethod", "  GET  ");
        Assert.assertEquals(JsonUtils.fromJson(templateEngine.generate(str1, map), Map.class).get("httpMethod"), "GET");
    }

    @Test
    public void testLTrim() throws Exception {

        String str1 =
                "{\n" +
                        "  \"httpMethod\" : \"${ltrim('${httpMethod}')}\",\n" +
                        "  \"url\" : \"u\",\n" +
                        "  \"headers\" : {\n" +
                        "    \"a\" : \"a1\",\n" +
                        "    \"b\" : \"b1\"\n" +
                        "  },\n" +
                        "  \"payload\" : \"p\"\n" +
                        "}";

        Map<String, Object> map = new HashMap<>();
        map.put("httpMethod", "  GET  ");
        Assert.assertEquals(JsonUtils.fromJson(templateEngine.generate(str1, map), Map.class).get("httpMethod"), "GET  ");
    }


    @Test
    public void testRTrim() throws Exception {

        String str1 =
                "{\n" +
                        "  \"httpMethod\" : \"${rtrim('${httpMethod}')}\",\n" +
                        "  \"url\" : \"u\",\n" +
                        "  \"headers\" : {\n" +
                        "    \"a\" : \"a1\",\n" +
                        "    \"b\" : \"b1\"\n" +
                        "  },\n" +
                        "  \"payload\" : \"p\"\n" +
                        "}";

        Map<String, Object> map = new HashMap<>();
        map.put("httpMethod", "  GET  ");
        Assert.assertEquals(JsonUtils.fromJson(templateEngine.generate(str1, map), Map.class).get("httpMethod"), "  GET");
    }

}