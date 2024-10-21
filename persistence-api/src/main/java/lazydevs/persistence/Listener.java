package lazydevs.persistence;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Abhijeet Rai
 */
@RequiredArgsConstructor
public abstract class Listener<T> {
    private final Consumer<Message<T>> consumer;

    public void listen(Message<T> message) {
        consumer.accept(message);
    }

    @Getter @Setter @ToString @NoArgsConstructor @AllArgsConstructor
    public static class Message<T>{
        private T payload;
        private Map<String, Object> headers;
    }
}
