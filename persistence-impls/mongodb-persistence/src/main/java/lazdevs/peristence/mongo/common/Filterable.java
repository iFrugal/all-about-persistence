package lazdevs.peristence.mongo.common;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Abhijeet Rai
 */
@Getter @Setter @ToString @NoArgsConstructor @AllArgsConstructor
public class Filterable {
    private Map<String, Object> filter = new HashMap<>();
}
