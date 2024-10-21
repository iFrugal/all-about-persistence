package lazydevs.persistence.writer.general;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

import static lazydevs.mapper.utils.SerDe.JSON;
import static lazydevs.mapper.utils.engine.TemplateEngine.getInstance;

/**
 * @author Abhijeet Rai
 */
@Getter @Setter @ToString
public class TemplatisedWriteInstruction {
    private boolean template;

    public static <T extends TemplatisedWriteInstruction> T process(Map<String, Object> map, T writeInstruction){
        if(null !=  writeInstruction && writeInstruction.isTemplate()){
            return JSON.deserialize(getInstance().generate(JSON.serialize(writeInstruction), map), (Class<T>)writeInstruction.getClass());
        }
        return writeInstruction;
    }

}
