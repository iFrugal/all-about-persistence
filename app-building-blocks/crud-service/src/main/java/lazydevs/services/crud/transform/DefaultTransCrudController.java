package lazydevs.services.crud.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatch;
import lazydevs.services.basic.exception.RESTException;
import lazydevs.services.basic.exception.ValidationException;
import lazydevs.services.basic.filter.RequestContext;
import lazydevs.services.crud.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;

import static java.lang.String.format;
import static lazydevs.mapper.utils.SerDe.JSON;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.status;
import static org.springframework.web.bind.annotation.RequestMethod.*;


public abstract class DefaultTransCrudController<T extends BaseEntity, DTO> implements TransformableCrudController<T, DTO> {


    @GetMapping(value = "/{id}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<DTO> findById(@PathVariable("id") String id, @RequestParam(value = "projection", required = false) Set<String> projection) {
        DTO dto = convertToDTO(findOneEntity(id, projection));
        return status(null == dto ? NOT_FOUND : OK).body(dto);
    }

    protected abstract T findOneEntity(String id, Set<String> projection);
    protected abstract T createEntity(T neww);
    protected abstract T replaceEntity(T neww);
    protected abstract T deleteEntity(T neww);
    protected abstract Class<T> getEntityType();

    @Override @RequestMapping(method = RequestMethod.POST, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<DTO> post(@RequestBody @Validated DTO dto) {
        validate(dto);
        prePersist(dto, RequestMethod.POST);
        T neww = convertToEntity(dto);
        ensureItDoesNotExists(neww);
        T t = createEntity(neww);
        postPersist(t, RequestMethod.POST);
        return status(CREATED).body(convertToDTO(t));
    }



    @Override @RequestMapping(method = PUT, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<DTO> put(@RequestBody @Validated DTO dto) {
        validate(dto);
        prePersist(dto, PUT);
        T neww = convertToEntity(dto);
        T old = ensureItExists(neww);
        return update(neww, old, PUT);
    }

    @SneakyThrows
    @PatchMapping(path = "/{id}", consumes = "application/json-patch+json", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<DTO> patch(@PathVariable("id") String id, @RequestBody List<JsonPatchOperation> jsonPatch) {
        T old = ensureItExists(id);
        validateJsonPatch(old, jsonPatch);
        JsonPatch patch =  JsonPatch.fromJson(JSON.getOBJECT_MAPPER().convertValue(jsonPatch, JsonNode.class));
        T neww = applyPatch(patch, old);
        DTO dto = convertToDTO(neww);
        prePersist(dto, PATCH);
        neww = convertToEntity(dto);
        return update(neww, old, PATCH);
    }

    protected abstract Map<String, Set<String>> getAllowedOperationVsPath();
    protected void validateJsonPatch(T old, List<JsonPatchOperation> jsonPatch) {
        Map<String, Set<String>> allowedOperationVsPath = this.getAllowedOperationVsPath();

        jsonPatch.forEach((operation) -> {
            String op = operation.getOp();
            String path = operation.getPath();

            if (!allowedOperationVsPath.containsKey(op)) {
                throw new ValidationException(String.format("Operation not allowed. op = '%s'", op));
            }

            if (!path.startsWith("/")) {
                throw new ValidationException(String.format("Invalid path = '%s'. Must start with '/'. Read: https://datatracker.ietf.org/doc/html/rfc6902/ , https://jsonpatch.com/", path));
            }

            boolean matched = allowedOperationVsPath.get(op).stream()
                    .anyMatch(allowedPath -> matchJsonPathPattern(allowedPath, path));

            if (!matched) {
                throw new ValidationException(String.format("Operation op = '%s' is not allowed on path = '%s'", op, path));
            }

            // "add" specific rule: reject if field already exists
            if ("add".equalsIgnoreCase(op)) {
                String fieldName = extractRootFieldName(path);
                try {
                    Field field = old.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object oldValue = field.get(old);
                    if (oldValue != null) {
                        throw new ValidationException(String.format("Cannot add value to field '%s' as it already exists.", fieldName));
                    }
                } catch (NoSuchFieldException e) {
                    throw new ValidationException(String.format("Invalid field '%s' in JSON Patch request.", fieldName));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(String.format("Error accessing field '%s'. Ensure proper permissions.", fieldName), e);
                }
            }
        });
    }

    private boolean matchJsonPathPattern(String pattern, String actualPath) {
        // Replace '*' with regex digit matcher (e.g. /files/\d+)
        String regex = pattern
                .replace("/", "\\/")
                .replace("*", "\\d+")
                .replace("-", "\\-");  // for add operations
        return actualPath.matches(regex);
    }

    private String extractRootFieldName(String path) {
        // E.g. "/packages/0/name" => "packages"
        String[] tokens = path.split("/");
        return tokens.length > 1 ? tokens[1] : "";
    }


    @Getter @Setter @ToString
    public static class JsonPatchOperation {
        private String op;
        private String path;
        private Object value;
    }


    private ResponseEntity<DTO> update(T neww, T old, RequestMethod requestMethod) {
        T t = replaceEntity(neww);
        postPersist(t, requestMethod);
        return status(OK).body(convertToDTO(t));
    }

    private T applyPatch(JsonPatch patch, T old) {
        try {
            JsonNode patched  = patch.apply(JSON.getOBJECT_MAPPER().convertValue(old, JsonNode.class));
            return JSON.getOBJECT_MAPPER().treeToValue(patched, getEntityType());
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
    }

    @Override @RequestMapping(method = DELETE, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<DTO> delete(@RequestBody @Validated DTO dto) {
        validate(dto);
        preDelete(dto);
        T neww = convertToEntity(dto);
        T old = ensureItExists(neww);
        T t = deleteEntity(neww);
        postDelete(t);
        return status(OK).body(convertToDTO(t));
    }

    private T ensureItExists(T t){
        return ensureItExists(t.getId());
    }

    private T ensureItExists(String id){
        T old = findOneEntity(id, new HashSet<>());
        if(null == old){
            throw new RESTException("Entity Not Found with _id = "+id, NOT_FOUND.value());
        }
        RequestContext.current().set("old", old);
        return old;
    }

    private void ensureItDoesNotExists(T t){
        if (!ObjectUtils.isEmpty(t.getId())) {
            T old = findOneEntity(t.getId(), new HashSet<>());
            if(null != old){
                throw new RESTException("Entity Already Present Found with _id = " + t.getId(), CONFLICT.value());
            }
        }
    }
}
