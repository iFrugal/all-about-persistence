package lazdevs.peristence.mongo.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 * @author Abhijeet Rai
 */
@Getter @Setter @ToString
@NoArgsConstructor
public class Distinct extends Filterable {
    private String fieldName;
    private Class<?> resultClass;
    private String resultClassFqcn;

    public Distinct(String fieldName, Map<String, Object> filter, Class<?> resultClass){
        super(filter);
        this.fieldName = fieldName;
        this.resultClass = resultClass;
    }
}

