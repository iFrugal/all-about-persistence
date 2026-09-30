package lazydevs.readeasy.it.manualimport;

import lazydevs.readeasy.it.ReadEasyContextTest;
import lazydevs.springhelpers.dynabeans.DynaBeansAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * A host that still carries the old workaround of importing
 * DynaBeansAutoConfiguration by hand. Now that dyna-beans is also registered as
 * auto-configuration, the two registrations must collapse into one, with no
 * bean-definition-overriding failure.
 */
@SpringBootTest(classes = ManuallyImportedDynaBeansHostTest.HostApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("readeasy-it")
class ManuallyImportedDynaBeansHostTest extends ReadEasyContextTest {

    @SpringBootApplication
    @Import(DynaBeansAutoConfiguration.class)
    static class HostApp {
        public static void main(String[] args) {
            SpringApplication.run(HostApp.class, args);
        }
    }
}
