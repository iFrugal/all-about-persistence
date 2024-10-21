package lazydevs.scheduleit.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Abhijeet Rai
 */
@Getter
@Setter
@ToString @NoArgsConstructor
public class LockAttributes {
    private String lockName;
    private int lockAtLeastFor = 300;
    private int lockAtMostFor = 600;

    public LockAttributes(String lockName) {
        this.lockName = lockName;
    }
}
