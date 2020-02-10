package ifrugal.persistence.utils.engine;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.testng.Assert.*;
import org.testng.annotations.Test;


import java.io.IOException;

import static ifrugal.persistence.utils.engine.ScriptEngines.JAVASCRIPT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class ScriptEnginesTest {

    @Test
    public void testLoadScriptFile() {
    }

    @Test
    public void testTestLoadScriptFile() {
    }

    @Test
    public void testLoadScript() {
    }

    @Test
    public void testCall() {
    }

    @Test
    public void testJavaScriptViaFile(){
        JAVASCRIPT.loadScriptFile(getClass().getClassLoader().getResource("script.js").getPath());
        assertNull(JAVASCRIPT.call("f1"));
        assertEquals(JAVASCRIPT.call("f2"), "F2");
        assertEquals(JAVASCRIPT.call("f3", "Abhijeet"), "F3-Abhijeet");
    }

    @Test
    public void testJavaScriptViaScriptAsString() throws IOException {
        String fileContent = FileUtils.readFileToString(new File(getClass().getClassLoader().getResource("script.js").getPath()));
        JAVASCRIPT.reset();
        JAVASCRIPT.loadScript(fileContent);
        assertNull(JAVASCRIPT.call("f1"));
        assertEquals(JAVASCRIPT.call("f2"), "F2");
        assertEquals(JAVASCRIPT.call("f3", "Abhijeet"), "F3-Abhijeet");
    }
}