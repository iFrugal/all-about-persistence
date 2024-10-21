package lazydevs.persistence.reader;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;


/**
 * @author Abhijeet Rai
 */

@ToString @Getter @Setter
public class Param<T> {
    private String name;
    private T value;

    public Param(){}

    public Param(@NonNull String name, T value) {
        this.name = name;
        this.value = value;
    }

    public static <P> Map<String, P> convert(Param<P>... params){
        Map<String, P> map = new LinkedHashMap<>();
        if(null != params && params.length != 0){
            map = stream(params).collect(toMap(Param::getName, Param::getValue));
        }
        return map;
    }

}
