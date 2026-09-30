package lazydevs.transporter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Abhijeet Rai
 */

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({TransporterController.class, TransporterCoreAutoConfiguration.class})
public class TransporterWebAutoConfiguration {

}
