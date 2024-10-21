package lazydevs.scheduleit.pojo;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.concurrent.ScheduledFuture;

/**
 * @author Abhijeet Rai
 */
@Getter @Setter @ToString
public class ScheduledInstruction {
    private String scheduleName;
    private ScheduledFuture<?> scheduledFuture;
    private CronAttributes cronAttributes;
    private LockAttributes lockAttributes;
    private ScheduledTarget target;
}
