package lazydevs.readeasy.integration;

import lazydevs.readeasy.beans.ReadEasyAutoConfiguration;
import lazydevs.readeasy.config.ReadEasyConfig;
import lazydevs.readeasy.devtools.DevModeQueryReloader;
import lazydevs.springhelpers.dynabeans.DynaBeansAutoConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DevModeQueryReloader.
 * Tests the hot-reload functionality with actual component initialization.
 */
@SpringBootTest(
        classes = {
                ReadEasyAutoConfiguration.class,
                DynaBeansAutoConfiguration.class,
                MockReadersTestConfiguration.class
        }
)
@ActiveProfiles("devtools")
@TestPropertySource(properties = {
        "readeasy.devtools.enabled=true",
        "readeasy.devtools.watchIntervalMs=500",
        "readeasy.devtools.validateOnReload=true",
        "readeasy.validation.enabled=true",
        "readeasy.validation.failOnError=false"
})
@DisplayName("DevModeQueryReloader Integration Tests")
@Disabled("Requires Spring context - run manually")
class DevModeQueryReloaderIntegrationTest {

    @Autowired(required = false)
    private DevModeQueryReloader devModeQueryReloader;

    @Autowired
    private ReadEasyConfig readEasyConfig;

    @Nested
    @DisplayName("Component Initialization")
    class ComponentInitialization {

        @Test
        @DisplayName("Should initialize DevModeQueryReloader when enabled")
        void shouldInitializeWhenEnabled() {
            assertNotNull(devModeQueryReloader,
                    "DevModeQueryReloader should be initialized when devtools.enabled=true");
        }

        @Test
        @DisplayName("Should have devtools config enabled")
        void shouldHaveDevtoolsConfigEnabled() {
            assertTrue(readEasyConfig.getDevtools().isEnabled(),
                    "Devtools should be enabled in config");
        }

        @Test
        @DisplayName("Should have correct watch interval")
        void shouldHaveCorrectWatchInterval() {
            assertEquals(500, readEasyConfig.getDevtools().getWatchIntervalMs(),
                    "Watch interval should match configured value");
        }

        @Test
        @DisplayName("Should have validateOnReload enabled")
        void shouldHaveValidateOnReloadEnabled() {
            assertTrue(readEasyConfig.getDevtools().isValidateOnReload(),
                    "ValidateOnReload should be enabled");
        }
    }

    @Nested
    @DisplayName("Status Reporting")
    class StatusReporting {

        @Test
        @DisplayName("Should return status information")
        void shouldReturnStatusInformation() {
            Map<String, Object> status = devModeQueryReloader.getStatus();

            assertNotNull(status, "Status should not be null");
            assertTrue((Boolean) status.get("enabled"),
                    "Status should show enabled=true");
            assertTrue((Boolean) status.get("validateOnReload"),
                    "Status should show validateOnReload value");
            assertNotNull(status.get("watchedFiles"),
                    "Status should include watched files");
        }
    }

    @Nested
    @DisplayName("Force Reload")
    class ForceReload {

        @Test
        @DisplayName("Should execute force reload without errors")
        void shouldExecuteForceReloadWithoutErrors() {
            // This test verifies that force reload doesn't throw exceptions
            // even if some files are not available (classpath resources in JAR)
            assertDoesNotThrow(() -> devModeQueryReloader.forceReloadAll(),
                    "Force reload should not throw exceptions");
        }
    }
}

/**
 * Tests for when DevModeQueryReloader is disabled.
 */
@SpringBootTest(
        classes = {
                ReadEasyAutoConfiguration.class,
                DynaBeansAutoConfiguration.class,
                MockReadersTestConfiguration.class
        }
)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "readeasy.devtools.enabled=false"
})
@DisplayName("DevModeQueryReloader Disabled Tests")
class DevModeQueryReloaderDisabledTest {

    @Autowired(required = false)
    private DevModeQueryReloader devModeQueryReloader;

    @Test
    @DisplayName("Should not initialize when disabled")
    void shouldNotInitializeWhenDisabled() {
        assertNull(devModeQueryReloader,
                "DevModeQueryReloader should be null when devtools.enabled=false");
    }
}
