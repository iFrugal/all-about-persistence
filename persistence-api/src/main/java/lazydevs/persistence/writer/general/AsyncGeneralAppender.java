package lazydevs.persistence.writer.general;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Abhijeet Rai
 */
public class AsyncGeneralAppender<WI> implements GeneralAppender<WI> {

    private final GeneralQueueGuzzler queueGuzzler;

    public AsyncGeneralAppender(GeneralAppender<WI> appender){
        queueGuzzler = new GeneralQueueGuzzler<WI>(appender);
    }

    public AsyncGeneralAppender(GeneralAppender<WI> appender, int noOfProcessingThreads, long initialDelay, long delay, TimeUnit timeUnit) {
        this.queueGuzzler = new GeneralQueueGuzzler<WI>(appender, noOfProcessingThreads, initialDelay, delay, timeUnit);
    }

    @Override
    public Map<String, Object> create(Map<String, Object> t, WI wi) {
        this.queueGuzzler.add(t, wi);
        return t;
    }

    @Override
    public List<Map<String, Object>> create(List<Map<String, Object>> iterable, WI wi) {
        this.queueGuzzler.add(iterable, wi);
        return iterable;
    }

    @Override
    public Class<WI> getWriteInstructionType() {
        return this.queueGuzzler.getAppender().getWriteInstructionType();
    }
}
