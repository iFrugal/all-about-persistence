package lazydevs.springhelpers.dynabeans;

import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.engine.ScriptEngines;
import lazydevs.mapper.utils.reflection.ClassUtils;
import lazydevs.mapper.utils.reflection.InitDTO.ArgDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static lazydevs.mapper.utils.file.FileUtils.readInputStreamAsString;
import static lazydevs.mapper.utils.reflection.ReflectionUtils.getInterfaceReference;

/**
 * @author Abhijeet Rai
 */
@Configuration
@EnableConfigurationProperties(DynaBeansConfig.class)
@Slf4j
@Import(ApplicationContextHolder.class)
public class DynaBeansAutoConfiguration {
    @Autowired private ApplicationContext applicationContext;
    @Autowired private Environment environment;

    @Autowired private ResourceLoader resourceLoader;
    @Value("${dynabeans.banner.path:classpath:dynabeans-banner.txt}")
    private String dynabeansBannerPath;


    @Bean
    public String dynaBeansGenerator(DynaBeansConfig dynaBeansConfig) throws IOException {
        log.info(readInputStreamAsString(applicationContext.getResource(dynabeansBannerPath).getInputStream()));
        for (ScriptEngines value : ScriptEngines.values()) {
            if(null != value.getScriptEngine()){
                log.info("Putting applicationContext in scriptEngine = {} {}", value, value.getScriptEngine());
                value.getScriptEngine().put("applicationContext", applicationContext);
            }
        }
        initializeAndInject(dynaBeansConfig);
        return "";
    }

    public void initializeAndInject(DynaBeansConfig dynaBeansConfig){
        initializeAndInject(null, dynaBeansConfig);
    }

    private static String getPrefixedBeanName(String beanNamespace, String beanName){
        String prefix = null == beanNamespace ? "" : beanNamespace + ".";
        return prefix + beanName;
    }


    @RequiredArgsConstructor
    public static class BeanSupplier implements Function<String, Object>{
        private final ApplicationContext applicationContext;
        private final String beanNamespace;
        @Override
        public Object apply(String beanName) {
            try {
                return applicationContext.getBean(getPrefixedBeanName(beanNamespace, beanName));
            }catch (Exception e){
                return applicationContext.getBean(beanName);
            }
        }
    }

    public Object construct(DynaBeansConfig.InitInstruction initInstruction, String beanNamespace){
        validateInstruction(initInstruction);
        Object instance = null;
        if(null != initInstruction.getVal()){
            Map<String, Object> map = initInstruction.getVal();
            Class<?> type = initInstruction.getType();
            instance = null != type ? SerDe.JSON.fromMap(map, type) : map;
        }else if (initInstruction.getInitDTO() != null) {
            instance = getInterfaceReference(initInstruction.getInitDTO(), Object.class, new BeanSupplier(applicationContext, beanNamespace), environment::getProperty);
        } else if(null != initInstruction.getScript()){
            instance = callScript(initInstruction.getScript(), beanNamespace);
        }
        return instance;
    }

    public Object callScript(DynaBeansConfig.ScriptInstruction script, String beanNamespace){
        return script.getEngine().invokeFunction(script.getFunctionName(), getArgs(beanNamespace, script.getArgs()));
    }

    public Object construct(DynaBeansConfig.InitInstruction initInstruction){
        return construct(initInstruction, null);
    }


    public void initializeAndInject(String beanNamespace, DynaBeansConfig dynaBeansConfig){
        if(null == dynaBeansConfig){
            return;
        }
        ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext)applicationContext).getBeanFactory();
        loadScripts(dynaBeansConfig);
        dynaBeansConfig.getInit().entrySet().forEach(e -> {
            String beanName = getPrefixedBeanName(beanNamespace, e.getKey());
            log.info("Creating bean with name = {}, initInstruction = {}", beanName, e.getValue());
            try {
                beanFactory.registerSingleton(beanName, construct(e.getValue(), beanNamespace));
            }catch (Exception ex){
                throw new IllegalArgumentException("Error while creating bean with beanName = " + beanName, ex);
            }
        });
    }

    private Object[] getArgs(String beanNamespace, List<ArgDTO> list){
        return list.stream().map(argDTO -> getVal(argDTO, new BeanSupplier(applicationContext, beanNamespace))).toArray();
    }

    private Object getVal(ArgDTO argDTO, Function<String, Object> beanSupplier) {
        if (Class.class.getName().equals(argDTO.getTypeFqcn())) {
            if (null != argDTO.getVal() && argDTO.getVal() instanceof String) {
                return ClassUtils.loadClass(String.valueOf(argDTO.getVal()));
            }
        } else if (null != argDTO.getBeanName()) {
            return beanSupplier.apply(argDTO.getBeanName());
        } else if(null != argDTO.getSysPropKey()){
            Object val = environment.getProperty(argDTO.getSysPropKey());
            if(null != argDTO.getTypeFqcn()){
                val = ClassUtils.getVal(argDTO.getTypeFqcn(), (String)val);
            }
            return val;
        }
        return argDTO.getVal();
    }


    private void validateInstruction(DynaBeansConfig.InitInstruction instruction) {
        if(null == instruction.getVal() && null == instruction.getInitDTO() && null == instruction.getScript()){
            throw new IllegalArgumentException("All possible attributes val, initDTO and script are null, can't create object");
        }else if (null != instruction.getInitDTO() && null != instruction.getScript()){
            throw new IllegalArgumentException("Both initDTO and script are present, dyna-bean is confused which one to use to create the instance");
        }
    }

    private void loadScripts(DynaBeansConfig dynaBeansConfig){
        dynaBeansConfig.getScriptPathsByEngine().forEach((scriptEngine, value) -> value.forEach(path -> {
            log.info("Registering the script file. engine = {},  filePath = {}",scriptEngine, path);
            try {
                scriptEngine.loadScript(readInputStreamAsString(resourceLoader.getResource(path).getInputStream()));
            } catch (IOException e) {
                throw new IllegalArgumentException("Script can't be load from path = "+ path, e);
            }
        }));
    }
}
