package lazydevs.readeasy.beans;

import lazydevs.readeasy.config.ReadEasyConfig;
import lazydevs.readeasy.controller.ConfiguredReadController;
import lazydevs.services.basic.validation.ParamValidator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Abhijeet Rai
 */
@Configuration
@EnableConfigurationProperties(ReadEasyConfig.class)
@Import({ConfiguredReadController.class, ParamValidator.class})
public class ReadEasyAutoConfiguration {

}
