package lazydevs.transporter.config;

import lazydevs.mapper.utils.reflection.InitDTO;
import lazydevs.scheduleit.pojo.CronAttributes;
import lazydevs.scheduleit.pojo.LockAttributes;
import lazydevs.springhelpers.dynabeans.DynaBeansConfig;
import lazydevs.springhelpers.dynabeans.DynaBeansConfig.ScriptInstruction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Abhijeet Rai
 */
@Getter @Setter @ToString
public class Pipeline {
    private List<Schedule> schedules = new ArrayList<>();
    private DynaBeansConfig dynaBeans;
    private List<Flow> flows = new ArrayList<>();
    private Instruction pre;
    private Instruction post;
    private boolean scheduleItEnabled = true;
    private ScriptInstruction paramsListProviderScript;



    @Getter @Setter @NoArgsConstructor
    public static class Schedule{
        private List<Map<String, Object>> paramList = new ArrayList<>();
        private String scheduleName;
        private CronAttributes cronAttributes;
        private LockAttributes lockAttributes;
   }

    @Getter @Setter @NoArgsConstructor
    public static class Instruction{
        private ScriptInstruction scriptInstruction;
        private InitDTO runnableInit;
        private String inlineJavascript;
    }

}
