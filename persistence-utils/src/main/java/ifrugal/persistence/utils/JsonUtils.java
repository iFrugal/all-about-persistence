package ifrugal.persistence.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;

public class JsonUtils {
    private static ObjectMapper OBJECT_MAPPER;

    public static void setObjectMapper(@NonNull ObjectMapper objectMapper){
        OBJECT_MAPPER = objectMapper;
        OBJECT_MAPPER.configure(FAIL_ON_EMPTY_BEANS, false);
    }

    static {
        OBJECT_MAPPER = new ObjectMapper();
    }

    public static <T> T fromJson(String jsonString, Class<T> clazz){
        try{
            return OBJECT_MAPPER.readValue(jsonString, clazz);
        }catch (Exception e){
            throw new RuntimeException(String.format("Exception while de-serializing from jsonString = '%s', to type = '%s'", jsonString, clazz), e);
        }
    }

    public static <T> T fromJson(InputStream inputStream, Class<T> clazz){
        try{
            return OBJECT_MAPPER.readValue(inputStream, clazz);
        }catch (Exception e){
            throw new RuntimeException(String.format("Exception while de-serializing from inputStream to type = '%s'", clazz), e);
        }
    }

    public static <T> T fromJsonFile(String jsonFilePath, Class<T> clazz){
        try{
            return OBJECT_MAPPER.readValue(Files.readAllBytes(Paths.get(jsonFilePath)), clazz);
        }catch (Exception e){
            throw new RuntimeException(String.format("Exception while de-serializing from jsonFile = '%s', to type = '%s'", jsonFilePath, clazz), e);
        }
    }

    public static <T> List<T> fromJsonToList(String jsonString, Class<T> clazz){
        try{
            return OBJECT_MAPPER.readValue(jsonString, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        }catch (Exception e){
            throw new RuntimeException(String.format("Exception while de-serializing from jsonString = '%s', to List<T> of type = '%s'", jsonString, clazz), e);
        }
    }

    public static Map<String, Object> fromJson(String jsonString){
        return fromJson(jsonString, Map.class);
    }

    public static Map<String, Object> toMap(Object object){
        return OBJECT_MAPPER.convertValue(object, Map.class);
    }

    public static String toJson(Object object){
        return toJson(object, false);
    }

    public static String toJson(Object object, boolean pretty){
        try {
            if (pretty) {
                return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
            }
            return OBJECT_MAPPER.writeValueAsString(object);
        }catch (Exception e){
            throw new RuntimeException(String.format("Exception while serializing object to json. object = '%s'", object), e);
        }
    }

}
