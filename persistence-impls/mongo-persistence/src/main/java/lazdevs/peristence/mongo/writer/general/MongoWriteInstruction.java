package lazdevs.peristence.mongo.writer.general;

import lazydevs.persistence.writer.general.TemplatisedWriteInstruction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * @author Abhijeet Rai
 */
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class MongoWriteInstruction extends TemplatisedWriteInstruction {
    private String collectionName;
    private Map<String, Object> document;

}
