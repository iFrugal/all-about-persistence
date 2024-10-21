package lazydevs.persistence.impl.file.general;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 * @author Abhijeet Rai
 */
@Getter @Setter @ToString
public class WriteInstruction {
    private String filePath;
    private Map<Integer, String> columnIndexMap;
    private String[] headers;
}
