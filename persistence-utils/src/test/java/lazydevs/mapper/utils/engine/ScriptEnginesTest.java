package lazydevs.mapper.utils.engine;

import lazydevs.mapper.utils.file.FileUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

import static lazydevs.mapper.utils.engine.ScriptEngines.GROOVY;
import static lazydevs.mapper.utils.engine.ScriptEngines.JAVASCRIPT;
import static lazydevs.mapper.utils.engine.TemplateEngineTest.getMap;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class ScriptEnginesTest {

    @Test
    public void testJavaScriptViaFile(){
        JAVASCRIPT.loadScriptFromFile(getClass().getClassLoader().getResource("script.js").getPath());
        assertNull(JAVASCRIPT.invokeFunction("f1"));
        assertEquals(JAVASCRIPT.invokeFunction("f2"), "F2");
        assertEquals(JAVASCRIPT.invokeFunction("f3", "Abhijeet"), "F3-Abhijeet");
        Map<String, Object>  obj1 = (Map<String, Object>) JAVASCRIPT.invokeFunction("f4", getMap("a=a1,b=b1"));
        assertEquals(obj1.get("x"), "a1_x");
        assertEquals(obj1.get("y"), "b1_y");
    }

    @Test
    public void testGroovyViaFile(){
        GROOVY.loadScriptFromFile(getClass().getClassLoader().getResource("script.groovy").getPath());
        assertNull(GROOVY.invokeFunction("f1"));
        assertEquals(GROOVY.invokeFunction("f2"), "F2");
        assertEquals(GROOVY.invokeFunction("f3", "Abhijeet"), "F3-Abhijeet");
        Map<String, Object>  obj1 = (Map<String, Object>) GROOVY.invokeFunction("f4", getMap("a=a1,b=b1"));
        assertEquals(obj1.get("x"), "a1_x");
        assertEquals(obj1.get("y"), "b1_y");
    }

    @Test
    public void testJavaScriptViaScriptAsString() throws IOException {
        JAVASCRIPT.loadScript(FileUtils.readFileAsString(getClass().getClassLoader().getResource("script.js").getPath()));
        assertNull(JAVASCRIPT.invokeFunction("f1"));
        assertEquals(JAVASCRIPT.invokeFunction("f2"), "F2");
        assertEquals(JAVASCRIPT.invokeFunction("f3", "Abhijeet"), "F3-Abhijeet");
        Map<String, Object>  obj1 = (Map<String, Object>) JAVASCRIPT.invokeFunction("f4", getMap("a=a1,b=b1"));
        assertEquals(obj1.get("x"), "a1_x");
        assertEquals(obj1.get("y"), "b1_y");
    }

    @Test
    public void testEngineVal(){
        JAVASCRIPT.loadScriptFromFile(getClass().getClassLoader().getResource("script.js").getPath());
        JAVASCRIPT.getScriptEngine().put("a1", A.builder().id("1").name("One").build());
        assertNull(JAVASCRIPT.invokeFunction("printA"));
    }

    @Test
    public void testGroovy(){
        //ScriptEngines.GROOVY.getScriptEngine().put("a", "a1");
        //System.out.println(ScriptEngines.GROOVY.getScriptEngine().get("a"));
        //System.out.println(JAVASCRIPT.getScriptEngine().get("a"));
        ScriptEngines.values();
        //ScriptEngines.GROOVY.loadScriptFromFile().put("b", "b1");
        System.out.println(ScriptEngines.GROOVY.getScriptEngine().get("b"));

    }

    @Getter @Builder @ToString
    private static class A{
        private String name;
        private String id;
    }
}
