package lazydevs.springhelpers.dynabeans;

import lazydevs.mapper.utils.engine.ScriptEngines;
import lazydevs.mapper.utils.reflection.InitDTO;
import lazydevs.mapper.utils.reflection.InitDTO.ArgDTO;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Abhijeet Rai
 */
@ConfigurationProperties(prefix = "dyna-beans")
@Getter @Setter @ToString
public class DynaBeansConfig  {
    private Map<ScriptEngines, List<String>> scriptPathsByEngine = new HashMap<>();
    private LinkedHashMap<String, InitInstruction> init = new LinkedHashMap<>();


    @Getter @Setter @ToString
    public static class InitInstruction extends ArgDTO{
        private InitDTO initDTO;
        private ScriptInstruction script;
        private Map<String, Object> val;
        private Class<?> type;
    }

    @Getter @Setter @ToString
    public static class ScriptInstruction{
        private ScriptEngines engine = ScriptEngines.JAVASCRIPT;
        private String functionName;
        private List<ArgDTO> args = new ArrayList<>();
    }


}
