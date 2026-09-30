package lazydevs.transporter.autoconfig;

import lazydevs.scheduleit.ScheduleItAutoConfiguration;
import lazydevs.scheduleit.ScheduleItService;
import lazydevs.springhelpers.dynabeans.DynaBeansAutoConfiguration;
import lazydevs.transporter.TransportService;
import lazydevs.transporter.TransporterController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A host that only has the-transporter on its classpath (which brings dyna-beans-injector
 * and just-schedule-it-core): all three must be picked up through their
 * AutoConfiguration.imports files, with no manual @Import and no pipelines configured.
 */
@SpringBootTest(classes = TransporterAutoConfigurationTest.HostApp.class)
class TransporterAutoConfigurationTest {

    @Autowired private ApplicationContext applicationContext;

    @Test
    void transporterAndItsDependenciesAreAutoConfigured() {
        assertTrue(applicationContext.containsBean("dynaBeansGenerator"));
        assertEquals(1, applicationContext.getBeansOfType(DynaBeansAutoConfiguration.class).size());
        assertEquals(1, applicationContext.getBeansOfType(ScheduleItAutoConfiguration.class).size());
        assertEquals(1, applicationContext.getBeansOfType(ScheduleItService.class).size());
        assertEquals(1, applicationContext.getBeansOfType(TransportService.class).size());
        assertEquals(1, applicationContext.getBeansOfType(TransporterController.class).size());
        assertTrue(applicationContext.getBean(TransportService.class).getPipelines().isEmpty());
    }

    @SpringBootApplication
    static class HostApp {
        public static void main(String[] args) {
            SpringApplication.run(HostApp.class, args);
        }
    }
}
