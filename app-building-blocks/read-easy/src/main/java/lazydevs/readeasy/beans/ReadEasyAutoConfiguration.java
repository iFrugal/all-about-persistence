package lazydevs.readeasy.beans;

import lazydevs.readeasy.config.ReadEasyConfig;
import lazydevs.readeasy.controller.ConfiguredReadController;
import lazydevs.readeasy.devtools.DevModeQueryReloader;
import lazydevs.readeasy.registry.QueryRegistry;
import lazydevs.services.basic.validation.ParamValidator;
import lazydevs.springhelpers.dynabeans.DynaBeansAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;

/**
 * Auto-configuration for the Read-Easy framework.
 *
 * <p>Bootstraps:</p>
 * <ul>
 *   <li>{@link ConfiguredReadController} - the /read/* endpoints</li>
 *   <li>{@link QueryRegistry} - the single owner of registered queries</li>
 *   <li>{@link DevModeQueryReloader} - hot reload, only when {@code readeasy.devtools.enabled=true}</li>
 *   <li>{@link ParamValidator} - request parameter validation</li>
 * </ul>
 *
 * <p>Registered for Spring Boot 3 via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * (the legacy {@code spring.factories} entry is kept for Boot 2 consumers).</p>
 *
 * @author Abhijeet Rai
 */
@Configuration
@EnableConfigurationProperties(ReadEasyConfig.class)
@Import({ConfiguredReadController.class, ParamValidator.class})
public class ReadEasyAutoConfiguration {

    @Bean
    public QueryRegistry readEasyQueryRegistry(DynaBeansAutoConfiguration dynaBeansAutoConfiguration) {
        return new QueryRegistry(dynaBeansAutoConfiguration);
    }

    @Bean
    @ConditionalOnProperty(name = "readeasy.devtools.enabled", havingValue = "true")
    public DevModeQueryReloader devModeQueryReloader(ReadEasyConfig readEasyConfig,
                                                    ResourceLoader resourceLoader,
                                                    QueryRegistry queryRegistry) {
        return new DevModeQueryReloader(readEasyConfig, resourceLoader, queryRegistry);
    }
}
