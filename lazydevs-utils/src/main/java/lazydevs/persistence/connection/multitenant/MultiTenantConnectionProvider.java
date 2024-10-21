package lazydevs.persistence.connection.multitenant;

import lazydevs.mapper.utils.reflection.InitDTO;
import lazydevs.persistence.connection.ConnectionProvider;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static lazydevs.mapper.utils.reflection.ReflectionUtils.getInterfaceReference;

@RequiredArgsConstructor
public class MultiTenantConnectionProvider<T> implements ConnectionProvider<T> {
    private final Function<String, T> mappingFunction;
    private final Map<String, T> connectionHolderMap = new ConcurrentHashMap<>();

    public MultiTenantConnectionProvider(String mappingFunctionFqcn){
        InitDTO init = new InitDTO();
        init.setFqcn(mappingFunctionFqcn);
        this.mappingFunction = getInterfaceReference(init, Function.class);
    }

    public T getConnection() {
        return connectionHolderMap.computeIfAbsent(TenantContext.getTenantId(), mappingFunction);
    }
}
