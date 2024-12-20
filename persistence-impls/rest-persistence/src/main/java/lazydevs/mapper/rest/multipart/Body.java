package lazydevs.mapper.rest.multipart;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.InputStream;

public interface Body<T> {

    abstract T getBody();
    abstract String getName();
    abstract Type getType();

    public enum Type{
        STRING, FILE, INPUT_STREAM, BYTE_ARRAY;
    }

    @Getter @RequiredArgsConstructor
    public static class FileBody implements Body<File>{
        private Type type = Type.FILE;
        private final String name;
        private final File body;
    }

    @RequiredArgsConstructor @Getter
    public static class InputStreamBody implements Body<InputStream>{
        private Type type = Type.INPUT_STREAM;
        private final String name;
        private final InputStream body;
    }

    @RequiredArgsConstructor @Getter
    public static class StringBody implements Body<String>{
        private Type type = Type.STRING;
        private final String name;
        private final String body;
    }

    @RequiredArgsConstructor @Getter
    public static class ByteArrayBody implements Body<byte[]>{
        private Type type = Type.BYTE_ARRAY;
        private final String name;
        private final byte[] body;
    }

    static Body getInstance(String name, File file){
        return new FileBody(name, file);
    }

    static Body getInstance(String name, InputStream inputStream){
        return new InputStreamBody(name, inputStream);
    }

    static Body getInstance(String name, String string){
        return new StringBody(name, string);
    }

    static Body getInstance(String name, byte[] bytes){
        return new ByteArrayBody(name, bytes);
    }

}


