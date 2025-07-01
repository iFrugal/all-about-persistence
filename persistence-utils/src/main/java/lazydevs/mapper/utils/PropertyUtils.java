package lazydevs.mapper.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyUtils {
     private static Properties properties = new Properties();

     public static void load(String filePath) {
         try (InputStream is = new FileInputStream(new File(filePath))){
             properties.load(is);
         } catch (IOException e) {
             throw new RuntimeException("Error while  loading property file.", e);
         }
     }

    public static String getValue(String key){
        String value= properties.getProperty(key);
        return null == value ? null : value.trim();
    }
}
