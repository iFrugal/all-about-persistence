package lazydevs.readeasy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Test application for integration tests.
 * This minimal Spring Boot application provides the context needed
 * for integration tests of the Read-Easy framework.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "lazydevs.readeasy",
        "lazydevs.springhelpers"
})
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
