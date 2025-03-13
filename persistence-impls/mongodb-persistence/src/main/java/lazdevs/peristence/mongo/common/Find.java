package lazdevs.peristence.mongo.common;

import lazydevs.mapper.utils.SerDe;
import lombok.*;

import java.util.Map;

/**
 * @author Abhijeet Rai
 */
@Getter @Setter @ToString
@NoArgsConstructor @AllArgsConstructor
public class Find extends Filterable {
    private Map<String, Object> projection;
    private Map<String, Object> sort;

    public Find(String filter, String projection, String sort){
        super(SerDe.JSON.deserializeToMap(filter));
        this.projection = SerDe.JSON.deserializeToMap(projection);
        this.sort = SerDe.JSON.deserializeToMap(sort);
    }

    public static FindBuilder builder() {
        return new FindBuilder();
    }

    public static class FindBuilder {
        private Map<String, Object> filter;
        private Map<String, Object> projection;
        private Map<String, Object> sort;

        FindBuilder() {
        }

        public FindBuilder filter(Map<String, Object> filter) {
            this.filter = filter; return this;
        }

        public FindBuilder projection(Map<String, Object> projection) {
            this.projection = projection; return this;
        }

        public FindBuilder sort(Map<String, Object> sort) {
            this.sort = sort; return this;
        }

        public FindBuilder filter(String filterJsonString) {
            this.filter = SerDe.JSON.deserializeToMap(filterJsonString); return this;
        }

        public FindBuilder projection(String projectionJsonString) {
            this.projection = SerDe.JSON.deserializeToMap(projectionJsonString); return this;
        }

        public FindBuilder sort(String sortJsonString) {
            this.sort = SerDe.JSON.deserializeToMap(sortJsonString); return this;
        }

        public Find build() {
            Find find = new Find(projection, sort);
            find.setFilter(filter);
            return find;
        }
    }
}
