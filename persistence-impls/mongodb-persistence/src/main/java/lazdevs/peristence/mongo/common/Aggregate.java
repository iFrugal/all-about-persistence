package lazdevs.peristence.mongo.common;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * @author Abhijeet Rai
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Aggregate {
    private List<Map<String, Object>> pipeline;
}
