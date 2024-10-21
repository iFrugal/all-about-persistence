package lazydevs.mapper.utils;

/**
 * @author Abhijeet Rai
 */


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

public enum SerDe {
    JSON(new MappingJsonFactory()), YAML(new YAMLFactory()), XML(new XmlMapper());

    SerDe(JsonFactory jsonFactory){
        OBJECT_MAPPER = new ObjectMapper(jsonFactory);
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    SerDe(ObjectMapper objectMapper){
        OBJECT_MAPPER = objectMapper;
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @Getter
    private final ObjectMapper OBJECT_MAPPER ;


    public <T> T deserialize(String string, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(string, clazz);
        } catch (Exception e) {
            throw new RuntimeException(format("Exception while deserializing from string = '%s' to type = '%s'", string, clazz), e);
        }
    }

    public <T>  T deserialize(File file, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(file, clazz);
        } catch (Exception e) {
            throw new RuntimeException(format("Exception while deserializing from file = '%s' to type = '%s'", file, clazz), e);
        }
    }

    public <T> T deserialize(InputStream is, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(is, clazz);
        } catch (Exception e) {
            throw new RuntimeException(format("Exception while deserializing from inputStream to type = '%s'", clazz), e);
        }
    }

    public Map<String, Object> deserializeToMap(InputStream is) {
        return deserialize(is, Map.class);
    }

    public Map<String, Object> deserializeToMap(String string){
        return deserialize(string, Map.class);
    }

    public <K, V> Map<K, V> deserializeToMap(String string, Class<K> keyType, Class<V> valueType){
        try {
            return OBJECT_MAPPER.readValue(string, OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, keyType, valueType));
        } catch (Exception e) {
            throw new RuntimeException(format("Exception while deserializing from string = '%s' to Map<K, V> where K = '%s', V = '%s'", string,  keyType, valueType), e);
        }
    }

    public <K, V> Map<K, V> deserializeToMap(InputStream is, Class<K> keyType, Class<V> valueType){
        try {
            return OBJECT_MAPPER.readValue(is, OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, keyType, valueType));
        } catch (Exception e) {
            throw new RuntimeException(format("Exception while deserializing from inputStream to Map<K, V> where K = '%s', V = '%s'", keyType, valueType), e);
        }
    }

    public <K, V> Map<K, V> deserializeToMap(File file, Class<K> keyType, Class<V> valueType){
        try {
            return OBJECT_MAPPER.readValue(file, OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, keyType, valueType));
        } catch (Exception e) {
            throw new RuntimeException(format("Exception while deserializing from file = '%s' to Map<K, V> where K = '%s', V = '%s'", file.getAbsolutePath(), keyType, valueType), e);
        }
    }

    public Map<String, Object> deserializeToMap(File file){
        return deserialize(file, Map.class);
    }

    public <T> List<T> deserializeToList(InputStream is, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(is, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            throw new RuntimeException(format("Exception while deserializing from inputStream to List<T> where T  = '%s'", clazz), e);
        }
    }

    public <T> List<T> deserializeToList(String jsonString, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(jsonString, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            throw new RuntimeException(format("Exception while deserializing from jsonString = '%s' to List<T> where T  = '%s'", jsonString, clazz), e);
        }
    }

    public List<Map<String, Object>> deserializeToListOfMap(String jsonString) {
        try {
            return OBJECT_MAPPER.readValue(jsonString, new TypeReference<List<Map<String, Object>>>(){});
        } catch (Exception e) {
            throw new RuntimeException(format("Exception while deserializing from jsonString = '%s' to List<Map<String, Object>>", jsonString), e);
        }
    }

    public List<Map<String, Object>> deserializeToListOfMap(InputStream inputStream) {
        try {
            return OBJECT_MAPPER.readValue(inputStream, new TypeReference<List<Map<String, Object>>>(){});
        } catch (Exception e) {
            throw new RuntimeException("Exception while deserializing from inputStream to List<Map<String, Object>>", e);
        }
    }

    public String serialize(Object object, boolean pretty) {
        try {
            if (pretty) {
                return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
            }
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Exception while serializing object to json. Pretty = " + pretty + ", Object = " + object, e);
        }
    }

    public String serialize(Object object) {
        return serialize(object, false);
    }

    public Map<String, Object> toMap(Object object){
        return OBJECT_MAPPER.convertValue(object, Map.class);
    }

    public <T> T fromMap(Map<String, Object> map, Class<T> clazz){
        return OBJECT_MAPPER.convertValue(map, clazz);
    }
}



