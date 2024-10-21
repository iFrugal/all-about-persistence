package lazydevs.scheduleit.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Abhijeet Rai
 */
@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor
public class ScheduledInstructionDTO {
    private String scheduleName;
    private CronAttributes cronAttributes;
    private LockAttributes lockAttributes;
    private ScheduledTarget scheduledTarget;

    public ScheduledInstructionDTO(ScheduledInstruction instruction){
        this.scheduleName = instruction.getScheduleName();
        this.cronAttributes = instruction.getCronAttributes();
        this.lockAttributes = instruction.getLockAttributes();
        this.scheduledTarget = instruction.getTarget();
    }


}
