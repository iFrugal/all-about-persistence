package lazydevs.conman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

/**
 * @author Abhijeet Rai
 */

@SpringBootApplication
@EnableAutoConfiguration(exclude = MongoAutoConfiguration.class)
public class ConmanTestMain {
    public static void main(String[] args) {
        SpringApplication.run(ConmanTestMain.class, args);
    }
}
