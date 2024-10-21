package lazydevs.mapper.utils.reflection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;
import java.util.function.Function;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.lang.System.getProperty;
import static java.util.stream.Collectors.toList;
import static lazydevs.mapper.utils.reflection.ClassUtils.Arg;
import static lazydevs.mapper.utils.reflection.ClassUtils.NamedArg;
import static lazydevs.mapper.utils.reflection.ClassUtils.loadClass;

@Getter @Setter @JsonInclude(NON_NULL)
public class InitDTO {

    String fqcn;
    String beanName;
    List<ArgDTO> constructorArgs;
    List<NamedArgDTO> attributes;

    public InitDTO(@NonNull String fqcn, List<ArgDTO> constructorArgs, List<NamedArgDTO> attributes) {
        this.fqcn = fqcn;
        this.constructorArgs = constructorArgs;
        this.attributes = attributes;
    }

    public InitDTO(String beanName) {
        this.beanName = beanName;
    }

    public InitDTO() {
    }


    @JsonIgnore
    public Init buildInit(Function<String, Object> envSupplier){
        return Init.builder()
                   .fqcn(fqcn)
                   .constructorArgsInOrder(null == constructorArgs ? null : constructorArgs.stream()
                                                                                           .map(argDTO -> argDTO.getArg(envSupplier))
                                                                                           .collect(toList()))
                   .attributes(null == attributes ? null : attributes.stream()
                                                                     .map(namedArgDTO -> namedArgDTO.getNamedArg(envSupplier))
                                                                     .collect(toList()))
                   .build();
    }


    @Getter @Setter
    public static class ArgDTO{
        private String typeFqcn;
        private Object val;
        private String sysPropKey;
        private String beanName;

        public ArgDTO(String typeFqcn, Object val, String beanName) {
            this.typeFqcn = typeFqcn;
            this.val = val;
            this.beanName = beanName;
        }

        public ArgDTO(String typeFqcn, Object val) {
            this.typeFqcn = typeFqcn;
            this.val = val;
        }

        public ArgDTO() {
        }

        @JsonIgnore
        public Arg getArg(Function<String, Object> envSupplier){
            if (null != sysPropKey) {
                val = null == envSupplier? System.getProperty(sysPropKey) : envSupplier.apply(sysPropKey);
                if(null ==  typeFqcn){
                    typeFqcn = String.class.getName();
                }else{
                    val = ClassUtils.getVal(typeFqcn, (String)val);
                }
            }
            return new Arg(val, loadClass(typeFqcn));
        }
    }

    @Getter @Setter
    public static class NamedArgDTO {
        private String name;
        private Object val;
        private String sysPropKey;
        private String beanName;

        public NamedArgDTO(String name, Object val, String beanName) {
            this.name = name;
            this.val = val;
            this.beanName = beanName;
        }

        public NamedArgDTO(String name, Object val) {
            this.name = name;
            this.val = val;
        }

        public NamedArgDTO() {
        }


        public NamedArg getNamedArg(Function<String, Object> envSupplier){
            if (null != sysPropKey) {
                val = null == envSupplier? System.getProperty(sysPropKey) : envSupplier.apply(sysPropKey);
            }
            return new NamedArg(name, val);
        }
    }
}