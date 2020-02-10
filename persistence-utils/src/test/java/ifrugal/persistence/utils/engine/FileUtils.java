package ifrugal.persistence.utils.engine;

import java.io.*;
import java.util.stream.Collectors;

/**
 * Created by Abhijeet Rai on 12/19/2018.
 */
public class FileUtils {

    public static String readFileAsString(String fileName) throws IOException {
        try(FileReader fr = new FileReader(fileName);
            BufferedReader br = new BufferedReader(fr);
        ) {
            return br.lines().collect(Collectors.joining("\n"));
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
        new File(filePath).delete();
    }

    public static void validateFileExists(String filepath, boolean isDirAccepted){
        File file = new File(filepath);
        if (!file.exists()){
            throw new IllegalArgumentException("File not found filePath = "+ filepath );
        }if(!isDirAccepted){
            if(file.isDirectory()){
                throw new IllegalArgumentException("File is a Directory = "+ filepath);
            }
        }
    }
}
