package lazydevs.persistence.reader;

import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.engine.TemplateEngine;
import lazydevs.mapper.utils.reflection.Init;
import lazydevs.mapper.utils.reflection.InitDTO;
import lazydevs.mapper.utils.reflection.ReflectionUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static lazydevs.mapper.utils.engine.ScriptEngines.JAVASCRIPT;

/**
 * @author Abhijeet Rai
 */
@Getter @Setter @ToString
public class GeneralTransformer {
    private String template;
    private String transformerFqcn;
    private String jsFunctionName;
    private boolean transformAllAtOnce;
    private boolean transformAndMergeToOriginal;

    public List<Map<String, Object>> convert(List<Map<String, Object>> list) {
        if (transformAllAtOnce) {
            if (null != template) {
                Map<String, Object> map = new HashMap<>();
                map.put("list", list);
                return SerDe.JSON.deserializeToListOfMap(TemplateEngine.getInstance().generate(getTemplate(), map));
            } else if (null != getJsFunctionName()) {
                return (List<Map<String, Object>>) JAVASCRIPT.invokeFunction(getJsFunctionName(), list);
            } else
                return list;
        }
        return list.stream().map(row -> convert(row)).collect(Collectors.toList());
    }

    public Map<String, Object> convert(Map<String, Object> row)
    {
        if (isTransformAndMergeToOriginal()) {
            String originalAsStr = SerDe.JSON.serialize(row);
            Map<String, Object> transformed = convertLocal(row);
            Map<String, Object> original = SerDe.JSON.deserializeToMap(originalAsStr);
            original.putAll(transformed);
            return original;
        }
            return convertLocal(row);
    }

    private Map<String, Object> convertLocal(Map<String, Object> row) {

        if (null != template) {
            return SerDe.JSON.deserializeToMap(TemplateEngine.getInstance().generate(getTemplate(), row));
        } else if (null != getJsFunctionName()) {
            return (Map<String, Object>) JAVASCRIPT.invokeFunction(getJsFunctionName(), row);
        }else if(transformerFqcn != null){
            GenericTransformer genericTransformer = ReflectionUtils.getInterfaceReference(Init.builder().fqcn(transformerFqcn).build(), GenericTransformer.class);
            genericTransformer.transform(row);
        }
        return row;
    }

}
