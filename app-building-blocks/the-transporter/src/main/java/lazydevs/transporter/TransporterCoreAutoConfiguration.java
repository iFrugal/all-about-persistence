package lazydevs.transporter;

import lazydevs.transporter.config.TransporterConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Abhijeet Rai
 */
@Configuration
@Import({TransportService.class, TransporterConfig.class})
public class TransporterCoreAutoConfiguration {

}
