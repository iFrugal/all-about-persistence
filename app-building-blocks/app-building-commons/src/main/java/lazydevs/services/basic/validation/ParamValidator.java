package lazydevs.services.basic.validation;

import lazydevs.services.basic.exception.ValidationException;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static lazydevs.mapper.utils.engine.ScriptEngines.JAVASCRIPT;
import static lazydevs.mapper.utils.reflection.ClassUtils.loadClass;
import static org.springframework.util.StringUtils.hasText;

@Component
public class ParamValidator {
    private final Map<String, Pattern> patternMap = new HashMap<>();

    public void validate(String queryId, Map<String, Param> configuredParams, @NonNull Map<String, Object> actualParams) {
        validate(true,null, queryId, configuredParams, actualParams);
    }

    public void validate(boolean onlyRegisteredParamsAllowed, String type, String queryId, Map<String, Param> configuredParams, @NonNull Map<String, Object> actualParams) {
        if(null == type){
            type = "";
        }
        Map<String, Object> duplicate = new HashMap<>(actualParams);
        String finalType = type;
        configuredParams.entrySet().stream().forEach(p -> {
            Param validator = p.getValue();
            Object val = duplicate.remove(p.getKey());
            if (val == null){
                if(validator.isRequired()){
                    throw new ValidationException(format("%s param '%s' is mandatory, but it is null", finalType, p.getKey()));
                }else{
                    if(null != validator.getDefaultValue()){
                        actualParams.put(p.getKey(), validator.getDefaultValue());
                        val = validator.getDefaultValue();
                    }else{
                        return;
                    }
                }
            }
            val = validateUsingTypeFqcn(finalType, p, validator, val);
            validateUsingRegexValidator(queryId, finalType, p, validator, val);
            validateUsingJsFunction(finalType, p, validator, val);
        });
        if(onlyRegisteredParamsAllowed && duplicate.size() > 0){
            throw new ValidationException(format("Some unregistered %s params are passed. They are %s", finalType, duplicate));
        }
    }

    private void validateUsingJsFunction(String finalType, Map.Entry<String, Param> p, Param validator, Object val) {
        if(hasText(validator.getJsFunctionName())){
            Boolean isValid = (Boolean) JAVASCRIPT.invokeFunction(validator.getJsFunctionName(), val);
            if(!isValid){
                throw new ValidationException(format("JS Validation for %s param = '%s' failed. Val='%s', val.class='%s'", finalType, p.getKey(), val, val.getClass()));
            }
        }
    }

    private void validateUsingRegexValidator(String queryId, String finalType, Map.Entry<String, Param> p, Param validator, Object val) {
        if(hasText(validator.getRegexValidator())){
            if(!(val instanceof String)){
                throw new ValidationException(format("Regex Validation for %s param = '%s' is set, but the value is not a String. Val='%s', val.class='%s'", finalType, p.getKey(), val, val.getClass()));
            }
            Pattern pattern = patternMap.computeIfAbsent(queryId + "$$" + p.getKey(), key -> Pattern.compile(validator.getRegexValidator()));
            if(!pattern.matcher((String) val).matches()){
                throw new ValidationException(format("Regex Validation for %s param = '%s' failed. Regex = '%s', Val='%s', val.class='%s'", finalType, p.getKey(), validator.getRegexValidator(), val, val.getClass()));
            }
        }
    }

    private Object validateUsingTypeFqcn(String finalType, Map.Entry<String, Param> p, Param validator, Object val) {
        if(hasText(validator.getTypeFqcn())){
            if (!loadClass(validator.getTypeFqcn()).isAssignableFrom(val.getClass())) {
                if(finalType.equals("Query")
                        && String.class.equals(loadClass(validator.getTypeFqcn())) //param registered as String
                        && val.getClass().equals(loadClass("java.lang.String[]"))) //but val is String[] as queryParams can be comma seperated too
                {
                    val = Arrays.stream((String[]) val).collect(Collectors.joining(","));
                }else {
                    throw new ValidationException(format("The val for %s param = '%s' is not a instance of typeFqcn('%s') defined in the validator. Val = %s, val.class=%s", finalType, p.getKey(), validator.getTypeFqcn(), val, val.getClass()));
                }
            }
        }
        return val;
    }


}
