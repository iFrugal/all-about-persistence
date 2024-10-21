package lazydevs.services.crud.transform;

import com.fasterxml.jackson.databind.JsonNode;
import lazydevs.services.crud.transform.DefaultTransCrudController.JsonPatchOperation;
import lombok.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static lazydevs.mapper.utils.SerDe.JSON;
import static org.junit.jupiter.api.Assertions.*;

class DefaultTransCrudControllerTest<T> {

    @SneakyThrows
    @Test
    <T> void patch() {
        JsonNode jsonNode = JSON.getOBJECT_MAPPER().convertValue(Arrays.asList(jsonPatchOperation()), JsonNode.class);
        com.github.fge.jsonpatch.JsonPatch patch =  com.github.fge.jsonpatch.JsonPatch.fromJson(jsonNode);
        ABC neww = applyPatch(patch, new ABC("a1", "b1","c1"));
        System.out.println(neww);
    }

    private JsonPatchOperation jsonPatchOperation(){
        JsonPatchOperation operation = new JsonPatchOperation();
        operation.setOp("replace");
        operation.setPath("/a");
        operation.setValue("a2");
        return operation;
    }


    private  ABC applyPatch(com.github.fge.jsonpatch.JsonPatch patch, ABC old) {
        try {
            JsonNode patched  = patch.apply(JSON.getOBJECT_MAPPER().convertValue(old, JsonNode.class));
            return JSON.getOBJECT_MAPPER().treeToValue(patched, ABC.class);
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
    }


    @Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor
    private static final class ABC{
       private String a, b, c;
    }
}