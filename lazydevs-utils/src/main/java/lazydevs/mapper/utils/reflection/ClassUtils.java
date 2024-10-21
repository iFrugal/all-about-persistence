package lazydevs.mapper.utils.reflection;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.lang.String.format;

public class ClassUtils {
    private ClassUtils(){}

    public static Object construct(String fqcn, Arg... consArgInOrder){
        try {
            Class<?> cl = loadClass(fqcn);
            if(null == consArgInOrder){
                consArgInOrder = new Arg[]{};
            }
            Constructor<?> constructor = cl.getConstructor(Stream.of(consArgInOrder).map(arg -> arg.getType()).toArray(size -> new Class<?>[size]));
            return constructor.newInstance(Stream.of(consArgInOrder).map(arg -> arg.getVal()).toArray(size -> new Object[size]));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Class<?> loadClass(String fqcn){
        try {
            switch(fqcn){
                case "boolean" : return boolean.class;
                case "byte" : return byte.class;
                case "short" : return short.class;
                case "int" : return int.class;
                case "long" : return long.class;
                case "float" : return float.class;
                case "double" : return double.class;
                case "char" : return char.class;
                case "int[]" : return int[].class;
                case "double[]" : return double[].class;
                case "long[]" : return long[].class;
                default:
                    if(fqcn.endsWith("[]")){
                        String elementFqcn = fqcn.substring(0, fqcn.lastIndexOf("["));
                        return Class.forName("[L" + elementFqcn + ";");
                    }
                    return Class.forName(fqcn);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Object getVal(String fqcn, String val){
        try {
            switch(fqcn){
                case "boolean" : case "java.lang.Boolean": return Boolean.valueOf(val);
                case "byte" : case "java.lang.Byte" : return Byte.valueOf(val);
                case "short" : case "java.lang.Short" : return Short.valueOf(val);
                case "int" : case "java.lang.Integer" : return Integer.valueOf(val);
                case "long" : case "java.lang.Long" : return Long.valueOf(val);
                case "float" : case "java.lang.Float" : return Float.valueOf(val);
                case "double" : case "java.lang.Double" : return Double.valueOf(val);
                case "java.lang.String": return val;
                default:
                    throw new IllegalArgumentException("Not handled fqcn = "+fqcn);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> Class<T> loadClassAssignableFrom(String fqcn, Class<T> assigneeClass){
        Class<?> clazz = loadClass(fqcn);
        if (assigneeClass.isAssignableFrom(clazz)) {
            return (Class<T>) clazz;
        }else{
            throw new IllegalArgumentException(String.format("The class = '%s' does not extends assigneeClass('%s')", fqcn, assigneeClass.getName()));
        }
    }

    public static Class<?> loadClassAssignableFrom(String fqcn, String assigneeClassFqcn){
        Class<?> assigneeClass = loadClass(assigneeClassFqcn);
        return loadClassAssignableFrom(fqcn, assigneeClass);
    }

    public static Object construct(String className, Arg[] consArgInOrder, NamedArg... attributes){
        try {
            Object obj = construct(className, consArgInOrder);
            if(!(null == attributes || attributes.length == 0)) {
                Map<String, Field> fieldMap = new HashMap<>();
                Consumer<? super Field> action = f -> fieldMap.put(f.getName(), f);
                Arrays.stream(obj.getClass().getDeclaredFields()).forEach(action);
                Arrays.stream(obj.getClass().getFields()).forEach(action);
                Stream.of(attributes).forEach(attribute -> {
                    try {
                        Field field = fieldMap.get(attribute.getName());
                        field.setAccessible(true);
                        field.set(obj, attribute.getVal());
                    } catch (IllegalAccessException e) {
                        throw new IllegalArgumentException(format("Error while setting field with name = '%s' in class = '%s'", attribute.getName(), obj.getClass()), e);
                    }
                });
            }
            return obj;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Object construct(String className, Arg[] consArgInOrder, NamedArg[] attributes, NamedMethod... namedMethods){
        try {
            Object obj = construct(className, consArgInOrder, attributes);
            if (!(null == namedMethods || namedMethods.length == 0)) {
                Stream.of(namedMethods).forEach(namedMethod -> {
                    try {
                        Method method = obj.getClass().getMethod(namedMethod.getName(), namedMethod.getType());
                        method.invoke(obj, namedMethod.getVal());
                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        throw new IllegalArgumentException(format("Error while calling setterMethod = %s in class = '%s'", namedMethod, obj.getClass()), e);
                    }
                });
            }
            return obj;
        }catch (Exception e){
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> T getInterfaceRef(String className, Class<T> interface1, Arg... consArgInOrder){
        return getInterfaceRef(className, interface1, consArgInOrder, new NamedArg[]{}, new NamedMethod[]{});
    }

    public static <T> T getInterfaceRef(String className, Class<T> interface1, Arg[] consArgInOrder, NamedArg... attributes){
        return getInterfaceRef(className, interface1, consArgInOrder, attributes, new NamedMethod[]{});
    }

    public static <T> T getInterfaceRef(String className, Class<T> interface1, Arg[] consArgInOrder, NamedArg[] attributes, NamedMethod... namedMethods){
        try {
            Object obj = construct(className, consArgInOrder, attributes, namedMethods);
            if (interface1.isInstance(obj)){
                return (T)obj;
            }else{
                throw new IllegalArgumentException(obj.getClass() + " is not an implementation of " + interface1);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @RequiredArgsConstructor @Getter @ToString
    public static class Arg<T>{
        private final T val;
        private final Class<T> type;
    }

    @RequiredArgsConstructor @Getter @ToString
    public static class NamedArg {
        private final String name;
        private final Object val;
    }

    @Getter @ToString @RequiredArgsConstructor
    public static class NamedMethod<T> {
        private final String name;
        private final Object val;
        private final Class<T> type;
    }
}
