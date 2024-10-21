package lazydevs.mapper.utils.engine;


import lazydevs.mapper.utils.file.FileUtils;
import lazydevs.mapper.utils.reflection.InitDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

//TODO: Create an enum to support Jav
public enum ScriptEngines {

    JAVASCRIPT("javascript"),
    GROOVY("groovy"),
    PYTHON("python")
    ;

    private static final Logger log = LoggerFactory.getLogger(ScriptEngines.class );

    private static final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    @Getter
    private final ScriptEngine scriptEngine;
    private final String name;

    static{
        new ScriptEngineManager().getEngineFactories().forEach(f-> {
            log.info("Engine = {}({}), Language = {}({}), names = {}", f.getEngineName(), f.getEngineVersion(), f.getLanguageName(),f.getLanguageVersion(), f.getNames());
        });
    }
    ScriptEngines(String name){
        this.name = name;
        scriptEngine = new ScriptEngineManager().getEngineByName(name);
        validate(false);
    }

    private void validate(boolean throwException){
        if(scriptEngine == null){
            if(throwException) {
                throw new IllegalStateException("scriptEngine not found with name = " + this.name);
            }else{
                System.err.println("scriptEngine not found with name = " + this.name);
            }
        }
    }

    public void loadScriptFromFile(String filePath){
        loadScript(FileUtils.readFileAsString(filePath));
    }

    public void loadScriptFromFile(InputStream inputStream){
        loadScript(FileUtils.readInputStreamAsString(inputStream));
    }

    public void loadScript(String scriptAsText){
        validate(true);
        try {
            this.scriptEngine.eval(scriptAsText);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object invokeFunction(String expression, Object... args) {
        validate(true);
        Invocable invocable = (Invocable) this.scriptEngine;
        try {
            return invocable.invokeFunction(expression, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
