package lazydevs.transporter.config;

import lazydevs.mapper.utils.SerDe;
import lazydevs.persistence.reader.GeneralTransformer;
import lazydevs.transporter.enums.Actions;
import lazydevs.transporter.enums.Modes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * @author Abhijeet Rai
 */
@Getter @Setter @ToString @NoArgsConstructor
public class Flow {
    private Reader reader;
    private List<Writer> writers;
    private String id = "";
    private String desc = "";
    private PipelineTriggerInstruction pipelineTriggerInstruction;

    @Getter @Setter
    public  static class PipelineTriggerInstruction {
        private String pipelineKey;
        private List<Map<String, Object>> params;
    }

    @Getter @Setter
    public  static class Reader
    {
        private String beanName;
        private GeneralTransformer transformer;
        private Modes mode = Modes.BATCHED;
        private int batchSize = 5_000;
        private boolean transformAndMergeToOriginal = false;
        private String readInstruction;
        private SerDe instructionSerDe = SerDe.JSON;
        private String writeToVariableName;
        private String enrichmentOrFilterFunction;
        private String id = "";
        private String desc = "";
    }
    @Getter @Setter
    public  static class Writer
    {
        private String beanName;
        private Actions action = Actions.CREATE;
        private  String writeInstruction;
        private SerDe instructionSerDe = SerDe.JSON;
        private boolean enabled = true;
        private Writer onSuccess;
        private Writer onFailure;
        private String id = "";
        private String desc = "";

    }

}