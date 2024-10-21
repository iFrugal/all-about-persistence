package lazydevs.mapper.utils.engine;

import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.file.FileUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.thymeleaf.context.Context;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static lazydevs.mapper.utils.engine.ScriptEngines.JAVASCRIPT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class TemplateEngineTest {

    private TemplateEngine templateEngine = TemplateEngine.getInstance();

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
        Assert.assertEquals(SerDe.JSON.deserialize(templateEngine.generate(str, map), Map.class).get("httpMethod"), "GET");
    }


    @Test
    public void testGenerateUsingFileFucntion(){
        String fileName = "abc.txt";
        FileUtils.writeToFile(fileName, "POSTO");
        str = str.replace("${httpMethod}", String.format("${file('%s')}", fileName));
        System.out.println(str);
        Map<String, Object> map = new HashMap<>();
        //map.put("httpMethod", "${file('abc.txt')}");
        Assert.assertEquals(SerDe.JSON.deserialize(templateEngine.generate(str, map), Map.class).get("httpMethod"), "POSTO");
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
        Assert.assertEquals(SerDe.JSON.deserialize(templateEngine.generate(str, map), Map.class).get("httpMethod"), "POSTO-1");
        FileUtils.delete(fileName);
    }

    @Test
    public void testGenerateUsingJavaScriptFunction() throws Exception{
        JAVASCRIPT.loadScript(FileUtils.readFileAsString(getClass().getClassLoader().getResource("script.js").getPath()));
        //assertNull(TemplateEngine.getInstance().generate("${js('f1')}", new HashMap<>()));
        assertEquals(TemplateEngine.getInstance().generate("${js('f2')}", new HashMap<>()), "F2");
        assertEquals(TemplateEngine.getInstance().generate("${js('f3', 'Abhijeet')}", new HashMap<>()), "F3-Abhijeet");
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
        Assert.assertEquals(SerDe.JSON.deserialize(templateEngine.generate(str1, map), Map.class).get("httpMethod"), "GET");
    }


    @Test
    public void switchCase(){
        String template = "<#assign x>\n" +
                "  <#switch a>\n" +
                "\t  <#case \"a1\">a1-zzz<#break>\n" +
                "\t  <#case \"a2\">a2-zzz<#break>\n" +
                "\t  <#case \"a3\">a3-zzz<#break>\n" +
                "\t  <#default>an-zzzz\n" +
                "\t</#switch>\n" +
                "</#assign>\n" +
                "Hello My Name is ${x}";
        System.out.println(template);
        Arrays.asList("a1", "a2", "a3").forEach(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("a", a);
            Assert.assertEquals(templateEngine.generate(template, map), "Hello My Name is "+a+"-zzz");
        });

    }

    @Test
    public void testAssignments(){
        String template = "" +
                "<#assign isImpacted>\n" +
                "  <#switch i2>\n" +
                "\t  <#case \"Yes\">true<#break>\n" +
                "\t  <#case \"No\">false<#break>\n" +
                "\t  <#default>Invalid 3rd(index=2) Column\n" +
                "\t</#switch>\n" +
                "</#assign>\n" +
                "<#assign impactedReasonCd>\n" +
                "<#if isImpacted == \"false\" && i3 != \"No\">Invalid 4th(index=3) Column\n" +
                "  <#else>\n" +
                "  \t<#switch i3>\n" +
                "\t  <#case \"No\">\"1\"<#break>\n" +
                "\t  <#case \"Yes – no agency exception/extension\">\"2\"<#break>\n" +
                "\t  <#case \"Yes – informal agency exception/extension\">\"3\"<#break>\n" +
                "\t  <#case \"Yes – formal agency exception/extension\">\"4\"<#break>\n" +
                "\t  <#case \"Yes – unable to complete application and attachments due to external restrictions\">\"5\"<#break>\n" +
                "\t  <#default>Invalid 4th(index=3) Column\n" +
                "\t</#switch>\n" +
                "  </#if>\n" +
                "</#assign>\n" +
                "<#assign endDateProvided>\n" +
                "  <#if i4 == \"Yes\" && impactedReasonCd != 3 && impactedReasonCd != 4>Invalid 5th(index=4) Column\n" +
                "  <#else>\n" +
                "    <#switch i4>\n" +
                "\t<#case \"Yes\">true<#break>\n" +
                "\t<#case \"No\">false<#break>\n" +
                "\t<#default>Invalid 5th(index=4) Column\n" +
                "\t</#switch>\n" +
                "</#if>\n" +
                "</#assign>\n" +
                "<#assign endDate>\n" +
                "<#if i5== \"Not Required\">\n" +
                "\t<#if endDateProvided == \"true\">Invalid 6th(index=5) Column\n" +
                "\t</#if>\n" +
                "<#else>${i5}\n" +
                "</#if>\n" +
                "</#assign>\n" +
                "Output = ${i1} | ${isImpacted} | ${impactedReasonCd} | ${endDateProvided} | ${endDate}";
        System.out.println(template);
        /*Assert.assertEquals(templateEngine.generate(template, getMap("i0=Mexico,i1=1234,i2=No,i3=Yes – no agency exception/extension,i4=No,i5=Not Required"))
                , "Output = 1234 | false | Invalid 4th(index=3) Column | false | Not Required");*/
    }

    @Test
    public void testList(){
        Map<String, Object> map = new HashMap<>();
        map.put("list1", Arrays.asList("s1", "s2", "s3"));

        String template = "A____ ${(list1[0])!'-'}  ${(list1[1])!'-'} ${(list1[2])!'-'} ${(list1[3])!'-'} _____B";
        System.out.println(TemplateEngine.getInstance().generate(template, map));
    }


    static Map<String, Object> getMap(String s){
        Map<String, Object> map = new LinkedHashMap<>();
        Arrays.stream(s.split(",")).forEach(token-> {
            String[] keyValPair = token.split("=");
            map.put(keyValPair[0].trim(), keyValPair[1].trim());
        });
        return map;
    }

    @Test
    public void testTransform(){
        Input input1 = new Input("a1", "b1", "c1", "d1", new String[]{"x1", "y1"}, new SubInput("p1", "q1"));
        Input input2 = new Input("a2", "b2", "c2", "d2", new String[]{"x2", "y2"}, new SubInput("p2", "q2"));
        List<Map<String, Object>> inputList = Arrays.asList(SerDe.JSON.toMap(input1), SerDe.JSON.toMap(input2));

        Map<String, Object> data = new HashMap<>();
        data.put("inputList", inputList);

        String template = "[\n" +
                "<#list inputList as input> \n" +
                "\t{\n" +
                "\t\t\"a\" : \"${input.ax}\",\n" +
                "\t\t\"b\" : \"${input.bx}\",\n" +
                "\t\t\"c\" : [\n" +
                "\t\t\t\"${input.cx}\", \"${input.dx}\"\n" +
                "\t\t]\t\n" +
                "\n" +
                "\t},\n" +
                "</#list>\n" +
                "]";
        String str = TemplateEngine.getInstance().generate(template, data);
        System.out.println(str);
        //Map<String, Object>  outputMap = SerDe.JSON.deserializeToMap(str);
        //Output output = SerDe.JSON.deserialize(str, Output.class);


    }


    @Test
    public void testThymeleaf(){
        Map<String, Object> map = new HashMap<>();
        map.put("a", "a1");
        map.put("b", "b1");
        org.thymeleaf.TemplateEngine thymeLeafEngine = new org.thymeleaf.TemplateEngine();
        StringWriter writer = new StringWriter();
        Context context = new Context();
        context.setVariable("message", "Welcome to thymeleaf article");
        context.setVariables(map);
        thymeLeafEngine.process("<th th:text=${a}/>", context, writer);
        writer.flush();
        System.out.println(writer.toString());
    }

    @AllArgsConstructor @Getter
    static class Input{
        private String ax, bx, cx, dx;
        private String[] arr;
        private SubInput subInput;


    }

    @AllArgsConstructor @Getter
    private static class SubInput{
        private String px, qx;
    }



    @AllArgsConstructor @Getter @Setter @NoArgsConstructor
    static class Output{
        private String a, b;
        private String[]c;
        private String[]d;

    }
}