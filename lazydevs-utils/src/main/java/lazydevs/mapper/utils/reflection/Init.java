package lazydevs.mapper.utils.reflection;

import lazydevs.mapper.utils.reflection.ClassUtils.Arg;
import lazydevs.mapper.utils.reflection.ClassUtils.NamedArg;
import lazydevs.mapper.utils.reflection.ClassUtils.NamedMethod;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter @ToString @Builder
public class Init{
    private String fqcn;
    private List<Arg> constructorArgsInOrder;
    private List<NamedArg> attributes;
    private List<NamedMethod> namedMethods;
}

