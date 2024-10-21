package lazydevs.persistence.writer.general;

import lazydevs.persistence.connection.multitenant.TenantContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Abhijeet Rai
 */
@Slf4j
public class GeneralQueueGuzzler<WI>{

    @Getter @Setter @AllArgsConstructor
    private class ElementWithWriteInstruction<T>{
        private T element;
        private WI wi;
        private final String tenantId = TenantContext.getTenantId();
    }

    private final int queueSize =  1_000;
    private final BlockingQueue<ElementWithWriteInstruction<Map<String, Object>>> eventQueue;
    private final BlockingQueue<ElementWithWriteInstruction<Collection<Map<String, Object>>>> multiEventQueue;
    @Getter private final GeneralAppender<WI> appender;
    public void add(Map<String, Object> t, WI wi){
        eventQueue.add(new ElementWithWriteInstruction<>(t, wi));
    }

    public void add(List<Map<String, Object>> iterable, WI wi){
        multiEventQueue.add(new ElementWithWriteInstruction<>(iterable, wi));
    }

    public GeneralQueueGuzzler(GeneralAppender appender) {
        this(appender, 1, 30, 1, TimeUnit.SECONDS);
    }

    public GeneralQueueGuzzler(GeneralAppender appender, int noOfProcessingThreads, long initialDelay, long delay, TimeUnit timeUnit) {
        this.appender = appender;
        eventQueue = new ArrayBlockingQueue<>(queueSize);
        multiEventQueue = new ArrayBlockingQueue<>(queueSize);
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(noOfProcessingThreads);
        for(int i = 0; i < noOfProcessingThreads; i++){
            scheduledExecutorService.scheduleWithFixedDelay(new AsynEventProcessor(), initialDelay, delay, timeUnit);
        }
    }

    private class AsynEventProcessor implements Runnable {
        @Override
        public void run() {
            try {
                ElementWithWriteInstruction<Map<String, Object>> t = eventQueue.poll(10, TimeUnit.MILLISECONDS);
                ElementWithWriteInstruction<Collection<Map<String, Object>>> collection = multiEventQueue.poll(10, TimeUnit.MILLISECONDS);
                if (!(null == t && null == collection)) {
                    if (null != t) {
                        TenantContext.setTenantId(t.getTenantId());
                        appender.create(t.element, t.wi);
                    }
                    if (null != collection) {
                        TenantContext.setTenantId(collection.getTenantId());
                        appender.create(new ArrayList<>(collection.element), collection.wi);
                    }
                }
            } catch (Throwable e) {
                log.error("Error while polling the eventQueue", e);
                Thread.currentThread().interrupt();
                throw new RuntimeException("Error while polling the eventQueue", e);
            }
        }
    }
}
