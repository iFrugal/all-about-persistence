package lazydevs.scheduleit.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Abhijeet Rai
 */
@Getter
@Setter @ToString @NoArgsConstructor
public class CronAttributes {
    private String cronExpression;
    private String timeZone;

    public CronAttributes(String cronExpression) {
        this.cronExpression = cronExpression;
    }
}
