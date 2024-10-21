package lazydevs.scheduleit;

import lazydevs.scheduleit.pojo.CronAttributes;
import lazydevs.scheduleit.pojo.LockAttributes;
import lazydevs.scheduleit.pojo.ScheduledInstruction;
import lazydevs.scheduleit.pojo.ScheduledTarget;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import static java.lang.Long.valueOf;
import static java.util.TimeZone.getDefault;
import static java.util.UUID.randomUUID;

/**
 * @author Abhijeet Rai
 */
@Slf4j
@Service
public class ScheduleItService {
    private final Map<String, ScheduledInstruction> map = new ConcurrentHashMap<>();
    private final TaskScheduler taskScheduler = taskScheduler();
    @Autowired private ApplicationContext applicationContext;

    private TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("ThreadPoolTaskScheduler");
        scheduler.setPoolSize(10);
        scheduler.initialize();
        return scheduler;
    }

    private LockingTaskExecutor getLockingTaskExecutor(LockProvider lockProvider) {
        return new DefaultLockingTaskExecutor(lockProvider);
    }

    public ScheduledInstruction addSchedule(CronAttributes cronAttributes, ScheduledTarget target){
        return addSchedule(null, cronAttributes, target, null);
    }

    public ScheduledInstruction addSchedule(LockProvider lockProvider, CronAttributes cronAttributes, String scheduleName, Runnable runnable, LockAttributes lockAttributes){
        if (null == lockAttributes) {
            log.info("Locking of schedule in disabled because either the bean 'scheduleItLockProvider' is not provided or lockAttributes is null for method = "+scheduleName);
        }else {
            Runnable finalRunnable = runnable;
            runnable = () -> getLockingTaskExecutor(lockProvider).executeWithLock(finalRunnable,
                    new LockConfiguration(Instant.now(), lockAttributes.getLockName(),
                            Duration.ofSeconds(valueOf(lockAttributes.getLockAtMostFor())),
                            Duration.ofSeconds(valueOf(lockAttributes.getLockAtLeastFor()))
                    )
            );
            scheduleName = lockAttributes.getLockName();
        }

        ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(
                runnable,
                new CronTrigger(cronAttributes.getCronExpression(), getTimeZone(cronAttributes.getTimeZone()))
        );
        ScheduledInstruction scheduledInstruction = new ScheduledInstruction();
        scheduledInstruction.setScheduledFuture(scheduledFuture);
        scheduledInstruction.setCronAttributes(cronAttributes);
        scheduledInstruction.setLockAttributes(lockAttributes);
        scheduledInstruction.setTarget(new ScheduledTarget(runnable));
        scheduledInstruction.setScheduleName(scheduleName);
        map.put(scheduledInstruction.getScheduleName(), scheduledInstruction);
        return scheduledInstruction;
    }

    public ScheduledInstruction addSchedule(LockProvider lockProvider, CronAttributes cronAttributes, ScheduledTarget target, LockAttributes lockAttributes){
        String scheduleName = target.getMethod().getName() + "-" + randomUUID().toString();
        return addSchedule(lockProvider, cronAttributes, scheduleName, getRunnable(target), lockAttributes);
    }


    private Runnable getRunnable(ScheduledTarget target){
        if(null != target.getRunnable()){
            return target.getRunnable();
        }
        return () -> {
            Object[] args = target.getArgs();
            Method method = target.getMethod();
            try {
                target.getPre().newInstance().run();
                if (args != null) {
                    method.invoke(applicationContext.getBean(method.getDeclaringClass()), args);
                } else {
                    method.invoke(applicationContext.getBean(method.getDeclaringClass()));
                }
                target.getPost().newInstance().run();
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                log.error("Exception while executing task " + method.getName(), e);
            }
        };
    }

    public ScheduledInstruction removeSchedule(String scheduleName) {
        ScheduledInstruction scheduledInstruction = getSchedule(scheduleName);
        scheduledInstruction.getScheduledFuture().cancel(false);
        map.remove(scheduleName);
        return scheduledInstruction;
    }

    /*public ScheduledInstruction editSchedule(ScheduledInstruction scheduledInstruction) {
        ScheduledInstruction oldScheduledInstruction = removeSchedule(scheduledInstruction.getScheduleName());
        addSchedule(scheduledInstruction.getCronAttributes(), scheduledInstruction.getTarget(), scheduledInstruction.getLockAttributes());
        return scheduledInstruction;
    }*/

    public ScheduledInstruction getSchedule(String scheduleName) {
        return map.get(scheduleName);
    }

    public void runSchedule(String scheduleName) {
         map.get(scheduleName).getTarget().getRunnable().run();
    }


    public List<ScheduledInstruction> getAllSchedules() {
        return map.entrySet().stream().map(e -> e.getValue()).collect(Collectors.toList());
    }

    private static TimeZone getTimeZone(String timeZone) {
        return StringUtils.hasText(timeZone) ? TimeZone.getTimeZone(timeZone) : getDefault();
    }
}
