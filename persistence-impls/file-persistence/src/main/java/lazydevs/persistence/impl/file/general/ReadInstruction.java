package lazydevs.persistence.impl.file.general;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Abhijeet Rai
 */
@Getter @Setter @ToString
public class ReadInstruction {
    private String filePath;
    private int noOfLinesToIgnore;
}
