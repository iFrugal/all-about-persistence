package lazydevs.scheduleit;

import lazydevs.mapper.utils.SerDe;
import lazydevs.scheduleit.annotations.ScheduleIt;
import lazydevs.scheduleit.pojo.CronAttributes;
import lazydevs.scheduleit.pojo.LockAttributes;
import lazydevs.scheduleit.pojo.ScheduledTarget;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.expression.Expression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.reflections.util.ClasspathHelper.forPackage;
import static org.springframework.util.StringUtils.isEmpty;

@Slf4j
@Configuration
@DependsOn("dynaBeansGenerator") @Import({ScheduleItConfig.class, ScheduleItService.class})
public class ScheduleItAutoConfiguration {
    @Autowired private Environment environment;
    @Autowired private ScheduleItConfig scheduleItConfig;
    @Autowired private ScheduleItService scheduleItService;
    @Autowired private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        Set<Method> methods = getAnnotatedMethods();
        log.info("No. of tasks to schedule : " + methods.size());
        LockProvider lockProvider = null;
        try {
            lockProvider = applicationContext.getBean(LockProvider.class);
        }catch (BeansException e){
            log.warn("No LockProvider bean is provided");
        }
        for (Method method : methods) {
            handleAnnotatedMethods(lockProvider, method);
        }
    }

    private void handleAnnotatedMethods(LockProvider lockProvider, Method method) {
        ScheduleIt scheduledEasily = method.getAnnotation(ScheduleIt.class);
        validateAttributes(scheduledEasily);
        if (!isEmpty(scheduledEasily.cron())) {
            handleCron(lockProvider, method, scheduledEasily);
        } else if(!isEmpty(scheduledEasily.multiCron())) {
            handleMultiCron(lockProvider, method, scheduledEasily);
        }
    }

    private void handleMultiCron(LockProvider lockProvider, Method method, ScheduleIt scheduledEasily) {
        List<ScheduleIt.MultiCron> multiCrons = SerDe.JSON.deserializeToList(getValue(scheduledEasily.multiCron()), ScheduleIt.MultiCron.class);
        requireNonNull(multiCrons, "Could not load the property multiCron");
        multiCrons.stream()
            .forEach(multiCron -> {
                ScheduledTarget target = new ScheduledTarget(method);
                target.setArgs(multiCron.getParamsInOrder());
                CronAttributes cronAttributes = new CronAttributes(getValue(null == multiCron.getCronExpression() ? scheduledEasily.cron() : multiCron.getCronExpression()));
                cronAttributes.setTimeZone(getValue(multiCron.getTimezone()));
                LockAttributes lockAttributes = null;
                lockAttributes = handleMultiCronLock(scheduledEasily, multiCron, lockAttributes);
                scheduleItService.addSchedule(lockProvider, cronAttributes, target, lockAttributes);
            });
    }

    private LockAttributes handleMultiCronLock(ScheduleIt scheduledEasily, ScheduleIt.MultiCron multiCron, LockAttributes lockAttributes) {
        if(scheduledEasily.locked()) {
            String lockName =  null == multiCron.getLockName() ? scheduledEasily.lockName() :  multiCron.getLockName();
            if(isEmpty(lockName)){
                throw new IllegalArgumentException("Locking is opted, but no lockName is provided. There is a scope of improvement in library to autoCreate lockName in such a way that, it is common across nodes, may be method signature/definition or a hash of it. But, for now, you have to live with it, please contribute if you can.");
            }
            lockAttributes = new LockAttributes(lockName);
            lockAttributes.setLockAtLeastFor(null == multiCron.getLockAtLeastFor() ? scheduledEasily.lockAtLeastFor() : multiCron.getLockAtLeastFor());
            lockAttributes.setLockAtMostFor(null == multiCron.getLockAtMostFor() ? scheduledEasily.lockAtMostFor() : multiCron.getLockAtMostFor());
        }
        return lockAttributes;
    }

    private void handleCron(LockProvider lockProvider, Method method, ScheduleIt scheduledEasily) {
        ScheduledTarget target = new ScheduledTarget(method);
        CronAttributes cronAttributes = new CronAttributes(getValue(scheduledEasily.cron()));
        cronAttributes.setTimeZone(getValue(scheduledEasily.timezone()));
        LockAttributes lockAttributes = null;
        if(scheduledEasily.locked()) {
            if(isEmpty(scheduledEasily.lockName())){
                throw new IllegalArgumentException("Locking is opted, but no lockName is provided. There is a scope of improvement in library to autoCreate lockName in such a way that, it is common across nodes, may be method signature/definition or a hash of it. But, for now, you have to live with it, please contribute if you can.");
            }
            lockAttributes = new LockAttributes(scheduledEasily.lockName());
            lockAttributes.setLockAtLeastFor(scheduledEasily.lockAtLeastFor());
            lockAttributes.setLockAtMostFor(scheduledEasily.lockAtMostFor());
        }
        scheduleItService.addSchedule(lockProvider, cronAttributes, target, lockAttributes);
    }


    private void validateAttributes(ScheduleIt scheduledEasily) {
        if (isEmpty(scheduledEasily.cron()) && isEmpty(scheduledEasily.multiCron())) {
            throw new IllegalArgumentException("Both cron and multiCron cannot be null.");
        }
        if (!isEmpty(scheduledEasily.cron()) && !isEmpty(scheduledEasily.multiCron())) {
            throw new IllegalArgumentException("Any one of cron and multiCron should be provided.");
        }
    }

    private Set<Method> getAnnotatedMethods() {
        List<URL> urls = new ArrayList<>();
        scheduleItConfig.getPackagesToScan().stream().forEach(package1-> urls.addAll(forPackage(package1)));
        Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(urls)
                .setScanners(Scanners.MethodsAnnotated));
        return reflections.getMethodsAnnotatedWith(ScheduleIt.class);
    }



    private String getValue(String key) {
        String value = key;
        if (key.startsWith("${") && key.endsWith("}")) {
            Expression expression = new SpelExpressionParser().parseExpression(value, new TemplateParserContext("${", "}"));
            value = environment.getProperty(requireNonNull(expression.getExpressionString(), "Expression cannot be null"));
        }
        return value;
    }
}