package ifrugal.persistence.utils.engine;

import org.apache.commons.io.FileUtils;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.nio.charset.Charset;
import java.nio.file.Paths;

public enum ScriptEngines {

    JAVASCRIPT("javascript"),
    GROOVY("groovy");

    private ScriptEngine scriptEngine;
    private final String name;

    ScriptEngines(String name){
        this.name = name;
        this.scriptEngine = new ScriptEngineManager().getEngineByName(name);
    }

    public void reset(){
        this.scriptEngine = new ScriptEngineManager().getEngineByName(this.name);
    }

    /**
     * Executes the specified script.  The default <code>ScriptContext</code> for the <code>ScriptEngine</code>
     * is used
     *
     * @param absoluteFilePath from where file is to be loaded
     * @param charset used as encoding of file
     */
    public void loadScriptFile(String absoluteFilePath, Charset charset){
        try {
            loadScript(FileUtils.readFileToString(Paths.get(absoluteFilePath).toFile(), charset));
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes the specified script.  The default <code>ScriptContext</code> for the <code>ScriptEngine</code>
     * is used, uses Charset.defaultCharset() for encoding
     *
     * @param absoluteFilePath from where file is to be loaded
     */
    public void loadScriptFile(String absoluteFilePath){
        loadScriptFile(absoluteFilePath, Charset.defaultCharset());
    }

    /**
     * Executes the specified script.  The default <code>ScriptContext</code> for the <code>ScriptEngine</code>
     * is used.
     *
     * @param scriptAsText The script language source to be executed.
     */
    public void loadScript(String scriptAsText){
        try{
            this.scriptEngine.eval(scriptAsText);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Used to call top-level procedures and functions defined in scripts.
     *
     * @param functionName of the procedure or function to call
     * @param args Arguments to pass to the procedure or function
     * @return The value returned by the procedure or function
     */
    public Object call(String functionName, Object... args){
        Invocable invocable = (Invocable) this.scriptEngine;
        try{
            return invocable.invokeFunction(functionName, args);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
