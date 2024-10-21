package lazydevs.readeasy.config;

import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.reflection.InitDTO;
import lazydevs.persistence.reader.GeneralTransformer;
import lazydevs.services.basic.validation.Param;
import lazydevs.springhelpers.dynabeans.DynaBeansConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Abhijeet Rai
 */


@Getter @Setter @ToString
@Configuration @ConfigurationProperties(prefix = "readeasy")
public class ReadEasyConfig{
    private InitDTO generalReaderInit;
    Map<String, InitDTO> generalReaders;
    private InitDTO requestContextSupplierInit;
    private InitDTO globalContextSupplierInit;
    private Map<String, List<String>> queryFiles = new LinkedHashMap<>();
    private Map<Operation, Map<String, Object>> operationInstruction = new HashMap<>();


    @Getter @Setter
    public static class QueryWithDynaBeans{
        private DynaBeansConfig dynaBeans;
        private Map<String, Query> queries;
    }

    @Getter @Setter @ToString
    public static class Query {
        private String readerId = "default";
        private SerDe rawFormat = SerDe.JSON;
        private String raw;
        private Map<String, Param> params = new HashMap<>();
        private CacheFetchInstruction cacheFetchInstruction;
        private GeneralTransformer rowTransformer;
        private Map<Operation, Map<String, Object>> operationInstruction = new HashMap<>();

    }

    public enum Operation{
        ONE, LIST, PAGE, COUNT, EXPORT
    }

    @Getter @Setter @ToString
    public static class CacheFetchInstruction{
        private String jsFunctionName;
        private List<String> args = new ArrayList<>();
    }

}


