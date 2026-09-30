package lazydevs.conman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mongo auto-configuration is excluded by name so this source compiles on every
 * supported Boot line: Boot 3 ships it in spring-boot-autoconfigure, Boot 4 moved
 * it to the spring-boot-mongodb module. Names absent from the classpath are ignored.
 *
 * @author Abhijeet Rai
 */

@SpringBootApplication(excludeName = {
        "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration",
        "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration"
})
public class ConmanTestMain {
    public static void main(String[] args) {
        SpringApplication.run(ConmanTestMain.class, args);
    }
}
