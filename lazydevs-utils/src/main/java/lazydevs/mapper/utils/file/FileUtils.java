package lazydevs.mapper.utils.file;

import java.io.*;
import java.util.stream.Collectors;

/**
 * Created by Abhijeet Rai on 12/19/2018.
 */
public class FileUtils {

    public static String readFileAsString(String fileName){
        try(FileReader fr = new FileReader(fileName);
            BufferedReader br = new BufferedReader(fr);
        ) {
            return br.lines().collect(Collectors.joining("\n"));
        }catch (Throwable t){
            throw new RuntimeException(t);
        }

    }

    public static String readInputStreamAsString(InputStream inputStream){
        try(InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(inputStreamReader);
        ) {
            return br.lines().collect(Collectors.joining("\n"));
        }catch (Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static void writeToFile(String fileName, String content){
        try(FileWriter fw = new FileWriter(fileName)){
            fw.write(content);
            fw.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void delete(String filePath){
        boolean isDeleted = new File(filePath).delete();
        if(!isDeleted){
            throw new IllegalArgumentException("Unable to delete the file + "+filePath);
        }
    }

    public static void validateFileExists(String filepath, boolean isDirAccepted){
        File file = new File(filepath);
        if (!file.exists()){
            throw new IllegalArgumentException("File not found filePath = "+ filepath );
        }
        if(!isDirAccepted && file.isDirectory()){
                throw new IllegalArgumentException("File is a Directory = "+ filepath);
        }
    }

}
