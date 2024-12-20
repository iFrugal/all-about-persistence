package lazydevs.persistence.jdbc.general;

import lazydevs.mapper.db.jdbc.JDBCParam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author Abhijeet Rai
 */
@Getter @NoArgsConstructor @AllArgsConstructor
public class JdbcParam extends JDBCParam {
    private boolean nameTemplate;

    public JdbcParam(String name, Type type, Object value) {
        super(name, type, value);
    }

    public JdbcParam(Object value){
        super(Type.get(value.getClass()), value);
    }

    public JdbcParam(boolean nameTemplate, String name, Type type, Object value) {
        super(name, type, value);
        this.nameTemplate = nameTemplate;
    }
    public JdbcParam(String name, Object value) {
        super(name, value);
    }
}
