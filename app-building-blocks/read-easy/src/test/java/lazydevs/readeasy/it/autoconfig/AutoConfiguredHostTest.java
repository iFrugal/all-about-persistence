package lazydevs.readeasy.it.autoconfig;

import lazydevs.readeasy.it.ReadEasyContextTest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * A host that only adds read-easy to its classpath: read-easy and dyna-beans
 * must both come up through Boot auto-configuration, with no manual @Import.
 */
@SpringBootTest(classes = AutoConfiguredHostTest.HostApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("readeasy-it")
class AutoConfiguredHostTest extends ReadEasyContextTest {

    @SpringBootApplication
    static class HostApp {
        public static void main(String[] args) {
            SpringApplication.run(HostApp.class, args);
        }
    }
}
