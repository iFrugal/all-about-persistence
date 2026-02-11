package lazydevs.readeasy.beans;

import lazydevs.readeasy.config.ReadEasyConfig;
import lazydevs.readeasy.controller.ConfiguredReadController;
import lazydevs.readeasy.devtools.DevModeQueryReloader;
import lazydevs.readeasy.validation.QueryValidator;
import lazydevs.services.basic.validation.ParamValidator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for Read-Easy framework.
 *
 * <p>This configuration class bootstraps all Read-Easy components including:</p>
 * <ul>
 *   <li>{@link ConfiguredReadController} - REST endpoints for query execution</li>
 *   <li>{@link QueryValidator} - Startup-time query validation</li>
 *   <li>{@link DevModeQueryReloader} - Hot-reload support in development mode</li>
 *   <li>{@link ParamValidator} - Request parameter validation</li>
 * </ul>
 *
 * @author Abhijeet Rai
 */
@Configuration
@EnableConfigurationProperties(ReadEasyConfig.class)
@Import({ConfiguredReadController.class, ParamValidator.class, QueryValidator.class, DevModeQueryReloader.class})
public class ReadEasyAutoConfiguration {

}
