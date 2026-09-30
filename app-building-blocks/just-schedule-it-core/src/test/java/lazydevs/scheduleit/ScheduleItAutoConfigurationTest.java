package lazydevs.scheduleit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This module does not depend on dyna-beans-injector, so its test classpath is
 * exactly a host that carries just-schedule-it-core without dyna-beans.
 */
class ScheduleItAutoConfigurationTest {

    @Test
    void dynaBeansIsAbsentFromThisClasspath() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("lazydevs.springhelpers.dynabeans.DynaBeansAutoConfiguration"));
    }

    @Test
    void backsOffWithoutDynaBeansInsteadOfFailingStartup() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ScheduleItAutoConfiguration.class))
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertFalse(context.containsBean(ScheduleItAutoConfiguration.class.getName()));
                    assertFalse(context.containsBean("scheduleItService"));
                });
    }
}
