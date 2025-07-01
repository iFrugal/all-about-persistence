package lazydevs.mapper.utils.reflection;

import java.util.function.Function;

import static lazydevs.mapper.utils.reflection.ClassUtils.*;

public class ReflectionUtils {

    public static <T> T getInterfaceReference(Init init, Class<T> interface1) {
        Arg[] consArgs = init.getConstructorArgsInOrder() != null
                ? init.getConstructorArgsInOrder().toArray(new Arg[]{})
                : null;

        NamedArg[] attributes = init.getAttributes() != null
                ? init.getAttributes().toArray(new NamedArg[]{})
                : null;

        NamedMethod[] namedMethods = init.getNamedMethods() != null
                ? init.getNamedMethods().toArray(new NamedMethod[]{})
                : null;

        return getInterfaceRef(init.getFqcn(), interface1, consArgs, attributes, namedMethods);
    }

    public static <T> T getInterfaceReference(InitDTO initDTO, Class<T> interface1){
        return getInterfaceReference(initDTO.buildInit(null), interface1);
    }

    public static <T> T getInterfaceReference(InitDTO initDTO, Class<T> interface1, Function<String, Object> beanSupplier){
        if(null != initDTO.getBeanName()){
            Object obj = beanSupplier.apply(initDTO.getBeanName());
            if (interface1.isInstance(obj)){
                return (T)obj;
            }else{
                throw new IllegalArgumentException(obj.getClass() + " is not an implementation of " + interface1);
            }
        }
        injectBeans(initDTO, beanSupplier);
        return getInterfaceReference(initDTO.buildInit(null), interface1);
    }
    public static <T> T getInterfaceReference(InitDTO initDTO, Class<T> interface1, Function<String, Object> beanSupplier, Function<String, Object> envSupplier){
        if(null != initDTO.getBeanName()){
            Object obj = beanSupplier.apply(initDTO.getBeanName());
            if (interface1.isInstance(obj)){
                return (T)obj;
            }else{
                throw new IllegalArgumentException(obj.getClass() + " is not an implementation of " + interface1);
            }
        }
        injectBeans(initDTO, beanSupplier);
        return getInterfaceReference(initDTO.buildInit(envSupplier), interface1);
    }

    private static void injectBeans(InitDTO initDTO, Function<String, Object> beanSupplier){
        if(null != initDTO) {
            if(null != initDTO.getConstructorArgs()) {
                initDTO.getConstructorArgs().forEach(argDTO -> injectBean(argDTO, beanSupplier));
            }
            if(null != initDTO.getAttributes()) {
                initDTO.getAttributes().forEach(argDTO -> injectBean(argDTO, beanSupplier));
            }
        }
    }

    private static void injectBean(InitDTO.ArgDTO argDTO, Function<String, Object> beanSupplier){
        if(Class.class.getName().equals(argDTO.getTypeFqcn())) {
            if(null != argDTO.getVal() && argDTO.getVal() instanceof String){
                argDTO.setVal(loadClass(String.valueOf(argDTO.getVal())));
            }
        }else if(null != argDTO.getBeanName()){
            argDTO.setVal(beanSupplier.apply(argDTO.getBeanName()));
        }
    }

    private static void injectBean(InitDTO.NamedArgDTO argDTO, Function<String, Object> beanSupplier){
        if(null != argDTO.getBeanName()){
            argDTO.setVal(beanSupplier.apply(argDTO.getBeanName()));
        }
    }
}
