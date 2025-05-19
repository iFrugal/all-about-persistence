package lazydevs.persistence.reader;

import java.util.Map;

public interface GenericTransformer {
     Map<String, Object> transform(Map<String, Object> map);
}
