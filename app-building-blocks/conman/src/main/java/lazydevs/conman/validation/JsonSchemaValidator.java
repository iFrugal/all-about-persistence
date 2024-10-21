package lazydevs.conman.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;
import lazydevs.mapper.utils.SerDe;
import lazydevs.services.basic.exception.ServerException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonSchemaValidator {


    public static JsonNode valid(JsonNode jsonNode, JsonSchema schema) throws IOException {
        Set<ValidationMessage> errors = schema.validate(jsonNode);
        List<String> listoferrors =
                errors.stream().map(msg -> msg.toString() + " \n").collect(Collectors.toList());
        if (!errors.isEmpty()) {
            throw new ServerException(listoferrors.toString());
        }
        return jsonNode;
    }

    public static JsonNode convert(String jsonStr) {
        try {
            return SerDe.JSON.getOBJECT_MAPPER().readTree(jsonStr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode convert(InputStream jsonStrInputStream) {
        try {
            return SerDe.JSON.getOBJECT_MAPPER().readTree(jsonStrInputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
