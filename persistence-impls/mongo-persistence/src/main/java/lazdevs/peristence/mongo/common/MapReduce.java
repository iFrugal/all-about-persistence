package lazdevs.peristence.mongo.common;

import lombok.*;

/**
 * @author Abhijeet Rai
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MapReduce{
    private String mapFunction;
    private String reduceFunction;
}