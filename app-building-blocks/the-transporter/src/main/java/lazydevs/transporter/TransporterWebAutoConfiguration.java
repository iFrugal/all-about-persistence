package lazydevs.transporter;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Abhijeet Rai
 */

@Configuration
@Import({TransporterController.class, TransporterCoreAutoConfiguration.class})
public class TransporterWebAutoConfiguration {

}
