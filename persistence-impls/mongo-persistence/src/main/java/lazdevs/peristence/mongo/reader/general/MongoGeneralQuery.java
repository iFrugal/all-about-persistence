package lazdevs.peristence.mongo.reader.general;

import lazdevs.peristence.mongo.common.MongoQuery;
import lombok.*;

/**
 * @author Abhijeet Rai
 */
@Getter @Setter @ToString
@AllArgsConstructor
@NoArgsConstructor
public class MongoGeneralQuery {
    private String collection;
    private MongoQuery query = new MongoQuery();
}
